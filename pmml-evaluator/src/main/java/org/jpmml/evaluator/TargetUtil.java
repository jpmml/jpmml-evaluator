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
	public <V extends Number> Map<FieldName, ?> evaluateRegression(TargetField targetField, Value<V> value){
		DataField dataField = targetField.getDataField();

		value = evaluateRegressionInternal(targetField, value);

		if(value instanceof HasReport){
			Regression<V> result = new Regression<>(value);

			return evaluateRegression(targetField, result);
		}

		Object result = TypeUtil.cast(dataField.getDataType(), value.getValue());

		return Collections.singletonMap(targetField.getName(), result);
	}

	static
	public <V extends Number> Map<FieldName, ? extends Regression<V>> evaluateRegression(TargetField targetField, Regression<V> regression){
		DataField dataField = targetField.getDataField();

		regression.computeResult(dataField.getDataType());

		return Collections.singletonMap(targetField.getName(), regression);
	}

	static
	public <V extends Number> Value<V> evaluateRegressionInternal(TargetField targetField, Value<V> value){
		Target target = targetField.getTarget();

		if(target != null){
			return processValue(target, value);
		}

		return value;
	}

	static
	public Map<FieldName, ? extends Vote> evaluateVote(TargetField targetField, Vote vote){
		DataField dataField = targetField.getDataField();

		vote.computeResult(dataField.getDataType());

		return Collections.singletonMap(targetField.getName(), vote);
	}

	static
	public <V extends Number> Map<FieldName, ? extends Classification<V>> evaluateClassificationDefault(ValueFactory<V> valueFactory, TargetField targetField){
		Target target = targetField.getTarget();

		if(target != null && target.hasTargetValues()){
			ProbabilityDistribution<V> result = getPriorProbabilities(valueFactory, target);

			if(result != null){
				return evaluateClassification(targetField, result);
			}
		}

		return Collections.singletonMap(targetField.getName(), null);
	}

	static
	public <V extends Number> Map<FieldName, ? extends Classification<V>> evaluateClassification(TargetField targetField, Classification<V> classification){
		DataField dataField = targetField.getDataField();

		classification.computeResult(dataField.getDataType());

		return Collections.singletonMap(targetField.getName(), classification);
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
				throw new UnsupportedAttributeException(target, castInteger);
		}
	}

	static
	public TargetValue getTargetValue(Target target, Object value){
		DataType dataType = TypeUtil.getDataType(value);

		List<TargetValue> targetValues = target.getTargetValues();
		for(TargetValue targetValue : targetValues){
			String stringValue = targetValue.getValue();
			if(stringValue == null){
				throw new MissingAttributeException(targetValue, PMMLAttributes.TARGETVALUE_VALUE);
			} // End if

			if((value).equals(TypeUtil.parse(dataType, stringValue))){
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
			throw new InvalidElementListException(targetValues);
		}

		TargetValue targetValue = targetValues.get(0);

		Double defaultValue = targetValue.getDefaultValue();

		// "The value and priorProbability attributes are used only if the optype of the field is categorical or ordinal"
		if(targetValue.getValue() != null || targetValue.getPriorProbability() != null){
			throw new InvalidElementException(targetValue);
		} // End if

		if(defaultValue == null){
			return null;
		}

		return valueFactory.newValue(defaultValue);
	}

	static
	private <V extends Number> ProbabilityDistribution<V> getPriorProbabilities(ValueFactory<V> valueFactory, Target target){

		if(!target.hasTargetValues()){
			return null;
		}

		ValueMap<String, V> values = new ValueMap<>();

		Value<V> sum = valueFactory.newValue();

		List<TargetValue> targetValues = target.getTargetValues();
		for(TargetValue targetValue : targetValues){
			String targetCategory = targetValue.getValue();
			if(targetCategory == null){
				throw new MissingAttributeException(targetValue, PMMLAttributes.TARGETVALUE_VALUE);
			}

			Double probability = targetValue.getPriorProbability();
			if(probability == null){
				throw new MissingAttributeException(targetValue, PMMLAttributes.TARGETVALUE_PRIORPROBABILITY);
			}

			// "The defaultValue attribute is used only if the optype of the field is continuous"
			if(targetValue.getDefaultValue() != null){
				throw new InvalidElementException(targetValue);
			}

			Value<V> value = valueFactory.newValue(probability);

			values.put(targetCategory, value);

			sum.add(value);
		}

		if(!sum.equals(1d)){

			if(sum.equals(0d)){
				throw new UndefinedResultException();
			}

			for(Value<V> value : values){
				value.divide(sum);
			}
		}

		return new ProbabilityDistribution<>(values);
	}
}