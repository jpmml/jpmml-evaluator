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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
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

		if(dataField == null){
			throw new InvalidFeatureException();
		}

		outlierTreatment:
		if(isOutlier(dataField, value)){

			if(miningField == null){
				throw new InvalidFeatureException();
			}

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
		}

		missingValueTreatment:
		if(isMissing(dataField, value)){

			if(miningField == null){
				return null;
			}

			value = miningField.getMissingValueReplacement();
			if(value != null){
				break missingValueTreatment;
			}

			return null;
		} // End if

		invalidValueTreatment:
		if(isInvalid(dataField, value)){

			if(miningField == null){
				throw new InvalidFeatureException();
			}

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

		OpType opType = dataField.getOptype();
		switch(opType){
			case CONTINUOUS:
				{
					List<Double> range = null;

					List<Interval> fieldIntervals = dataField.getIntervals();
					for(Interval fieldInterval : fieldIntervals){

						if(range == null){
							range = Lists.newArrayList();
						}

						range.add(fieldInterval.getLeftMargin());
						range.add(fieldInterval.getRightMargin());
					}

					List<Value> fieldValues = dataField.getValues();
					for(Value fieldValue : fieldValues){
						Value.Property property = fieldValue.getProperty();

						switch(property){
							case VALID:
								{
									if(range == null){
										range = Lists.newArrayList();
									}

									range.add((Double)TypeUtil.parseOrCast(DataType.DOUBLE, fieldValue.getValue()));
								}
								break;
							default:
								break;
						}
					}

					if(range == null){
						return false;
					}

					Double doubleValue = (Double)TypeUtil.parseOrCast(DataType.DOUBLE, value);

					Double minValue = Collections.min(range);
					if(TypeUtil.compare(DataType.DOUBLE, doubleValue, minValue) < 0){
						return true;
					}

					Double maxValue = Collections.max(range);
					if(TypeUtil.compare(DataType.DOUBLE, doubleValue, maxValue) > 0){
						return true;
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

		// Compare as String. Missing values are often represented as String constants that cannot be parsed to runtime data type (eg. N/A).
		String stringValue = TypeUtil.format(value);

		List<Value> fieldValues = dataField.getValues();
		for(Value fieldValue : fieldValues){
			Value.Property property = fieldValue.getProperty();

			switch(property){
				case MISSING:
					{
						boolean equals = TypeUtil.equals(DataType.STRING, stringValue, fieldValue.getValue());
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

		// Compare as runtime data type
		value = TypeUtil.parseOrCast(dataType, value);

		OpType opType = dataField.getOptype();
		switch(opType){
			case CONTINUOUS:
				{
					Double doubleValue = (Double)TypeUtil.cast(DataType.DOUBLE, value);

					int intervalCount = 0;

					List<Interval> fieldIntervals = dataField.getIntervals();
					for(Interval fieldInterval : fieldIntervals){
						intervalCount += 1;

						if(DiscretizationUtil.contains(fieldInterval, doubleValue)){
							return true;
						}
					}

					if(intervalCount > 0){
						return false;
					}
				}
				// Falls through
			case CATEGORICAL:
			case ORDINAL:
				{
					int validValueCount = 0;

					List<Value> fieldValues = dataField.getValues();
					for(Value fieldValue : fieldValues){
						Value.Property property = fieldValue.getProperty();

						switch(property){
							case VALID:
								{
									validValueCount += 1;

									boolean equals = TypeUtil.equals(dataType, value, TypeUtil.parseOrCast(dataType, fieldValue.getValue()));
									if(equals){
										return true;
									}
								}
								break;
							case INVALID:
								{
									boolean equals = TypeUtil.equals(dataType, value, TypeUtil.parseOrCast(dataType, fieldValue.getValue()));
									if(equals){
										return false;
									}
								}
								break;
							case MISSING:
								break;
							default:
								throw new UnsupportedFeatureException(fieldValue, property);
						}
					}

					if(validValueCount > 0){
						return false;
					}
				}
				break;
			default:
				throw new UnsupportedFeatureException(dataField, opType);
		}

		return true;
	}

	static
	public Value getValidValue(TypeDefinitionField field, Object value){
		List<Value> fieldValues = field.getValues();

		DataType dataType = field.getDataType();

		for(Value fieldValue : fieldValues){
			Value.Property property = fieldValue.getProperty();

			switch(property){
				case VALID:
					{
						boolean equals = TypeUtil.equals(dataType, value, TypeUtil.parseOrCast(dataType, fieldValue.getValue()));
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
	public List<String> getTargetCategories(TypeDefinitionField field){
		return CacheUtil.getValue(field, ArgumentUtil.targetCategoryCache);
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
}