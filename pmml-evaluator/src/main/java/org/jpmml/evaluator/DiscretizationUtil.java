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

import java.util.List;
import java.util.Map;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.Table;
import com.google.common.collect.TreeRangeMap;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Discretize;
import org.dmg.pmml.DiscretizeBin;
import org.dmg.pmml.InlineTable;
import org.dmg.pmml.Interval;
import org.dmg.pmml.MapValues;
import org.dmg.pmml.TableLocator;
import org.jpmml.manager.InvalidFeatureException;
import org.jpmml.manager.UnsupportedFeatureException;

public class DiscretizationUtil {

	private DiscretizationUtil(){
	}

	static
	public FieldValue discretize(Discretize discretize, FieldValue value){
		String result = discretize(discretize, (value.asNumber()).doubleValue());

		return FieldValueUtil.create(discretize.getDataType(), null, result);
	}

	static
	public String discretize(Discretize discretize, double value){
		RangeMap<Double, String> binRanges = CacheUtil.getValue(discretize, DiscretizationUtil.binRangeCache);

		Map.Entry<Range<Double>, String> entry = binRanges.getEntry(value);
		if(entry != null){
			return entry.getValue();
		}

		return discretize.getDefaultValue();
	}

	static
	public FieldValue mapValue(MapValues mapValues, Map<String, FieldValue> values){
		DataType dataType = mapValues.getDataType();

		TableLocator tableLocator = mapValues.getTableLocator();
		if(tableLocator != null){
			throw new UnsupportedFeatureException(tableLocator);
		}

		InlineTable inlineTable = mapValues.getInlineTable();
		if(inlineTable != null){
			Table<Integer, String, String> table = InlineTableUtil.getContent(inlineTable);

			Map<String, String> row = InlineTableUtil.match(table, values);
			if(row != null){
				String result = row.get(mapValues.getOutputColumn());
				if(result == null){
					throw new EvaluationException(mapValues);
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

		// "The attributes leftMargin and rightMargin are optional but at least one value must be defined"
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
	private RangeMap<Double, String> parseBinRanges(Discretize discretize){
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

	private static final LoadingCache<Discretize, RangeMap<Double, String>> binRangeCache = CacheBuilder.newBuilder()
		.weakKeys()
		.build(new CacheLoader<Discretize, RangeMap<Double, String>>(){

			@Override
			public RangeMap<Double, String> load(Discretize discretize){
				return ImmutableRangeMap.copyOf(parseBinRanges(discretize));
			}
		});
}