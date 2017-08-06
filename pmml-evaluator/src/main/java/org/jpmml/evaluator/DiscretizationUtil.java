/*
 * Copyright (c) 2013 Villu Ruusmann
 *
 * This file is part of JPMML-Evaluator
 *
 * JPMML-Evaluator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-Evaluator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-Evaluator.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jpmml.evaluator;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Table;
import com.google.common.collect.TreeRangeMap;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Discretize;
import org.dmg.pmml.DiscretizeBin;
import org.dmg.pmml.InlineTable;
import org.dmg.pmml.Interval;
import org.dmg.pmml.MapValues;
import org.dmg.pmml.OpType;

public class DiscretizationUtil {

	private DiscretizationUtil(){
	}

	static
	public FieldValue discretize(Discretize discretize, FieldValue value){
		String result = discretize(discretize, value.asDouble());

		return FieldValueUtil.create(discretize.getDataType(), null, result);
	}

	static
	public String discretize(Discretize discretize, Double value){
		RangeMap<Double, String> binRanges = CacheUtil.getValue(discretize, DiscretizationUtil.binRangeCache);

		Map.Entry<Range<Double>, String> entry = binRanges.getEntry(value);
		if(entry != null){
			return entry.getValue();
		}

		return discretize.getDefaultValue();
	}

	static
	public FieldValue mapValue(MapValues mapValues, Map<String, FieldValue> values){
		String outputColumn = mapValues.getOutputColumn();
		if(outputColumn == null){
			throw new InvalidFeatureException(mapValues);
		}

		DataType dataType = mapValues.getDataType();

		InlineTable inlineTable = InlineTableUtil.getInlineTable(mapValues);
		if(inlineTable != null){
			Map<String, String> row = match(inlineTable, values);

			if(row != null){
				String result = row.get(outputColumn);

				if(result == null){
					throw new InvalidFeatureException(inlineTable);
				}

				return FieldValueUtil.create(dataType, null, result);
			}
		}

		return FieldValueUtil.create(dataType, null, mapValues.getDefaultValue());
	}

	static
	public Range<Double> toRange(Interval interval){
		Double leftMargin = interval.getLeftMargin();
		Double rightMargin = interval.getRightMargin();

		// "The leftMargin and rightMargin attributes are optional, but at least one value must be defined"
		if(leftMargin == null && rightMargin == null){
			throw new InvalidFeatureException(interval);
		} // End if

		if(leftMargin != null && rightMargin != null && (leftMargin).compareTo(rightMargin) > 0){
			throw new InvalidFeatureException(interval);
		}

		Interval.Closure closure = interval.getClosure();
		switch(closure){
			case OPEN_OPEN:
				{
					if(leftMargin == null){
						return Range.lessThan(rightMargin);
					} else

					if(rightMargin == null){
						return Range.greaterThan(leftMargin);
					}

					return Range.open(leftMargin, rightMargin);
				}
			case OPEN_CLOSED:
				{
					if(leftMargin == null){
						return Range.atMost(rightMargin);
					} else

					if(rightMargin == null){
						return Range.greaterThan(leftMargin);
					}

					return Range.openClosed(leftMargin, rightMargin);
				}
			case CLOSED_OPEN:
				{
					if(leftMargin == null){
						return Range.lessThan(rightMargin);
					} else

					if(rightMargin == null){
						return Range.atLeast(leftMargin);
					}

					return Range.closedOpen(leftMargin, rightMargin);
				}
			case CLOSED_CLOSED:
				{
					if(leftMargin == null){
						return Range.atMost(rightMargin);
					} else

					if(rightMargin == null){
						return Range.atLeast(leftMargin);
					}

					return Range.closed(leftMargin, rightMargin);
				}
			default:
				throw new UnsupportedFeatureException(interval, closure);
		}
	}

	static
	private Map<String, String> match(InlineTable inlineTable, Map<String, FieldValue> values){
		Map<String, RowFilter> rowFilters = CacheUtil.getValue(inlineTable, DiscretizationUtil.rowFilterCache);

		Set<Integer> rows = null;

		Collection<Map.Entry<String, FieldValue>> entries = values.entrySet();
		for(Map.Entry<String, FieldValue> entry : entries){
			String key = entry.getKey();
			FieldValue value = entry.getValue();

			RowFilter rowFilter = rowFilters.get(key);
			if(rowFilter == null){
				throw new InvalidFeatureException(inlineTable);
			}

			Map<FieldValue, Set<Integer>> columnRowMap = rowFilter.getValueMapping(value.getDataType(), value.getOpType());

			Set<Integer> columnRows = columnRowMap.get(value);

			if(columnRows != null && columnRows.size() > 0){

				if(rows == null){
					rows = (entries.size() > 1 ? new HashSet<>(columnRows) : columnRows);
				} else

				{
					rows.retainAll(columnRows);
				} // End if

				if(rows.isEmpty()){
					return null;
				}
			} else

			{
				return null;
			}
		}

		if(rows != null && rows.size() > 0){
			Table<Integer, String, String> content = InlineTableUtil.getContent(inlineTable);

			// "It is an error if the table entries used for matching are not unique"
			if(rows.size() != 1){
				throw new EvaluationException();
			}

			Integer row = Iterables.getOnlyElement(rows);

			return content.row(row);
		}

		return null;
	}

	static
	private RangeMap<Double, String> parseDiscretize(Discretize discretize){
		RangeMap<Double, String> result = TreeRangeMap.create();

		List<DiscretizeBin> discretizeBins = discretize.getDiscretizeBins();
		for(DiscretizeBin discretizeBin : discretizeBins){
			Interval interval = discretizeBin.getInterval();
			String binValue = discretizeBin.getBinValue();

			if(interval == null || binValue == null){
				throw new InvalidFeatureException(discretizeBin);
			}

			Range<Double> range = toRange(interval);

			result.put(range, binValue);
		}

		return result;
	}

	static
	private Map<String, RowFilter> parseInlineTable(InlineTable inlineTable){
		Map<String, RowFilter> result = new LinkedHashMap<>();

		Table<Integer, String, String> table = InlineTableUtil.getContent(inlineTable);

		Set<String> columns = table.columnKeySet();
		for(String column : columns){
			Map<Integer, String> columnValues = table.column(column);

			RowFilter rowFilter = new RowFilter(columnValues);

			result.put(column, rowFilter);
		}

		return result;
	}

	static
	private class RowFilter implements HasParsedValueMapping<Set<Integer>> {

		private Map<Integer, String> columnValues = null;

		private SetMultimap<FieldValue, Integer> parsedColumnValues = null;


		private RowFilter(Map<Integer, String> columnValues){
			setColumnValues(columnValues);
		}

		@SuppressWarnings (
			value = {"rawtypes", "unchecked"}
		)
		@Override
		public Map<FieldValue, Set<Integer>> getValueMapping(DataType dataType, OpType opType){

			if(this.parsedColumnValues == null){
				this.parsedColumnValues = ImmutableSetMultimap.copyOf(parseColumnValues(dataType, opType));
			}

			return (Map)this.parsedColumnValues.asMap();
		}

		private SetMultimap<FieldValue, Integer> parseColumnValues(DataType dataType, OpType opType){
			SetMultimap<FieldValue, Integer> result = HashMultimap.create();

			Map<Integer, String> columnValues = getColumnValues();

			Collection<Map.Entry<Integer, String>> entries = columnValues.entrySet();
			for(Map.Entry<Integer, String> entry : entries){
				FieldValue value = FieldValueUtil.create(dataType, opType, entry.getValue());
				Integer row = entry.getKey();

				result.put(value, row);
			}

			return result;
		}

		public Map<Integer, String> getColumnValues(){
			return this.columnValues;
		}

		private void setColumnValues(Map<Integer, String> columnValues){
			this.columnValues = columnValues;
		}
	}

	private static final LoadingCache<Discretize, RangeMap<Double, String>> binRangeCache = CacheUtil.buildLoadingCache(new CacheLoader<Discretize, RangeMap<Double, String>>(){

		@Override
		public RangeMap<Double, String> load(Discretize discretize){
			return ImmutableRangeMap.copyOf(parseDiscretize(discretize));
		}
	});

	private static final LoadingCache<InlineTable, Map<String, RowFilter>> rowFilterCache = CacheUtil.buildLoadingCache(new CacheLoader<InlineTable, Map<String, RowFilter>>(){

		@Override
		public Map<String, RowFilter> load(InlineTable inlineTable){
			return ImmutableMap.copyOf(parseInlineTable(inlineTable));
		}
	});
}