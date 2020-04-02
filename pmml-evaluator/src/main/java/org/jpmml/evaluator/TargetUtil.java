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

import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMMLAttributes;
import org.dmg.pmml.Target;
import org.dmg.pmml.TargetValue;

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
	public <V extends Number> Map<FieldName, ?> evaluateRegressionDefault(ValueFactory<V> valueFactory, TargetField targetField){
		Target target = targetField.getTarget();

		if(target != null && target.hasTargetValues()){
			Value<V> value = getDefaultValue(valueFactory, target);

			if(value != null){
				return evaluateRegression(targetField, value);
			}
		}

		return Collections.singletonMap(targetField.getFieldName(), null);
	}

	static
	public <V extends Number> Map<FieldName, ?> evaluateRegression(TargetField targetField, Value<V> value){
		value = evaluateRegressionInternal(targetField, value);

		if(value instanceof HasReport){
			Regression<V> result = new Regression<>(value);

			return evaluateRegression(targetField, result);
		}

		Object result = TypeUtil.cast(targetField.getDataType(), value.getValue());

		return Collections.singletonMap(targetField.getFieldName(), result);
	}

	static
	public <V extends Number> Map<FieldName, ? extends Regression<V>> evaluateRegression(TargetField targetField, Regression<V> regression){
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
	public Map<FieldName, ? extends Vote> evaluateVote(TargetField targetField, Vote vote){
		vote.computeResult(targetField.getDataType());

		return Collections.singletonMap(targetField.getFieldName(), vote);
	}

	static
	public <V extends Number> Map<FieldName, ? extends Classification<?, V>> evaluateClassificationDefault(ValueFactory<V> valueFactory, TargetField targetField){
		Target target = targetField.getTarget();

		if(target != null && target.hasTargetValues()){
			ProbabilityDistribution<V> result = getPriorProbabilities(valueFactory, target);

			if(result != null){
				return evaluateClassification(targetField, result);
			}
		}

		return Collections.singletonMap(targetField.getFieldName(), null);
	}

	static
	public <V extends Number> Map<FieldName, ? extends Classification<?, V>> evaluateClassification(TargetField targetField, Classification<?, V> classification){
		classification.computeResult(targetField.getDataType());

		return Collections.singletonMap(targetField.getFieldName(), classification);
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
		for(TargetValue targetValue : targetValues){
			Object simpleValue = targetValue.getValue();
			if(simpleValue == null){
				throw new MissingAttributeException(targetValue, PMMLAttributes.TARGETVALUE_VALUE);
			} // End if

			if((value).equals(TypeUtil.parseOrCast(dataType, simpleValue))){
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

		Number defaultValue = targetValue.getDefaultValue();

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

		ValueMap<Object, V> values = new ValueMap<>();

		Value<V> sum = valueFactory.newValue();

		List<TargetValue> targetValues = target.getTargetValues();
		for(TargetValue targetValue : targetValues){
			Object targetCategory = targetValue.getValue();
			if(targetCategory == null){
				throw new MissingAttributeException(targetValue, PMMLAttributes.TARGETVALUE_VALUE);
			}

			Number probability = targetValue.getPriorProbability();
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