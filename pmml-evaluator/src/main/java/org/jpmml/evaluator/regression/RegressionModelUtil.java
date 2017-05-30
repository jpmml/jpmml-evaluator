/*
 * Copyright (c) 2017 Villu Ruusmann
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
package org.jpmml.evaluator.regression;

import java.util.Iterator;

import org.dmg.pmml.regression.RegressionModel;
import org.jpmml.evaluator.EvaluationException;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueMap;

public class RegressionModelUtil {

	private RegressionModelUtil(){
	}

	static
	public ValueMap computeBinomialProbabilities(ValueMap values, RegressionModel.NormalizationMethod normalizationMethod){

		if(values.size() != 2){
			throw new EvaluationException();
		}

		Iterator<Value<?>> valueIt = values.iterator();

		Value<?> firstValue = valueIt.next();

		// The probability of the first category is calculated
		normalizeBinaryLogisticClassificationResult(firstValue, normalizationMethod);

		Value<?> secondValue = valueIt.next();

		// The probability of the second category is obtained by subtracting the probability of the first category from 1.0
		secondValue.residual(firstValue);

		return values;
	}

	static
	public ValueMap computeMultinomialProbabilities(ValueMap values, RegressionModel.NormalizationMethod normalizationMethod){

		if(values.size() < 2){
			throw new EvaluationException();
		}

		switch(normalizationMethod){
			case NONE:
				{
					Value<?> sum = null;

					Iterator<Value<?>> valueIt = values.iterator();
					for(int i = 0, max = values.size() - 1; i < max; i++){
						Value<?> value = valueIt.next();

						if(sum == null){
							sum = value.copy();
						} else

						{
							sum.add(value);
						}
					}

					Value<?> lastValue = valueIt.next();

					lastValue.residual(sum);
				}
				break;
			case SIMPLEMAX:
			case SOFTMAX:
				{
					Value<?> sum = null;

					for(Value<?> value : values){

						if((RegressionModel.NormalizationMethod.SOFTMAX).equals(normalizationMethod)){
							value.exp();
						} // End if

						if(sum == null){
							sum = value.copy();
						} else

						{
							sum.add(value);
						}
					} // End for

					for(Value<?> value : values){
						value.divide(sum);
					}
				}
				break;
			default:
				throw new EvaluationException();
		}

		return values;
	}

	static
	public ValueMap computeOrdinalProbabilities(ValueMap values, RegressionModel.NormalizationMethod normalizationMethod){

		if(values.size() < 2){
			throw new EvaluationException();
		}

		switch(normalizationMethod){
			case NONE:
			case LOGIT:
			case PROBIT:
			case CLOGLOG:
			case LOGLOG:
			case CAUCHIT:
				{
					Value<?> sum = null;

					Iterator<Value<?>> valueIt = values.iterator();
					for(int i = 0, max = values.size() - 1; i < max; i++){
						Value<?> value = valueIt.next();

						normalizeBinaryLogisticClassificationResult(value, normalizationMethod);

						if(sum == null){
							sum = value.copy();
						} else

						{
							value.subtract(sum);

							sum.add(value);
						}
					}

					Value<?> lastValue = valueIt.next();

					lastValue.residual(sum);
				}
				break;
			default:
				throw new EvaluationException();
		}

		return values;
	}

	static
	public <V extends Number> Value<V> normalizeRegressionResult(Value<V> value, RegressionModel.NormalizationMethod normalizationMethod){

		switch(normalizationMethod){
			case NONE:
				return value;
			case SOFTMAX:
			case LOGIT:
				return value.logit();
			case EXP:
				return value.exp();
			case PROBIT:
				return value.probit();
			case CLOGLOG:
				return value.cloglog();
			case LOGLOG:
				return value.loglog();
			case CAUCHIT:
				return value.cauchit();
			default:
				throw new EvaluationException();
		}
	}

	static
	public <V extends Number> Value<V> normalizeBinaryLogisticClassificationResult(Value<V> value, RegressionModel.NormalizationMethod normalizationMethod){

		switch(normalizationMethod){
			case NONE:
				return value.restrict(0d, 1d);
			case LOGIT:
				return value.logit();
			case PROBIT:
				return value.probit();
			case CLOGLOG:
				return value.cloglog();
			case LOGLOG:
				return value.loglog();
			case CAUCHIT:
				return value.cauchit();
			default:
				throw new EvaluationException();
		}
	}
}