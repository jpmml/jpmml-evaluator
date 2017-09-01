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
import java.util.Map;

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Target;
import org.dmg.pmml.TargetValue;

public class TargetUtil {

	private TargetUtil(){
	}

	static
	public <V extends Number> Map<FieldName, ?> evaluateRegressionDefault(ValueFactory<V> valueFactory, TargetField targetField){
		Target target = targetField.getTarget();

		if(target != null && target.hasTargetValues()){
			Value<V> value = getDefaultValue(valueFactory, target);

			if(value != null){
				return evaluateRegression(targetField, value);
			}
		}

		return Collections.singletonMap(targetField.getName(), null);
	}

	static
	public Map<FieldName, ?> evaluateRegression(TargetField targetField, Double value){
		return Collections.singletonMap(targetField.getName(), evaluateRegressionInternal(targetField, value));
	}

	static
	public <V extends Number> Map<FieldName, ?> evaluateRegression(TargetField targetField, Value<V> value){
		return Collections.singletonMap(targetField.getName(), evaluateRegressionInternal(targetField, value));
	}

	static
	public Object evaluateRegressionInternal(TargetField targetField, Double value){
		DataField dataField = targetField.getDataField();
		Target target = targetField.getTarget();

		if(target != null){
			value = processValue(target, value);
		}

		return TypeUtil.cast(dataField.getDataType(), value);
	}

	static
	public <V extends Number> Object evaluateRegressionInternal(TargetField targetField, Value<V> value){
		DataField dataField = targetField.getDataField();
		Target target = targetField.getTarget();

		if(target != null){
			value = processValue(target, value);
		}

		return TypeUtil.cast(dataField.getDataType(), value.getValue());
	}

	static
	public <V extends Number> Map<FieldName, ? extends Classification> evaluateClassificationDefault(ValueFactory<V> valueFactory, TargetField targetField){
		Target target = targetField.getTarget();

		if(target != null && target.hasTargetValues()){
			ProbabilityDistribution result = getPriorProbabilities(valueFactory, target);

			if(result != null){
				return evaluateClassification(targetField, result);
			}
		}

		return Collections.singletonMap(targetField.getName(), null);
	}

	static
	public Map<FieldName, ? extends Classification> evaluateClassification(TargetField targetField, Classification value){
		DataField dataField = targetField.getDataField();

		value.computeResult(dataField.getDataType());

		return Collections.singletonMap(targetField.getName(), value);
	}

	static
	public Double processValue(Target target, Double value){
		double result = value.doubleValue();

		Double min = target.getMin();
		if(min != null){
			result = Math.max(result, min);
		}

		Double max = target.getMax();
		if(max != null){
			result = Math.min(result, max);
		}

		double rescaleFactor = target.getRescaleFactor();
		if(rescaleFactor != 1d){
			result *= rescaleFactor;
		}

		double rescaleConstant = target.getRescaleConstant();
		if(rescaleConstant != 0d){
			result += rescaleConstant;
		}

		Target.CastInteger castInteger = target.getCastInteger();
		if(castInteger == null){

			if(result == value.doubleValue()){
				return value;
			}

			return result;
		}

		switch(castInteger){
			case ROUND:
				return (double)Math.round(result);
			case CEILING:
				return Math.ceil(result);
			case FLOOR:
				return Math.floor(result);
			default:
				throw new UnsupportedFeatureException(target, castInteger);
		}
	}

	static
	public <V extends Number> Value<V> processValue(Target target, Value<V> value){
		Double min = target.getMin();
		Double max = target.getMax();

		if(min != null || max != null){
			value.restrict((min != null ? min : Double.NEGATIVE_INFINITY), (max != null ? max : Double.POSITIVE_INFINITY));
		}

		double rescaleFactor = target.getRescaleFactor();
		if(rescaleFactor != 1d){
			value.multiply(rescaleFactor);
		}

		double rescaleConstant = target.getRescaleConstant();
		if(rescaleConstant != 0d){
			value.add(rescaleConstant);
		}

		Target.CastInteger castInteger = target.getCastInteger();
		if(castInteger == null){
			return value;
		}

		switch(castInteger){
			case ROUND:
				return value.round();
			case CEILING:
				return value.ceiling();
			case FLOOR:
				return value.floor();
			default:
				throw new UnsupportedFeatureException(target, castInteger);
		}
	}

	static
	public TargetValue getTargetValue(Target target, Object value){
		DataType dataType = TypeUtil.getDataType(value);

		List<TargetValue> targetValues = target.getTargetValues();
		for(TargetValue targetValue : targetValues){

			if(TypeUtil.equals(dataType, value, TypeUtil.parseOrCast(dataType, targetValue.getValue()))){
				return targetValue;
			}
		}

		return null;
	}

	static
	private <V extends Number> Value<V> getDefaultValue(ValueFactory<V> valueFactory, Target target){

		if(!target.hasTargetValues()){
			return null;
		}

		List<TargetValue> targetValues = target.getTargetValues();
		if(targetValues.size() != 1){
			throw new InvalidFeatureException(target);
		}

		TargetValue targetValue = targetValues.get(0);

		Double defaultValue = targetValue.getDefaultValue();

		// "The value and priorProbability attributes are used only if the optype of the field is categorical or ordinal"
		if(targetValue.getValue() != null || targetValue.getPriorProbability() != null){
			throw new InvalidFeatureException(targetValue);
		} // End if

		if(defaultValue == null){
			return null;
		}

		return valueFactory.newValue(defaultValue);
	}

	static
	private <V extends Number> ProbabilityDistribution getPriorProbabilities(ValueFactory<V> valueFactory, Target target){

		if(!target.hasTargetValues()){
			return null;
		}

		ProbabilityDistribution result = new ProbabilityDistribution();

		Value<V> sum = valueFactory.newValue();

		List<TargetValue> targetValues = target.getTargetValues();
		for(TargetValue targetValue : targetValues){
			String targetCategory = targetValue.getValue();
			Double probability = targetValue.getPriorProbability();

			if(targetCategory == null || probability == null){
				throw new InvalidFeatureException(targetValue);
			} // End if

			// "The defaultValue attribute is used only if the optype of the field is continuous"
			if(targetValue.getDefaultValue() != null){
				throw new InvalidFeatureException(targetValue);
			}

			Value<V> value = valueFactory.newValue(probability);

			sum.add(value);

			result.put(targetCategory, value.doubleValue());
		}

		if(sum.doubleValue() != 1d){
			throw new InvalidFeatureException(target);
		}

		return result;
	}
}