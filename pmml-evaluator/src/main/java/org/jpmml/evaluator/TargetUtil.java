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
import org.dmg.pmml.MiningField;
import org.dmg.pmml.Target;
import org.dmg.pmml.TargetValue;

public class TargetUtil {

	private TargetUtil(){
	}

	static
	public Map<FieldName, ?> evaluateRegressionDefault(ModelEvaluationContext context){
		return evaluateRegression((Double)null, context);
	}

	static
	public Map<FieldName, ?> evaluateRegression(Double value, ModelEvaluationContext context){
		ModelEvaluator<?> evaluator = context.getModelEvaluator();

		return evaluateRegression(evaluator.getTargetField(), value, context);
	}

	static
	public Map<FieldName, ?> evaluateRegression(TargetField targetField, Double value, EvaluationContext context){
		return Collections.singletonMap(targetField.getName(), evaluateRegressionInternal(targetField, value, context));
	}

	static
	public Object evaluateRegressionInternal(TargetField targetField, Object value, EvaluationContext context){
		DataField dataField = targetField.getDataField();
		MiningField miningField = targetField.getMiningField();
		Target target = targetField.getTarget();

		if(target != null){

			if(value == null){
				value = getDefaultValue(target);
			} // End if

			if(value != null){
				value = processValue(target, (Double)value);
			}
		} // End if

		if(value != null){
			value = TypeUtil.cast(dataField.getDataType(), value);
		}

		return value;
	}

	static
	public Map<FieldName, ? extends Classification> evaluateClassificationDefault(ModelEvaluationContext context){
		return evaluateClassification((Classification)null, context);
	}

	static
	public Map<FieldName, ? extends Classification> evaluateClassification(Classification value, ModelEvaluationContext context){
		ModelEvaluator<?> evaluator = context.getModelEvaluator();

		return evaluateClassification(evaluator.getTargetField(), value, context);
	}

	static
	public Map<FieldName, ? extends Classification> evaluateClassification(TargetField targetField, Classification value, EvaluationContext context){
		return Collections.singletonMap(targetField.getName(), evaluateClassificationInternal(targetField, value, context));
	}

	static
	public Classification evaluateClassificationInternal(TargetField targetField, Classification value, EvaluationContext context){
		DataField dataField = targetField.getDataField();
		MiningField miningField = targetField.getMiningField();
		Target target = targetField.getTarget();

		if(target != null){

			if(value == null){
				value = getPriorProbabilities(target);
			}
		} // End if

		if(value != null){
			value.computeResult(dataField.getDataType());
		}

		return value;
	}

	static
	public Double processValue(Target target, Double value){
		double result = value;

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
	private Double getDefaultValue(Target target){

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

		return targetValue.getDefaultValue();
	}

	static
	private ProbabilityDistribution getPriorProbabilities(Target target){

		if(!target.hasTargetValues()){
			return null;
		}

		ProbabilityDistribution result = new ProbabilityDistribution();

		List<TargetValue> targetValues = target.getTargetValues();
		for(TargetValue targetValue : targetValues){

			// "The defaultValue attribute is used only if the optype of the field is continuous"
			if(targetValue.getDefaultValue() != null){
				throw new InvalidFeatureException(targetValue);
			}

			String targetCategory = targetValue.getValue();
			Double probability = targetValue.getPriorProbability();

			if(targetCategory == null || probability == null){
				continue;
			}

			result.put(targetCategory, probability);
		}

		if(result.isEmpty()){
			return null;
		}

		return result;
	}
}