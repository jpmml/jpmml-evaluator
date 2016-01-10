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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
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
import org.dmg.pmml.Field;
import org.dmg.pmml.Interval;
import org.dmg.pmml.InvalidValueTreatmentMethodType;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MissingValueTreatmentMethodType;
import org.dmg.pmml.OpType;
import org.dmg.pmml.OutlierTreatmentMethodType;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.Target;
import org.dmg.pmml.TypeDefinitionField;
import org.dmg.pmml.Value;

public class FieldValueUtil {

	private FieldValueUtil(){
	}

	static
	public FieldValue prepare(DataField dataField, MiningField miningField, Object value){
		DataType dataType = dataField.getDataType();
		OpType opType = dataField.getOpType();

		if(dataType == null || opType == null){
			throw new InvalidFeatureException(dataField);
		}

		value = safeParseOrCast(dataType, value);

		Value.Property status = getStatus(dataField, miningField, value);
		switch(status){
			case VALID:
				return performValidValueTreatment(dataField, miningField, value);
			case INVALID:
				return performInvalidValueTreatment(dataField, miningField, value);
			case MISSING:
				return performMissingValueTreatment(dataField, miningField);
			default:
				break;
		}

		throw new EvaluationException();
	}

	static
	public FieldValue performValidValueTreatment(Field field, MiningField miningField, Object value){
		OutlierTreatmentMethodType outlierTreatmentMethod = miningField.getOutlierTreatment();

		Double lowValue = miningField.getLowValue();
		Double highValue = miningField.getHighValue();

		Double doubleValue = null;

		switch(outlierTreatmentMethod){
			case AS_MISSING_VALUES:
			case AS_EXTREME_VALUES:
				{
					if(lowValue == null || highValue == null){
						throw new InvalidFeatureException(miningField);
					} // End if

					if((lowValue).compareTo(highValue) > 0){
						throw new InvalidFeatureException(miningField);
					}

					doubleValue = (Double)TypeUtil.parseOrCast(DataType.DOUBLE, value);
				}
				break;
			default:
				break;
		} // End switch

		switch(outlierTreatmentMethod){
			case AS_IS:
				break;
			case AS_MISSING_VALUES:
				{
					if(TypeUtil.compare(DataType.DOUBLE, doubleValue, lowValue) < 0 || TypeUtil.compare(DataType.DOUBLE, doubleValue, highValue) > 0){
						return createMissingActiveValue(field, miningField);
					}
				}
				break;
			case AS_EXTREME_VALUES:
				{
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

		return createActiveValue(field, miningField, value);
	}

	static
	public FieldValue performInvalidValueTreatment(Field field, MiningField miningField, Object value){
		InvalidValueTreatmentMethodType invalidValueTreatmentMethod = miningField.getInvalidValueTreatment();

		switch(invalidValueTreatmentMethod){
			case AS_IS:
				return createActiveValue(field, miningField, value);
			case AS_MISSING:
				return createMissingActiveValue(field, miningField);
			case RETURN_INVALID:
				throw new InvalidResultException(miningField);
			default:
				throw new UnsupportedFeatureException(miningField, invalidValueTreatmentMethod);
		}
	}

	static
	public FieldValue performMissingValueTreatment(Field field, MiningField miningField){
		MissingValueTreatmentMethodType missingValueTreatmentMethod = miningField.getMissingValueTreatment();

		if(missingValueTreatmentMethod == null){
			missingValueTreatmentMethod = MissingValueTreatmentMethodType.AS_IS;
		}

		switch(missingValueTreatmentMethod){
			case AS_IS:
			case AS_MEAN:
			case AS_MEDIAN:
			case AS_MODE:
			case AS_VALUE:
				return createMissingActiveValue(field, miningField);
			default:
				throw new UnsupportedFeatureException(miningField, missingValueTreatmentMethod);
		}
	}

	@SuppressWarnings (
		value = {"fallthrough"}
	)
	static
	public Value.Property getStatus(DataField dataField, MiningField miningField, Object value){

		if(value == null){
			return Value.Property.MISSING;
		}

		boolean hasValidSpace = false;

		if(dataField.hasValues()){
			DataType dataType = dataField.getDataType();
			OpType opType = dataField.getOpType();

			if(dataField instanceof HasParsedValueMapping){
				HasParsedValueMapping<?> hasParsedValueMapping = (HasParsedValueMapping<?>)dataField;

				Value fieldValue = getValidValue(hasParsedValueMapping, dataType, opType, value);
				if(fieldValue != null){
					return Value.Property.VALID;
				}
			}

			List<Value> fieldValues = dataField.getValues();
			for(Value fieldValue : fieldValues){
				Value.Property property = fieldValue.getProperty();

				switch(property){
					case VALID:
						hasValidSpace = true;
						// Falls through
					case INVALID:
					case MISSING:
						{
							boolean equals = equals(dataType, value, fieldValue.getValue());

							if(equals){
								return property;
							}
						}
						break;
					default:
						throw new UnsupportedFeatureException(fieldValue, property);
				}
			}
		}

		PMMLObject locatable = miningField;

		OpType opType = miningField.getOpType();
		if(opType == null){
			locatable = dataField;

			opType = dataField.getOpType();
		}

		switch(opType){
			case CONTINUOUS:
				{
					// "If intervals are present, then a value that is outside the intervals is considered invalid"
					if(dataField.hasIntervals()){
						RangeSet<Double> validRanges = getValidRanges(dataField);

						Double doubleValue = (Double)TypeUtil.parseOrCast(DataType.DOUBLE, value);

						return (validRanges.contains(doubleValue) ? Value.Property.VALID : Value.Property.INVALID);
					}
				}
				break;
			case CATEGORICAL:
			case ORDINAL:
				{
					// "Intervals are not allowed for non-continuous fields"
					if(dataField.hasIntervals()){
						throw new InvalidFeatureException(dataField);
					}
				}
				break;
			default:
				throw new UnsupportedFeatureException(locatable, opType);
		}

		// "If a field contains at least one Value element where the value of property is valid, then the set of Value elements completely defines the set of valid values"
		if(hasValidSpace){
			return Value.Property.INVALID;
		}

		// "Any value is valid by default"
		return Value.Property.VALID;
	}

	static
	public FieldValue createActiveValue(Field field, MiningField miningField, Object value){

		if(value == null){
			return null;
		}

		DataType dataType = field.getDataType();
		OpType opType = field.getOpType();

		// "A MiningField overrides a DataField"
		if(miningField != null){
			opType = firstNonNull(miningField.getOpType(), opType);
		}

		return create(dataType, opType, value);
	}

	static
	public FieldValue createMissingActiveValue(Field field, MiningField miningField){
		return createActiveValue(field, miningField, miningField.getMissingValueReplacement());
	}

	static
	public FieldValue createTargetValue(Field field, MiningField miningField, Target target, Object value){

		if(value == null){
			return null;
		}

		DataType dataType = field.getDataType();
		OpType opType = field.getOpType();

		// "A MiningField overrides a DataField, and a Target overrides a MiningField"
		if(miningField != null){
			opType = firstNonNull(miningField.getOpType(), opType);

			if(target != null){
				opType = firstNonNull(target.getOpType(), opType);
			}
		}

		return create(dataType, opType, value);
	}

	static
	public FieldValue create(Object value){
		return create((DataType)null, (OpType)null, value);
	}

	static
	public List<FieldValue> createAll(List<?> values){
		Function<Object, FieldValue> function = new Function<Object, FieldValue>(){

			@Override
			public FieldValue apply(Object value){
				return create(value);
			}
		};

		return Lists.transform(values, function);
	}

	static
	public FieldValue create(Field field, Object value){
		FieldValue result = create(field.getDataType(), field.getOpType(), value);

		if(field instanceof TypeDefinitionField){
			return enhance((TypeDefinitionField)field, result);
		}

		return result;
	}

	static
	public FieldValue create(DataType dataType, OpType opType, Object value){

		if(value == null){
			return null;
		} // End if

		if(value instanceof Collection){
			Collection<?> collection = (Collection<?>)value;

			if(dataType == null){
				Object firstElement = Iterables.getFirst(collection, null);

				if(firstElement != null){
					dataType = TypeUtil.getDataType(firstElement);
				} else

				{
					dataType = DataType.STRING;
				}
			} // End if

			if(opType == null){
				opType = TypeUtil.getOpType(dataType);
			}
		} else

		{
			if(dataType == null){
				dataType = TypeUtil.getDataType(value);
			} else

			{
				value = TypeUtil.parseOrCast(dataType, value);
			} // End if

			if(opType == null){
				opType = TypeUtil.getOpType(dataType);
			}
		}

		switch(opType){
			case CONTINUOUS:
				return ContinuousValue.create(dataType, value);
			case CATEGORICAL:
				return CategoricalValue.create(dataType, value);
			case ORDINAL:
				return OrdinalValue.create(dataType, value);
			default:
				break;
		}

		throw new EvaluationException();
	}

	static
	public FieldValue refine(Field field, FieldValue value){
		FieldValue result = refine(field.getDataType(), field.getOpType(), value);

		if((field instanceof TypeDefinitionField) && (result != value)){
			return enhance((TypeDefinitionField)field, result);
		}

		return result;
	}

	static
	public FieldValue refine(DataType dataType, OpType opType, FieldValue value){

		if(value == null){
			return null;
		}

		DataType valueDataType = value.getDataType();
		OpType valueOpType = value.getOpType();

		DataType refinedDataType = firstNonNull(dataType, valueDataType);
		OpType refinedOpType = firstNonNull(opType, valueOpType);

		if((refinedDataType).equals(valueDataType) && (refinedOpType).equals(valueOpType)){
			return value;
		}

		return create(refinedDataType, refinedOpType, value.getValue());
	}

	static
	private FieldValue enhance(TypeDefinitionField field, FieldValue value){

		if(value == null){
			return null;
		} // End if

		if(value instanceof OrdinalValue){
			OrdinalValue ordinalValue = (OrdinalValue)value;

			ordinalValue.setOrdering(getOrdering(field, ordinalValue.getDataType()));
		}

		return value;
	}

	static
	public Object getValue(FieldValue value){
		return (value != null ? value.getValue() : null);
	}

	static
	public <V> V getValue(Class<? extends V> clazz, FieldValue value){
		return TypeUtil.cast(clazz, getValue(value));
	}

	static
	public Value getValidValue(TypeDefinitionField field, Object value){

		if(field.hasValues()){
			DataType dataType = field.getDataType();
			OpType opType = field.getOpType();

			value = safeParseOrCast(dataType, value);

			if(field instanceof HasParsedValueMapping){
				HasParsedValueMapping<?> hasParsedValueMapping = (HasParsedValueMapping<?>)field;

				return getValidValue(hasParsedValueMapping, dataType, opType, value);
			}

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
					case INVALID:
					case MISSING:
						break;
					default:
						throw new UnsupportedFeatureException(fieldValue, property);
				}
			}
		}

		return null;
	}

	static
	private Value getValidValue(HasParsedValueMapping<?> hasParsedValueMapping, DataType dataType, OpType opType, Object object){
		FieldValue value;

		try {
			value = FieldValueUtil.create(dataType, opType, object);
		} catch(IllegalArgumentException iae){
			return null;
		}

		Map<FieldValue, ?> fieldValues = hasParsedValueMapping.getValueMapping(dataType, opType);

		return (Value)fieldValues.get(value);
	}

	static
	public List<Value> getValidValues(TypeDefinitionField field){

		if(field.hasValues()){
			List<Value> result = new ArrayList<>();

			List<Value> fieldValues = field.getValues();
			for(Value fieldValue : fieldValues){
				Value.Property property = fieldValue.getProperty();

				switch(property){
					case VALID:
						result.add(fieldValue);
						break;
					case INVALID:
					case MISSING:
						break;
					default:
						throw new UnsupportedFeatureException(fieldValue, property);
				}
			}

			return result;
		}

		return Collections.emptyList();
	}

	static
	public List<String> getTargetCategories(TypeDefinitionField field){
		return CacheUtil.getValue(field, FieldValueUtil.targetCategoryCache);
	}

	static
	public RangeSet<Double> getValidRanges(DataField dataField){
		return CacheUtil.getValue(dataField, FieldValueUtil.validRangeCache);
	}

	static
	private Object safeParseOrCast(DataType dataType, Object value){

		if(value != null){

			try {
				return TypeUtil.parseOrCast(dataType, value);
			} catch(IllegalArgumentException iae){
				// Ignored
			}
		}

		return value;
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
	private <V> V firstNonNull(V value, V defaultValue){

		if(value != null){
			return value;
		}

		return defaultValue;
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

	static
	private List<?> getOrdering(TypeDefinitionField field, final DataType dataType){
		List<Value> values = getValidValues(field);
		if(values.isEmpty()){
			return null;
		}

		Function<Value, Object> function = new Function<Value, Object>(){

			@Override
			public Object apply(Value value){
				return TypeUtil.parse(dataType, value.getValue());
			}
		};

		return Lists.newArrayList(Iterables.transform(values, function));
	}

	private static final LoadingCache<TypeDefinitionField, List<String>> targetCategoryCache = CacheUtil.buildLoadingCache(new CacheLoader<TypeDefinitionField, List<String>>(){

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

	private static final LoadingCache<DataField, RangeSet<Double>> validRangeCache = CacheUtil.buildLoadingCache(new CacheLoader<DataField, RangeSet<Double>>(){

		@Override
		public RangeSet<Double> load(DataField dataField){
			return ImmutableRangeSet.copyOf(parseValidRanges(dataField));
		}
	});
}