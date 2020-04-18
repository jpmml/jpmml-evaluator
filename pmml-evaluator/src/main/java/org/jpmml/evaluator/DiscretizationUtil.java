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
import java.util.EnumMap;
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
import org.dmg.pmml.PMMLAttributes;
import org.dmg.pmml.PMMLElements;

public class DiscretizationUtil {

	private DiscretizationUtil(){
	}

	static
	public FieldValue discretize(Discretize discretize, FieldValue value){
		Object result = discretize(discretize, value.asDouble());

		return FieldValueUtil.create(discretize.getDataType(DataType.STRING), OpType.CATEGORICAL, result);
	}

	static
	public Object discretize(Discretize discretize, Double value){
		RangeMap<Double, Object> binRanges = CacheUtil.getValue(discretize, DiscretizationUtil.binRangeCache);

		Map.Entry<Range<Double>, Object> entry = binRanges.getEntry(value);
		if(entry != null){
			return entry.getValue();
		}

		return discretize.getDefaultValue();
	}

	static
	public FieldValue mapValue(MapValues mapValues, Map<String, FieldValue> values){
		String outputColumn = mapValues.getOutputColumn();
		if(outputColumn == null){
			throw new MissingAttributeException(mapValues, PMMLAttributes.MAPVALUES_OUTPUTCOLUMN);
		}

		DataType dataType = mapValues.getDataType(DataType.STRING);

		InlineTable inlineTable = InlineTableUtil.getInlineTable(mapValues);
		if(inlineTable != null){
			Map<String, Object> row = match(inlineTable, values);

			if(row != null){
				Object result = row.get(outputColumn);

				if(result == null){
					throw new InvalidElementException(inlineTable);
				}

				return FieldValueUtil.create(dataType, OpType.CATEGORICAL, result);
			}
		}

		return FieldValueUtil.create(dataType, OpType.CATEGORICAL, mapValues.getDefaultValue());
	}

	static
	public Range<Double> toRange(Interval interval){
		Double leftMargin = NumberUtil.asDouble(interval.getLeftMargin());
		Double rightMargin = NumberUtil.asDouble(interval.getRightMargin());

		// "The leftMargin and rightMargin attributes are optional, but at least one value must be defined"
		if(leftMargin == null && rightMargin == null){
			throw new MissingAttributeException(interval, PMMLAttributes.INTERVAL_LEFTMARGIN);
		} // End if

		if(leftMargin != null && rightMargin != null && NumberUtil.compare(leftMargin, rightMargin) > 0){
			throw new InvalidElementException(interval);
		}

		Interval.Closure closure = interval.getClosure();
		if(closure == null){
			throw new MissingAttributeException(interval, PMMLAttributes.INTERVAL_CLOSURE);
		}

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
				throw new UnsupportedAttributeException(interval, closure);
		}
	}

	static
	private Map<String, Object> match(InlineTable inlineTable, Map<String, FieldValue> values){
		Map<String, RowFilter> rowFilters = CacheUtil.getValue(inlineTable, DiscretizationUtil.rowFilterCache);

		Set<Integer> rows = null;

		Collection<Map.Entry<String, FieldValue>> entries = values.entrySet();
		for(Map.Entry<String, FieldValue> entry : entries){
			String key = entry.getKey();
			FieldValue value = entry.getValue();

			RowFilter rowFilter = rowFilters.get(key);
			if(rowFilter == null){
				throw new InvalidElementException(inlineTable);
			}

			SetMultimap<Object, Integer> valueRowsMap = rowFilter.getValueRowsMap(value.getDataType());

			Set<Integer> valueRows = valueRowsMap.get(FieldValueUtil.getValue(value));

			if(valueRows != null && !valueRows.isEmpty()){

				if(rows == null){
					rows = (entries.size() > 1 ? new HashSet<>(valueRows) : valueRows);
				} else

				{
					rows.retainAll(valueRows);
				} // End if

				if(rows.isEmpty()){
					return null;
				}
			} else

			{
				return null;
			}
		}

		if(rows != null && !rows.isEmpty()){
			Table<Integer, String, Object> content = InlineTableUtil.getContent(inlineTable);

			// "It is an error if the table entries used for matching are not unique"
			if(rows.size() != 1){
				throw new InvalidElementException(inlineTable);
			}

			Integer row = Iterables.getOnlyElement(rows);

			return content.row(row);
		}

		return null;
	}

	static
	private RangeMap<Double, Object> parseDiscretize(Discretize discretize){
		RangeMap<Double, Object> result = TreeRangeMap.create();

		List<DiscretizeBin> discretizeBins = discretize.getDiscretizeBins();
		for(DiscretizeBin discretizeBin : discretizeBins){
			Interval interval = discretizeBin.getInterval();
			if(interval == null){
				throw new MissingElementException(discretizeBin, PMMLElements.DISCRETIZEBIN_INTERVAL);
			}

			Range<Double> range = toRange(interval);

			Object binValue = discretizeBin.getBinValue();
			if(binValue == null){
				throw new MissingAttributeException(discretizeBin, PMMLAttributes.DISCRETIZEBIN_BINVALUE);
			}

			result.put(range, binValue);
		}

		return result;
	}

	static
	private Map<String, RowFilter> parseInlineTable(InlineTable inlineTable){
		Map<String, RowFilter> result = new LinkedHashMap<>();

		Table<Integer, String, Object> table = InlineTableUtil.getContent(inlineTable);

		Set<String> columns = table.columnKeySet();
		for(String column : columns){
			Map<Integer, Object> columnValues = table.column(column);

			RowFilter rowFilter = new RowFilter(columnValues);

			result.put(column, rowFilter);
		}

		return result;
	}

	static
	private class RowFilter {

		private Map<Integer, Object> columnValues = null;

		private Map<DataType, SetMultimap<Object, Integer>> valueRowsMap = new EnumMap<>(DataType.class);


		private RowFilter(Map<Integer, Object> columnValues){
			setColumnValues(columnValues);
		}

		public SetMultimap<Object, Integer> getValueRowsMap(DataType dataType){
			SetMultimap<Object, Integer> result = this.valueRowsMap.get(dataType);

			if(result == null){
				result = ImmutableSetMultimap.copyOf(parseColumnValues(dataType));

				this.valueRowsMap.put(dataType, result);
			}

			return result;
		}

		private SetMultimap<Object, Integer> parseColumnValues(DataType dataType){
			Map<Integer, Object> columnValues = getColumnValues();

			SetMultimap<Object, Integer> result = HashMultimap.create();

			Collection<Map.Entry<Integer, Object>> entries = columnValues.entrySet();
			for(Map.Entry<Integer, Object> entry : entries){
				Object value = TypeUtil.parseOrCast(dataType, entry.getValue());
				Integer row = entry.getKey();

				result.put(value, row);
			}

			return result;
		}

		public Map<Integer, Object> getColumnValues(){
			return this.columnValues;
		}

		private void setColumnValues(Map<Integer, Object> columnValues){
			this.columnValues = columnValues;
		}
	}

	private static final LoadingCache<Discretize, RangeMap<Double, Object>> binRangeCache = CacheUtil.buildLoadingCache(new CacheLoader<Discretize, RangeMap<Double, Object>>(){

		@Override
		public RangeMap<Double, Object> load(Discretize discretize){
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