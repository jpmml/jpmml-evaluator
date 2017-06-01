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
import org.dmg.pmml.MathContext;
import org.dmg.pmml.Target;
import org.dmg.pmml.TargetValue;

public class TargetUtil {

	private TargetUtil(){
	}

	static
	public Map<FieldName, ?> evaluateRegressionDefault(TargetField targetField, MathContext mathContext){
		Target target = targetField.getTarget();

		if(target != null && target.hasTargetValues()){
			Value<?> value = getDefaultValue(target, mathContext);

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
	public Map<FieldName, ?> evaluateRegression(TargetField targetField, Value<?> value){
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
	public Object evaluateRegressionInternal(TargetField targetField, Value<?> value){
		DataField dataField = targetField.getDataField();
		Target target = targetField.getTarget();

		if(target != null){
			value = processValue(target, value);
		}

		return TypeUtil.cast(dataField.getDataType(), value.getValue());
	}

	static
	public Map<FieldName, ? extends Classification> evaluateClassificationDefault(TargetField targetField, MathContext mathContext){
		Target target = targetField.getTarget();

		if(target != null && target.hasTargetValues()){
			ProbabilityDistribution result = getPriorProbabilities(target, mathContext);

			if(result != null){
				return evaluateClassification(targetField, result);
			}
		}

		return Collections.singletonMap(targetField.getName(), null);
	}

	static
	public Map<FieldName, ? extends Classification> evaluateClassification(TargetField targetField, Classification value){
		return Collections.singletonMap(targetField.getName(), evaluateClassificationInternal(targetField, value));
	}

	static
	public Classification evaluateClassificationInternal(TargetField targetField, Classification value){
		DataField dataField = targetField.getDataField();

		value.computeResult(dataField.getDataType());

		return value;
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

		Double rescaleFactor = target.getRescaleFactor();
		if(rescaleFactor != null){
			result *= rescaleFactor;
		}

		Double rescaleConstant = target.getRescaleConstant();
		if(rescaleConstant != null){
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
	public Value<?> processValue(Target target, Value<?> value){
		double result = value.doubleValue();

		Double min = target.getMin();
		if(min != null){
			value.restrict(min, Double.MAX_VALUE);
		}

		Double max = target.getMax();
		if(max != null){
			value.restrict(-Double.MAX_VALUE, max);
		}

		Double rescaleFactor = target.getRescaleFactor();
		if(rescaleFactor != null){
			value.multiply(rescaleFactor);
		}

		Double rescaleConstant = target.getRescaleConstant();
		if(rescaleConstant != null){
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
	private Value<?> getDefaultValue(Target target, MathContext mathContext){

		if(!target.hasTargetValues()){
			return null;
		}

		List<TargetValue> targetValues = target.getTargetValues();
		if(targetValues.size() != 1){
			throw new InvalidFeatureException(target);
		}

		TargetValue targetValue = targetValues.get(0);

		// "The value and priorProbability attributes are used only if the optype of the field is categorical or ordinal"
		if(targetValue.getValue() != null || targetValue.getPriorProbability() != null){
			throw new InvalidFeatureException(targetValue);
		}

		Double defaultValue = targetValue.getDefaultValue();
		if(defaultValue == null){
			return null;
		}

		ValueFactory<?> valueFactory = ValueFactory.getInstance(mathContext);

		return valueFactory.newValue(defaultValue);
	}

	static
	private ProbabilityDistribution getPriorProbabilities(Target target, MathContext mathContext){

		if(!target.hasTargetValues()){
			return null;
		}

		ProbabilityDistribution result = new ProbabilityDistribution();

		ValueFactory<?> valueFactory = ValueFactory.getInstance(mathContext);

		Value<?> sum = valueFactory.newValue(0d);

		List<TargetValue> targetValues = target.getTargetValues();
		for(TargetValue targetValue : targetValues){

			// "The defaultValue attribute is used only if the optype of the field is continuous"
			if(targetValue.getDefaultValue() != null){
				throw new InvalidFeatureException(targetValue);
			}

			String targetCategory = targetValue.getValue();
			Double probability = targetValue.getPriorProbability();

			if(targetCategory == null || probability == null){
				throw new InvalidFeatureException(targetValue);
			}

			Value<?> value = valueFactory.newValue(probability);

			sum.add(value);

			result.put(targetCategory, value.doubleValue());
		}

		if(sum.doubleValue() != 1d){
			throw new InvalidFeatureException(target);
		}

		return result;
	}
}