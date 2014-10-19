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

import java.util.Collections;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Interval;
import org.dmg.pmml.InvalidValueTreatmentMethodType;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.OpType;
import org.dmg.pmml.OutlierTreatmentMethodType;
import org.dmg.pmml.TypeDefinitionField;
import org.dmg.pmml.Value;
import org.jpmml.manager.InvalidFeatureException;
import org.jpmml.manager.UnsupportedFeatureException;

public class ArgumentUtil {

	private ArgumentUtil(){
	}

	@SuppressWarnings (
		value = {"unused"}
	)
	static
	public FieldValue prepare(DataField dataField, MiningField miningField, Object value){

		if(value != null){
			DataType dataType = dataField.getDataType();

			try {
				value = TypeUtil.parseOrCast(dataType, value);
			} catch(IllegalArgumentException iae){
				// Ignored
			}
		}

		outlierTreatment:
		if(isOutlier(dataField, value)){
			OutlierTreatmentMethodType outlierTreatmentMethod = miningField.getOutlierTreatment();

			switch(outlierTreatmentMethod){
				case AS_IS:
					break;
				case AS_MISSING_VALUES:
					value = null;
					break;
				case AS_EXTREME_VALUES:
					{
						Double lowValue = miningField.getLowValue();
						Double highValue = miningField.getHighValue();

						if(lowValue == null || highValue == null){
							throw new InvalidFeatureException(miningField);
						} // End if

						if((lowValue).compareTo(highValue) > 0){
							throw new InvalidFeatureException(miningField);
						}

						Double doubleValue = (Double)TypeUtil.parseOrCast(DataType.DOUBLE, value);

						if(TypeUtil.compare(DataType.DOUBLE, doubleValue, lowValue) < 0){
							value = lowValue;
						} else

						if(TypeUtil.compare(DataType.DOUBLE, doubleValue, highValue) > 0){
							value = highValue;
						}
					}
					break;
				default:
					throw new UnsupportedFeatureException(miningField, outlierTreatmentMethod);
			}
		} // End if

		missingValueTreatment:
		if(isMissing(dataField, value)){
			value = miningField.getMissingValueReplacement();

			if(value != null){
				break missingValueTreatment;
			}

			return null;
		} // End if

		invalidValueTreatment:
		if(isInvalid(dataField, value)){
			InvalidValueTreatmentMethodType invalidValueTreatmentMethod = miningField.getInvalidValueTreatment();

			switch(invalidValueTreatmentMethod){
				case RETURN_INVALID:
					throw new InvalidResultException(miningField);
				case AS_IS:
					break invalidValueTreatment;
				case AS_MISSING:
					{
						value = miningField.getMissingValueReplacement();
						if(value != null){
							break invalidValueTreatment;
						}

						return null;
					}
				default:
					throw new UnsupportedFeatureException(miningField, invalidValueTreatmentMethod);
			}
		}

		return FieldValueUtil.create(dataField, value);
	}

	static
	public boolean isOutlier(DataField dataField, Object value){

		if(value == null){
			return false;
		}

		List<Interval> intervals = dataField.getIntervals();

		OpType opType = dataField.getOptype();
		switch(opType){
			case CONTINUOUS:
				{
					if(intervals.size() > 0){
						RangeSet<Double> validRange = CacheUtil.getValue(dataField, ArgumentUtil.validRangeCache);

						Range<Double> validRangeSpan = validRange.span();

						Double doubleValue = (Double)TypeUtil.parseOrCast(DataType.DOUBLE, value);

						return !validRangeSpan.contains(doubleValue);
					}
				}
				break;
			case CATEGORICAL:
			case ORDINAL:
				break;
			default:
				throw new UnsupportedFeatureException(dataField, opType);
		}

		return false;
	}

	static
	public boolean isMissing(DataField dataField, Object value){

		if(value == null){
			return true;
		}

		DataType dataType = dataField.getDataType();

		List<Value> fieldValues = dataField.getValues();
		for(Value fieldValue : fieldValues){
			Value.Property property = fieldValue.getProperty();

			switch(property){
				case MISSING:
					{
						boolean equals = equals(dataType, value, fieldValue.getValue());
						if(equals){
							return true;
						}
					}
					break;
				default:
					break;
			}
		}

		return false;
	}

	static
	public boolean isInvalid(DataField dataField, Object value){

		if(value == null){
			return false;
		}

		return !isValid(dataField, value);
	}

	@SuppressWarnings (
		value = "fallthrough"
	)
	static
	public boolean isValid(DataField dataField, Object value){

		if(value == null){
			return false;
		}

		DataType dataType = dataField.getDataType();

		List<Interval> intervals = dataField.getIntervals();

		OpType opType = dataField.getOptype();
		switch(opType){
			case CONTINUOUS:
				{
					// "If intervals are present, then a value that is outside the intervals is considered invalid"
					if(intervals.size() > 0){
						RangeSet<Double> validRanges = CacheUtil.getValue(dataField, ArgumentUtil.validRangeCache);

						Double doubleValue = (Double)TypeUtil.parseOrCast(DataType.DOUBLE, value);

						return validRanges.contains(doubleValue);
					}
				}
				// Falls through
			case CATEGORICAL:
			case ORDINAL:
				{
					// "Intervals are not allowed for non-continuous fields"
					if(intervals.size() > 0){
						throw new InvalidFeatureException(dataField);
					}

					int validValueCount = 0;

					List<Value> fieldValues = dataField.getValues();
					for(Value fieldValue : fieldValues){
						Value.Property property = fieldValue.getProperty();

						switch(property){
							case VALID:
								{
									validValueCount += 1;

									boolean equals = equals(dataType, value, fieldValue.getValue());
									if(equals){
										return true;
									}
								}
								break;
							case INVALID:
							case MISSING:
								{
									boolean equals = equals(dataType, value, fieldValue.getValue());
									if(equals){
										return false;
									}
								}
								break;
							default:
								throw new UnsupportedFeatureException(fieldValue, property);
						}
					}

					// "If a field contains at least one Value element where the value of property is valid, then the set of Value elements completely defines the set of valid values"
					if(validValueCount > 0){
						return false;
					}

					// "Any value is valid by default"
					return true;
				}
			default:
				throw new UnsupportedFeatureException(dataField, opType);
		}
	}

	static
	public Value getValidValue(TypeDefinitionField field, Object value){
		DataType dataType = field.getDataType();

		List<Value> fieldValues = field.getValues();
		for(Value fieldValue : fieldValues){
			Value.Property property = fieldValue.getProperty();

			switch(property){
				case VALID:
					{
						boolean equals = equals(dataType, value, fieldValue.getValue());
						if(equals){
							return fieldValue;
						}
					}
					break;
				default:
					break;
			}
		}

		return null;
	}

	static
	public List<Value> getValidValues(TypeDefinitionField field){
		List<Value> fieldValues = field.getValues();
		if(fieldValues.isEmpty()){
			return Collections.emptyList();
		}

		List<Value> result = Lists.newArrayList();

		for(Value fieldValue : fieldValues){
			Value.Property property = fieldValue.getProperty();

			switch(property){
				case VALID:
					result.add(fieldValue);
					break;
				default:
					break;
			}
		}

		return result;
	}

	static
	private boolean equals(DataType dataType, Object value, String referenceValue){

		try {
			return TypeUtil.equals(dataType, value, TypeUtil.parseOrCast(dataType, referenceValue));
		} catch(IllegalArgumentException iae){

			// The String representation of invalid or missing values (eg. "N/A") may not be parseable to the requested representation
			try {
				return TypeUtil.equals(DataType.STRING, value, referenceValue);
			} catch(TypeCheckException tce){
				// Ignored
			}

			throw iae;
		}
	}

	static
	public List<String> getTargetCategories(TypeDefinitionField field){
		return CacheUtil.getValue(field, ArgumentUtil.targetCategoryCache);
	}

	static
	private RangeSet<Double> parseValidRanges(DataField dataField){
		RangeSet<Double> result = TreeRangeSet.create();

		List<Interval> intervals = dataField.getIntervals();
		for(Interval interval : intervals){
			Range<Double> range = DiscretizationUtil.toRange(interval);

			result.add(range);
		}

		return result;
	}

	private static final LoadingCache<TypeDefinitionField, List<String>> targetCategoryCache = CacheBuilder.newBuilder()
		.weakKeys()
		.build(new CacheLoader<TypeDefinitionField, List<String>>(){

			@Override
			public List<String> load(TypeDefinitionField field){
				List<Value> values = getValidValues(field);

				Function<Value, String> function = new Function<Value, String>(){

					@Override
					public String apply(Value value){
						String result = value.getValue();
						if(result == null){
							throw new InvalidFeatureException(value);
						}

						return result;
					}
				};

				return ImmutableList.copyOf(Iterables.transform(values, function));
			}
		});

	private static final LoadingCache<DataField, RangeSet<Double>> validRangeCache = CacheBuilder.newBuilder()
		.weakKeys()
		.build(new CacheLoader<DataField, RangeSet<Double>>(){

			@Override
			public RangeSet<Double> load(DataField dataField){
				return ImmutableRangeSet.copyOf(parseValidRanges(dataField));
			}
		});
}