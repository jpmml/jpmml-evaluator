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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.RangeSet;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Field;
import org.dmg.pmml.HasContinuousDomain;
import org.dmg.pmml.HasDiscreteDomain;
import org.dmg.pmml.InvalidValueTreatmentMethod;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MissingValueTreatmentMethod;
import org.dmg.pmml.OpType;
import org.dmg.pmml.OutlierTreatmentMethod;
import org.dmg.pmml.PMMLAttributes;
import org.dmg.pmml.Value;
import org.jpmml.model.XPathUtil;

public class InputFieldUtil {

	private InputFieldUtil(){
	}

	static
	public boolean isDefault(Field<?> field, MiningField miningField){

		if(field instanceof DataField){

			if(!Objects.equals(field.getOpType(), FieldUtil.getOpType(field, miningField))){
				return false;
			}
		} else

		{
			if(!Objects.equals(miningField.getOpType(), null)){
				return false;
			}
		}

		Object invalidValueReplacement = miningField.getInvalidValueReplacement();
		if(invalidValueReplacement != null){
			return false;
		}

		InvalidValueTreatmentMethod invalidValueTreatmentMethod = miningField.getInvalidValueTreatment();
		switch(invalidValueTreatmentMethod){
			case RETURN_INVALID:
			case AS_IS: // XXX
				break;
			default:
				return false;
		}

		Object missingValueReplacement = miningField.getMissingValueReplacement();
		if(missingValueReplacement != null){
			return false;
		}

		OutlierTreatmentMethod outlierTreatmentMethod = miningField.getOutlierTreatment();
		switch(outlierTreatmentMethod){
			case AS_IS:
				break;
			default:
				return false;
		}

		return true;
	}

	static
	public FieldValue prepareInputValue(Field<?> field, MiningField miningField, Object value){
		InputTypeInfo typeInfo = getTypeInfo(field, miningField);

		if(value instanceof Collection){
			Collection<?> rawValues = (Collection<?>)value;

			List<Object> pmmlValues = new ArrayList<>(rawValues.size());

			for(Object rawValue : rawValues){
				FieldValue fieldValue = prepareScalarInputValue(typeInfo, rawValue);

				pmmlValues.add(FieldValueUtil.getValue(fieldValue));
			}

			return createInputValue(typeInfo, pmmlValues);
		} else

		{
			return prepareScalarInputValue(typeInfo, value);
		}
	}

	static
	public FieldValue prepareResidualInputValue(DataField dataField, MiningField miningField, Object value){
		return prepareInputValue(dataField, miningField, value);
	}

	static
	private ScalarValue prepareScalarInputValue(InputTypeInfo typeInfo, Object value){

		if(FieldValueUtil.isMissing(value)){
			return performMissingValueTreatment(typeInfo);
		}

		boolean compatible;

		try {
			value = createInputValue(typeInfo, value);

			// The value is a valid ScalarValue
			compatible = true;
		} catch(IllegalArgumentException | TypeCheckException e){

			// The value is an invalid ScalarValue or Object
			compatible = false;
		}

		int status = getStatus(typeInfo, value, compatible);

		if(status > 0){
			return performValidValueTreatment(typeInfo, (ScalarValue)value);
		} else

		if(status == 0){
			return performMissingValueTreatment(typeInfo);
		} else

		if(status < 0){
			return performInvalidValueTreatment(typeInfo, value);
		} else

		{
			throw new IllegalArgumentException();
		}
	}

	static
	private ScalarValue performValidValueTreatment(InputTypeInfo typeInfo, ScalarValue value){
		MiningField miningField = typeInfo.getMiningField();

		OutlierTreatmentMethod outlierTreatmentMethod = miningField.getOutlierTreatment();
		switch(outlierTreatmentMethod){
			case AS_IS:
				return value;
			case AS_MISSING_VALUES:
			case AS_EXTREME_VALUES:
				break;
			default:
				throw new UnsupportedAttributeException(miningField, outlierTreatmentMethod);
		}

		Number lowValue = miningField.getLowValue();
		Number highValue = miningField.getHighValue();

		// "At least one of bounds is required"
		if(lowValue == null && highValue == null){
			throw new MissingAttributeException(miningField, PMMLAttributes.MININGFIELD_LOWVALUE);
		} // End if

		if((lowValue != null && highValue != null) && NumberUtil.compare(lowValue, highValue) > 0){
			throw new InvalidElementException(miningField);
		}

		switch(outlierTreatmentMethod){
			case AS_MISSING_VALUES:
				if(lowValue != null && value.compareToValue(lowValue) < 0){
					return createMissingInputValue(typeInfo);
				} else

				if(highValue != null && value.compareToValue(highValue) > 0){
					return createMissingInputValue(typeInfo);
				}
				break;
			case AS_EXTREME_VALUES:
				if(lowValue != null && value.compareToValue(lowValue) < 0){
					return (ScalarValue)createInputValue(typeInfo, lowValue);
				} else

				if(highValue != null && value.compareToValue(highValue) > 0){
					return (ScalarValue)createInputValue(typeInfo, highValue);
				}
				break;
			default:
				throw new UnsupportedAttributeException(miningField, outlierTreatmentMethod);
		}

		return value;
	}

	static
	private ScalarValue performInvalidValueTreatment(InputTypeInfo typeInfo, Object value){
		MiningField miningField = typeInfo.getMiningField();

		InvalidValueTreatmentMethod invalidValueTreatmentMethod = miningField.getInvalidValueTreatment();
		switch(invalidValueTreatmentMethod){
			case AS_IS:
				break;
			case AS_MISSING:
			case RETURN_INVALID:
				Object invalidValueReplacement = miningField.getInvalidValueReplacement();
				if(invalidValueReplacement != null){
					throw new MisplacedAttributeException(miningField, PMMLAttributes.MININGFIELD_INVALIDVALUEREPLACEMENT, invalidValueReplacement);
				}
				break;
			default:
				throw new UnsupportedAttributeException(miningField, invalidValueTreatmentMethod);
		} // End switch

		switch(invalidValueTreatmentMethod){
			case RETURN_INVALID:
				Field<?> field = typeInfo.getField();

				throw new InvalidResultException("Field " + PMMLException.formatKey(field.getName()) + " cannot accept user input value " + PMMLException.formatValue(value), miningField);
			case AS_IS:
				return createInvalidInputValue(typeInfo, value);
			case AS_MISSING:
				return createMissingInputValue(typeInfo);
			default:
				throw new UnsupportedAttributeException(miningField, invalidValueTreatmentMethod);
		}
	}

	static
	private ScalarValue performMissingValueTreatment(InputTypeInfo typeInfo){
		MiningField miningField = typeInfo.getMiningField();

		MissingValueTreatmentMethod missingValueTreatmentMethod = miningField.getMissingValueTreatment();
		if(missingValueTreatmentMethod == null){
			missingValueTreatmentMethod = MissingValueTreatmentMethod.AS_IS;
		}

		switch(missingValueTreatmentMethod){
			case AS_IS:
			case AS_MEAN:
			case AS_MODE:
			case AS_MEDIAN:
			case AS_VALUE:
				return createMissingInputValue(typeInfo);
			case RETURN_INVALID:
				Field<?> field = typeInfo.getField();

				Object missingValueReplacement = miningField.getMissingValueReplacement();
				if(missingValueReplacement != null){
					throw new MisplacedAttributeException(miningField, PMMLAttributes.MININGFIELD_MISSINGVALUEREPLACEMENT, missingValueReplacement);
				}

				throw new InvalidResultException("Field " + PMMLException.formatKey(field.getName()) + " requires user input value", miningField);
			default:
				throw new UnsupportedAttributeException(miningField, missingValueTreatmentMethod);
		}
	}

	static
	private Integer getStatus(InputTypeInfo typeInfo, Object value, boolean compatible){
		Field<?> field = typeInfo.getField();

		values:
		if(field instanceof HasDiscreteDomain){
			HasDiscreteDomain<?> hasDiscreteDomain = (HasDiscreteDomain<?>)field;

			if(!hasDiscreteDomain.hasValues()){
				break values;
			}

			DataType dataType = typeInfo.getDataType();

			if(field instanceof ValueStatusHolder){
				ValueStatusHolder valueStatusHolder = (ValueStatusHolder)field;

				if(compatible){
					FieldValue fieldValue = (FieldValue)value;

					Integer index = valueStatusHolder.get(fieldValue.getDataType(), fieldValue.getValue());
					if(index != null){
						return index;
					} // End if

					if(valueStatusHolder.hasValidValues()){
						return FieldValue.STATUS_UNKNOWN_INVALID;
					}
				}
			}

			int validIndex = 0;

			List<Value> pmmlValues = hasDiscreteDomain.getValues();
			for(int i = 0, max = pmmlValues.size(); i < max; i++){
				Value pmmlValue = pmmlValues.get(i);

				Object simpleValue = pmmlValue.getValue();
				if(simpleValue == null){
					throw new MissingAttributeException(pmmlValue, PMMLAttributes.VALUE_VALUE);
				}

				Value.Property property = pmmlValue.getProperty();
				switch(property){
					case VALID:
						{
							validIndex++;

							if(!compatible){
								continue;
							}

							boolean equals;

							if(value instanceof FieldValue){
								FieldValue fieldValue = (FieldValue)value;

								equals = fieldValue.equalsValue(simpleValue);
							} else

							{
								equals = TypeUtil.equals(dataType, value, simpleValue);
							} // End if

							if(equals){
								return validIndex;
							}
						}
						break;
					case INVALID:
					case MISSING:
						{
							boolean equals;

							if(value instanceof FieldValue){
								FieldValue fieldValue = (FieldValue)value;

								equals = TypeUtil.equals(dataType, fieldValue.getValue(), simpleValue);
							} else

							{
								equals = TypeUtil.equals(dataType, value, simpleValue);
							} // End if

							if(equals){

								switch(property){
									case INVALID:
										return FieldValue.STATUS_UNKNOWN_INVALID;
									case MISSING:
										return FieldValue.STATUS_MISSING;
									default:
										throw new UnsupportedAttributeException(pmmlValue, property);
								}
							}
						}
						break;
					default:
						throw new UnsupportedAttributeException(pmmlValue, property);
				}
			}

			// "If a field contains at least one Value element where the value of property is valid, then the set of Value elements completely defines the set of valid values"
			if(validIndex > 0){
				return FieldValue.STATUS_UNKNOWN_INVALID;
			}
		} // End if

		if(!compatible){
			return FieldValue.STATUS_UNKNOWN_INVALID;
		} // End if

		intervals:
		if(field instanceof HasContinuousDomain){
			HasContinuousDomain<?> hasContinuousDomain = (HasContinuousDomain<?>)field;

			if(!hasContinuousDomain.hasIntervals()){
				break intervals;
			}

			OpType opType = typeInfo.getOpType();
			switch(opType){
				case CONTINUOUS:
					break;
				default:
					// "Intervals are not allowed for non-continuous fields"
					throw new InvalidElementException(field);
			}

			RangeSet<Double> validRanges = FieldUtil.getValidRanges((Field & HasContinuousDomain)field);

			Double doubleValue;

			if(value instanceof FieldValue){
				FieldValue fieldValue = (FieldValue)value;

				doubleValue = fieldValue.asDouble();
			} else

			{
				throw new IllegalArgumentException();
			}

			// "If intervals are present, then a value that is outside the intervals is considered invalid"
			if(!validRanges.contains(doubleValue)){
				return FieldValue.STATUS_UNKNOWN_INVALID;
			}
		} // End if

		if(value instanceof FieldValue){
			FieldValue fieldValue = (FieldValue)value;

			if(!fieldValue.isValid()){
				return FieldValue.STATUS_UNKNOWN_INVALID;
			}

			// "Any value is valid by default"
			return FieldValue.STATUS_UNKNOWN_VALID;
		} else

		{
			throw new IllegalArgumentException();
		}
	}

	static
	private ScalarValue createInvalidInputValue(InputTypeInfo typeInfo, Object value){
		MiningField miningField = typeInfo.getMiningField();

		Object invalidValueReplacement = miningField.getInvalidValueReplacement();
		if(invalidValueReplacement != null){
			return (ScalarValue)createInputValue(typeInfo, invalidValueReplacement);
		}

		ScalarValue fieldValue = (ScalarValue)createInputValue(typeInfo, value);
		if(fieldValue.isValid()){
			fieldValue.setValid(false);
		}

		return fieldValue;
	}

	static
	private ScalarValue createMissingInputValue(InputTypeInfo typeInfo){
		MiningField miningField = typeInfo.getMiningField();

		Object missingValueReplacement = miningField.getMissingValueReplacement();
		if(missingValueReplacement != null){
			return (ScalarValue)createInputValue(typeInfo, missingValueReplacement);
		}

		return (ScalarValue)FieldValues.MISSING_VALUE;
	}

	static
	private FieldValue createInputValue(TypeInfo typeInfo, Object value){

		if(value instanceof FieldValue){
			FieldValue fieldValue = (FieldValue)value;

			return fieldValue.cast(typeInfo);
		} else

		{
			return FieldValueUtil.create(typeInfo, value);
		}
	}

	static
	private InputTypeInfo getTypeInfo(Field<?> field, MiningField miningField){
		InputTypeInfo typeInfo = new InputTypeInfo(){

			@Override
			public Field<?> getField(){
				return field;
			}

			@Override
			public MiningField getMiningField(){
				return miningField;
			}

			@Override
			public DataType getDataType(){
				DataType dataType = FieldUtil.getDataType(field);
				if(dataType == null){
					throw new MissingAttributeException(MissingAttributeException.formatMessage(XPathUtil.formatElement(field.getClass()) + "@dataType"), field);
				}

				return dataType;
			}

			@Override
			public OpType getOpType(){
				OpType opType = FieldUtil.getOpType(field, miningField);
				if(opType == null){
					throw new MissingAttributeException(MissingAttributeException.formatMessage(XPathUtil.formatElement(field.getClass()) + "@optype"), field);
				}

				return opType;
			}

			@Override
			public List<?> getOrdering(){
				List<?> ordering = null;

				if(field instanceof HasDiscreteDomain){
					ordering = FieldUtil.getValidValues((Field & HasDiscreteDomain)field);
				}

				return ordering;
			}
		};

		return typeInfo;
	}
}