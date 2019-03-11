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
import org.dmg.pmml.HasDiscreteDomain;
import org.dmg.pmml.InvalidValueTreatmentMethod;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MissingValueTreatmentMethod;
import org.dmg.pmml.OpType;
import org.dmg.pmml.OutlierTreatmentMethod;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.Value;

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

		if(FieldValueUtil.isMissing(value)){
			return performMissingValueTreatment(field, miningField);
		} // End if

		if(value instanceof Collection){
			Collection<?> rawValues = (Collection<?>)value;

			List<Object> pmmlValues = new ArrayList<>(rawValues.size());

			for(Object rawValue : rawValues){
				FieldValue fieldValue = prepareScalarInputValue(field, miningField, rawValue);

				pmmlValues.add(FieldValueUtil.getValue(fieldValue));
			}

			return createInputValue(field, miningField, pmmlValues);
		} else

		{
			return prepareScalarInputValue(field, miningField, value);
		}
	}

	static
	public FieldValue prepareResidualInputValue(DataField dataField, MiningField miningField, Object value){
		return prepareInputValue(dataField, miningField, value);
	}

	static
	private FieldValue prepareScalarInputValue(Field<?> field, MiningField miningField, Object value){
		boolean compatible;

		try {
			value = createInputValue(field, miningField, value);

			compatible = true;
		} catch(IllegalArgumentException | TypeCheckException e){
			compatible = false;
		}

		Value.Property status = getStatus(field, miningField, value, compatible);
		switch(status){
			case VALID:
				return performValidValueTreatment(field, miningField, (FieldValue)value);
			case INVALID:
				return performInvalidValueTreatment(field, miningField, value);
			case MISSING:
				return performMissingValueTreatment(field, miningField);
			default:
				throw new IllegalArgumentException();
		}
	}

	static
	public FieldValue performValidValueTreatment(Field<?> field, MiningField miningField, FieldValue value){
		OutlierTreatmentMethod outlierTreatmentMethod = miningField.getOutlierTreatment();

		switch(outlierTreatmentMethod){
			case AS_IS:
				return createInputValue(field, miningField, value);
			case AS_MISSING_VALUES:
			case AS_EXTREME_VALUES:
				break;
			default:
				throw new UnsupportedAttributeException(miningField, outlierTreatmentMethod);
		}

		Double lowValue = miningField.getLowValue();
		Double highValue = miningField.getHighValue();

		// "At least one of bounds is required"
		if(lowValue == null && highValue == null){
			throw new MissingAttributeException(miningField, PMMLAttributes.MININGFIELD_LOWVALUE);
		} // End if

		if((lowValue != null && highValue != null) && (lowValue).compareTo(highValue) > 0){
			throw new InvalidElementException(miningField);
		} // End if

		if(FieldValueUtil.isMissing(value)){
			throw new TypeCheckException(Number.class, null);
		}

		Number numberValue = value.asNumber();

		switch(outlierTreatmentMethod){
			case AS_MISSING_VALUES:
				if(lowValue != null && numberValue.doubleValue() < lowValue){
					return createMissingInputValue(field, miningField);
				} else

				if(highValue != null && numberValue.doubleValue() > highValue){
					return createMissingInputValue(field, miningField);
				}
				break;
			case AS_EXTREME_VALUES:
				if(lowValue != null && numberValue.doubleValue() < lowValue){
					return createInputValue(field, miningField, lowValue);
				} else

				if(highValue != null && numberValue.doubleValue() > highValue){
					return createInputValue(field, miningField, highValue);
				}
				break;
			default:
				throw new UnsupportedAttributeException(miningField, outlierTreatmentMethod);
		}

		return createInputValue(field, miningField, value);
	}

	static
	public FieldValue performInvalidValueTreatment(Field<?> field, MiningField miningField, Object value){
		InvalidValueTreatmentMethod invalidValueTreatmentMethod = miningField.getInvalidValueTreatment();
		Object invalidValueReplacement = miningField.getInvalidValueReplacement();

		switch(invalidValueTreatmentMethod){
			case AS_IS:
				break;
			case AS_MISSING:
			case RETURN_INVALID:
				if(invalidValueReplacement != null){
					throw new MisplacedAttributeException(miningField, PMMLAttributes.MININGFIELD_INVALIDVALUEREPLACEMENT, invalidValueReplacement);
				}
				break;
			default:
				throw new UnsupportedAttributeException(miningField, invalidValueTreatmentMethod);
		}

		switch(invalidValueTreatmentMethod){
			case RETURN_INVALID:
				throw new InvalidResultException("Field " + PMMLException.formatKey(field.getName()) + " cannot accept user input value " + PMMLException.formatValue(value), miningField);
			case AS_IS:
				if(invalidValueReplacement != null){
					return createInputValue(field, miningField, invalidValueReplacement);
				}
				return createInputValue(field, miningField, value);
			case AS_MISSING:
				return createMissingInputValue(field, miningField);
			default:
				throw new UnsupportedAttributeException(miningField, invalidValueTreatmentMethod);
		}
	}

	static
	public FieldValue performMissingValueTreatment(Field<?> field, MiningField miningField){
		MissingValueTreatmentMethod missingValueTreatmentMethod = miningField.getMissingValueTreatment();
		Object missingValueReplacement = miningField.getMissingValueReplacement();

		if(missingValueTreatmentMethod == null){
			missingValueTreatmentMethod = MissingValueTreatmentMethod.AS_IS;
		}

		switch(missingValueTreatmentMethod){
			case AS_IS:
			case AS_MEAN:
			case AS_MODE:
			case AS_MEDIAN:
			case AS_VALUE:
				return createMissingInputValue(field, miningField);
			case RETURN_INVALID:
				if(missingValueReplacement != null){
					throw new MisplacedAttributeException(miningField, PMMLAttributes.MININGFIELD_MISSINGVALUEREPLACEMENT, missingValueReplacement);
				}
				throw new InvalidResultException("Field " + PMMLException.formatKey(field.getName()) + " requires user input value", miningField);
			default:
				throw new UnsupportedAttributeException(miningField, missingValueTreatmentMethod);
		}
	}

	static
	private Value.Property getStatus(Field<?> field, MiningField miningField, Object value, boolean compatible){

		if(field instanceof DataField){
			DataField dataField = (DataField)field;

			return getStatus(dataField, miningField, value, compatible);
		}

		return (compatible ? Value.Property.VALID : Value.Property.INVALID);
	}

	static
	private Value.Property getStatus(DataField dataField, MiningField miningField, Object value, boolean compatible){
		boolean hasValidSpace = false;

		if(dataField.hasValues()){
			DataType dataType = dataField.getDataType();
			if(dataType == null){
				throw new MissingAttributeException(dataField, PMMLAttributes.DATAFIELD_DATATYPE);
			} // End if

			if(dataField instanceof HasParsedValueMapping){
				HasParsedValueMapping<?> hasParsedValueMapping = (HasParsedValueMapping<?>)dataField;

				OpType opType = dataField.getOpType();
				if(opType == null){
					throw new MissingAttributeException(dataField, PMMLAttributes.DATAFIELD_OPTYPE);
				}

				try {
					FieldValue fieldValue = FieldValueUtil.createOrCast(dataType, opType, value);

					Value pmmlValue = (Value)fieldValue.getMapping(hasParsedValueMapping);
					if(pmmlValue != null){
						return pmmlValue.getProperty();
					}
				} catch(IllegalArgumentException | TypeCheckException e){
					// Ignored
				}
			}

			List<Value> pmmlValues = dataField.getValues();
			for(int i = 0, max = pmmlValues.size(); i < max; i++){
				Value pmmlValue = pmmlValues.get(i);

				Object simpleValue = pmmlValue.getValue();
				if(simpleValue == null){
					throw new MissingAttributeException(pmmlValue, PMMLAttributes.VALUE_VALUE);
				}

				boolean equals;

				Value.Property property = pmmlValue.getProperty();
				switch(property){
					case VALID:
						{
							hasValidSpace = true;

							if(!compatible){
								continue;
							} // End if

							if(value instanceof FieldValue){
								FieldValue fieldValue = (FieldValue)value;

								equals = fieldValue.equalsObject(simpleValue);
							} else

							{
								equals = TypeUtil.equals(dataType, value, simpleValue);
							}
						}
						break;
					case INVALID:
					case MISSING:
						{
							if(value instanceof FieldValue){
								FieldValue fieldValue = (FieldValue)value;

								equals = TypeUtil.equals(dataType, FieldValueUtil.getValue(fieldValue), simpleValue);
							} else

							{
								equals = TypeUtil.equals(dataType, value, simpleValue);
							}
						}
						break;
					default:
						throw new UnsupportedAttributeException(pmmlValue, property);
				}

				if(equals){
					return property;
				}
			}
		} // End if

		if(!compatible){
			return Value.Property.INVALID;
		} // End if

		if(dataField.hasIntervals()){
			PMMLObject locatable = miningField;

			OpType opType = miningField.getOpType();
			if(opType == null){
				locatable = dataField;

				opType = dataField.getOpType();
			}

			switch(opType){
				case CONTINUOUS:
					{
						RangeSet<Double> validRanges = FieldUtil.getValidRanges(dataField);

						Double doubleValue;

						if(value instanceof FieldValue){
							FieldValue fieldValue = (FieldValue)value;

							doubleValue = fieldValue.asDouble();
						} else

						{
							throw new IllegalArgumentException();
						}

						// "If intervals are present, then a value that is outside the intervals is considered invalid"
						return (validRanges.contains(doubleValue) ? Value.Property.VALID : Value.Property.INVALID);
					}
				case CATEGORICAL:
				case ORDINAL:
					// "Intervals are not allowed for non-continuous fields"
					throw new InvalidElementException(dataField);
				default:
					throw new UnsupportedAttributeException(locatable, opType);
			}
		}

		// "If a field contains at least one Value element where the value of property is valid, then the set of Value elements completely defines the set of valid values"
		if(hasValidSpace){
			return Value.Property.INVALID;
		}

		// "Any value is valid by default"
		return Value.Property.VALID;
	}

	static
	private FieldValue createInputValue(Field<?> field, MiningField miningField, Object value){

		if(FieldValueUtil.isMissing(value)){
			return FieldValues.MISSING_VALUE;
		}

		TypeInfo typeInfo = new TypeInfo(){

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

		return FieldValueUtil.createOrCast(typeInfo, value);
	}

	static
	private FieldValue createMissingInputValue(Field<?> field, MiningField miningField){
		Object missingValueReplacement = miningField.getMissingValueReplacement();

		return createInputValue(field, miningField, missingValueReplacement);
	}
}