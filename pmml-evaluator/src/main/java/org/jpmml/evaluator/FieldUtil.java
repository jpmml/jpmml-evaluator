/*
 * Copyright (c) 2018 Villu Ruusmann
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

import java.util.ArrayList;
import java.util.List;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Field;
import org.dmg.pmml.HasContinuousDomain;
import org.dmg.pmml.HasDiscreteDomain;
import org.dmg.pmml.Interval;
import org.dmg.pmml.Value;

public class FieldUtil {

	private FieldUtil(){
	}

	static
	public List<String> getCategories(DataField dataField){
		return CacheUtil.getValue(dataField, FieldUtil.categoryCache);
	}

	static
	public <F extends Field<F> & HasDiscreteDomain<F>> List<?> getValidValues(F field){
		return CacheUtil.getValue(field, FieldUtil.validValueCache);
	}

	static
	public <F extends Field<F> & HasContinuousDomain<F>> RangeSet<Double> getValidRanges(F field){
		return CacheUtil.getValue(field, FieldUtil.validRangeCache);
	}

	static
	private List<String> parseCategories(DataField dataField){
		List<String> result = new ArrayList<>();

		if(dataField.hasValues()){
			List<Value> pmmlValues = dataField.getValues();

			for(Value pmmlValue : pmmlValues){
				String stringValue = pmmlValue.getValue();
				if(stringValue == null){
					throw new MissingAttributeException(pmmlValue, PMMLAttributes.VALUE_VALUE);
				}

				Value.Property property = pmmlValue.getProperty();
				switch(property){
					case VALID:
						result.add(stringValue);
						break;
					default:
						break;
				}
			}
		}

		return result;
	}

	static
	private <F extends Field<F> & HasDiscreteDomain<F>> List<Object> parseValidValues(F field){
		List<Object> result = new ArrayList<>();

		DataType dataType = field.getDataType();

		if(field.hasValues()){
			List<Value> pmmlValues = field.getValues();

			for(Value pmmlValue : pmmlValues){
				String stringValue = pmmlValue.getValue();
				if(stringValue == null){
					throw new MissingAttributeException(pmmlValue, PMMLAttributes.VALUE_VALUE);
				}

				Value.Property property = pmmlValue.getProperty();
				switch(property){
					case VALID:
						result.add(TypeUtil.parse(dataType, stringValue));
						break;
					default:
						break;
				}
			}
		}

		return result;
	}

	static
	private <F extends Field<F> & HasContinuousDomain<F>> RangeSet<Double> parseValidRanges(F field){
		RangeSet<Double> result = TreeRangeSet.create();

		if(field.hasIntervals()){
			List<Interval> intervals = field.getIntervals();

			for(Interval interval : intervals){
				Range<Double> range = DiscretizationUtil.toRange(interval);

				result.add(range);
			}
		}

		return result;
	}

	private static final LoadingCache<DataField, List<String>> categoryCache = CacheUtil.buildLoadingCache(new CacheLoader<DataField, List<String>>(){

		@Override
		public List<String> load(DataField dataField){
			return ImmutableList.copyOf(parseCategories(dataField));
		}
	});

	private static final LoadingCache<Field<?>, List<Object>> validValueCache = CacheUtil.buildLoadingCache(new CacheLoader<Field<?>, List<Object>>(){

		@Override
		public List<Object> load(Field<?> field){
			return ImmutableList.<Object>copyOf(parseValidValues((Field & HasDiscreteDomain)field));
		}
	});

	private static final LoadingCache<Field<?>, RangeSet<Double>> validRangeCache = CacheUtil.buildLoadingCache(new CacheLoader<Field<?>, RangeSet<Double>>(){

		@Override
		public RangeSet<Double> load(Field<?> field){
			return ImmutableRangeSet.<Double>copyOf(parseValidRanges((Field & HasContinuousDomain)field));
		}
	});
}