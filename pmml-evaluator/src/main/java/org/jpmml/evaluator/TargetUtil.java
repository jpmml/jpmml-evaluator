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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.DataType;
import org.dmg.pmml.Target;
import org.dmg.pmml.TargetValue;
import org.jpmml.model.InvalidElementListException;
import org.jpmml.model.UnsupportedAttributeException;

public class TargetUtil {

	private TargetUtil(){
	}

	static
	public void computeResult(DataType dataType, Regression<?> regression){
		regression.computeResult(dataType);
	}

	static
	public void computeResult(DataType dataType, Classification<?, ?> classification){
		classification.computeResult(dataType);
	}

	static
	public void computeResult(DataType dataType, Vote vote){
		vote.computeResult(dataType);
	}

	static
	public <V extends Number> Map<String, ?> evaluateRegressionDefault(ValueFactory<V> valueFactory, TargetField targetField){
		Target target = targetField.getTarget();

		if(target != null && target.hasTargetValues()){
			Value<V> value = getDefaultValue(valueFactory, target);

			return evaluateRegression(targetField, value);
		}

		return Collections.singletonMap(targetField.getFieldName(), null);
	}

	static
	public <V extends Number> Map<String, ?> evaluateRegressionDefault(ValueFactory<V> valueFactory, List<TargetField> targetFields){

		if(targetFields.size() == 1){
			return evaluateRegressionDefault(valueFactory, targetFields.get(0));
		}

		Map<String, Object> result = new LinkedHashMap<>();

		for(TargetField targetField : targetFields){
			result.putAll(evaluateRegressionDefault(valueFactory, targetField));
		}

		return result;
	}

	static
	public <V extends Number> Map<String, ?> evaluateRegression(TargetField targetField, Value<V> value){
		value = evaluateRegressionInternal(targetField, value);

		if(value instanceof HasReport){
			Regression<V> result = new Regression<>(value);

			return evaluateRegression(targetField, result);
		}

		Object result = TypeUtil.cast(targetField.getDataType(), value.getValue());

		return Collections.singletonMap(targetField.getFieldName(), result);
	}

	static
	public <V extends Number> Map<String, ? extends Regression<V>> evaluateRegression(TargetField targetField, Regression<V> regression){
		regression.computeResult(targetField.getDataType());

		return Collections.singletonMap(targetField.getFieldName(), regression);
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
	public Map<String, ? extends Vote> evaluateVote(TargetField targetField, Vote vote){
		vote.computeResult(targetField.getDataType());

		return Collections.singletonMap(targetField.getFieldName(), vote);
	}

	static
	public <V extends Number> Map<String, ? extends Classification<?, V>> evaluateClassificationDefault(ValueFactory<V> valueFactory, TargetField targetField){
		Target target = targetField.getTarget();

		if(target != null && target.hasTargetValues()){
			ProbabilityDistribution<V> result = getPriorProbabilities(valueFactory, target);

			return evaluateClassification(targetField, result);
		}

		return Collections.singletonMap(targetField.getFieldName(), null);
	}

	static
	public <V extends Number> Map<String, ? extends Classification<?, V>> evaluateClassificationDefault(ValueFactory<V> valueFactory, List<TargetField> targetFields){

		if(targetFields.size() == 1){
			return evaluateClassificationDefault(valueFactory, targetFields.get(0));
		}

		Map<String, Classification<?, V>> result = new LinkedHashMap<>();

		for(TargetField targetField : targetFields){
			result.putAll(evaluateClassificationDefault(valueFactory, targetField));
		}

		return result;
	}

	static
	public <V extends Number> Map<String, ? extends Classification<?, V>> evaluateClassification(TargetField targetField, Classification<?, V> classification){
		classification.computeResult(targetField.getDataType());

		return Collections.singletonMap(targetField.getFieldName(), classification);
	}

	static
	public Map<String, ?> evaluateDefault(TargetField targetField){
		return Collections.singletonMap(targetField.getFieldName(), null);
	}

	static
	public <V extends Number> Map<String, ?> evaluateDefault(List<TargetField> targetFields){

		if(targetFields.size() == 1){
			return evaluateDefault(targetFields.get(0));
		}

		Map<String, Object> result = new LinkedHashMap<>();

		for(TargetField targetField : targetFields){
			result.put(targetField.getFieldName(), null);
		}

		return result;
	}

	static
	public <V extends Number> Value<V> processValue(Target target, Value<V> value){
		Number min = target.getMin();
		Number max = target.getMax();

		if(min != null || max != null){
			value.restrict((min != null ? min : Double.NEGATIVE_INFINITY), (max != null ? max : Double.POSITIVE_INFINITY));
		}

		value
			.multiply(target.getRescaleFactor())
			.add(target.getRescaleConstant());

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
		for(int i = 0, max = targetValues.size(); i < max; i++){
			TargetValue targetValue = targetValues.get(i);

			Object objectValue = targetValue.requireValue();

			if((value).equals(TypeUtil.parseOrCast(dataType, objectValue))){
				return targetValue;
			}
		}

		return null;
	}

	static
	private <V extends Number> Value<V> getDefaultValue(ValueFactory<V> valueFactory, Target target){
		List<TargetValue> targetValues = target.getTargetValues();
		if(targetValues.size() != 1){
			throw new InvalidElementListException(targetValues);
		}

		TargetValue targetValue = targetValues.get(0);

		Number defaultValue = targetValue.requireDefaultValue();

		return valueFactory.newValue(defaultValue);
	}

	static
	private <V extends Number> ProbabilityDistribution<V> getPriorProbabilities(ValueFactory<V> valueFactory, Target target){
		ValueMap<Object, V> values = new ValueMap<>();

		Value<V> sum = valueFactory.newValue();

		List<TargetValue> targetValues = target.getTargetValues();
		for(int i = 0, max = targetValues.size(); i < max; i++){
			TargetValue targetValue = targetValues.get(i);

			Number priorProbability = targetValue.requirePriorProbability();

			Value<V> value = valueFactory.newValue(priorProbability);

			values.put(targetValue.requireValue(), value);

			sum.add(value);
		}

		if(!sum.isOne()){

			if(sum.isZero()){
				throw new UndefinedResultException();
			}

			for(Value<V> value : values){
				value.divide(sum);
			}
		}

		return new ProbabilityDistribution<>(values);
	}
}