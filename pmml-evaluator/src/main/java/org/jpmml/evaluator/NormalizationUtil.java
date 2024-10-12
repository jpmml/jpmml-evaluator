/*
 * Copyright (c) 2019 Villu Ruusmann
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

import java.util.List;

import org.dmg.pmml.LinearNorm;
import org.dmg.pmml.NormContinuous;
import org.dmg.pmml.OutlierTreatmentMethod;
import org.jpmml.model.InvalidElementListException;
import org.jpmml.model.UnsupportedAttributeException;

public class NormalizationUtil {

	private NormalizationUtil(){
	}

	static
	public FieldValue normalize(NormContinuous normContinuous, FieldValue value){
		Number result = normalize(normContinuous, value.asNumber());

		return FieldValueUtil.create(TypeInfos.CONTINUOUS_DOUBLE, result);
	}

	static
	public Number normalize(NormContinuous normContinuous, Number value){
		Value<Double> doubleValue = new DoubleValue(value);

		doubleValue = normalize(normContinuous, doubleValue);
		if(doubleValue == null){
			return null;
		}

		return doubleValue.getValue();
	}

	static
	public <V extends Number> Value<V> normalize(NormContinuous normContinuous, Value<V> value){
		List<LinearNorm> linearNorms = ensureLinearNorms(normContinuous);

		LinearNorm start = linearNorms.get(0);
		LinearNorm end = linearNorms.get(linearNorms.size() - 1);

		Number startOrig = start.requireOrig();
		Number endOrig = end.requireOrig();

		if(value.compareTo(startOrig) < 0 || value.compareTo(endOrig) > 0){
			OutlierTreatmentMethod outlierTreatmentMethod = normContinuous.getOutliers();

			switch(outlierTreatmentMethod){
				case AS_IS:
					// "Extrapolate from the first interval"
					if(value.compareTo(startOrig) < 0){
						end = linearNorms.get(1);

						endOrig = end.requireOrig();
					} else

					// "Extrapolate from the last interval"
					{
						start = linearNorms.get(linearNorms.size() - 2);

						startOrig = start.requireOrig();
					}
					break;
				case AS_MISSING_VALUES:
					// "Map to a missing value"
					return null;
				case AS_EXTREME_VALUES:
					// "Map to the value of the first interval"
					if(value.compareTo(startOrig) < 0){
						Number startNorm = start.requireNorm();

						return value.reset(startNorm);
					} else

					// "Map to the value of the last interval"
					{
						Number endNorm = end.requireNorm();

						return value.reset(endNorm);
					}
				default:
					throw new UnsupportedAttributeException(normContinuous, outlierTreatmentMethod);
			}
		} else

		{
			for(int i = 1, max = (linearNorms.size() - 1); i < max; i++){
				LinearNorm linearNorm = linearNorms.get(i);

				Number orig = linearNorm.requireOrig();

				if(value.compareTo(orig) >= 0){
					start = linearNorm;

					startOrig = orig;
				} else

				if(value.compareTo(orig) <= 0){
					end = linearNorm;

					endOrig = orig;

					break;
				}
			}
		}

		Number startNorm = start.requireNorm();
		Number endNorm = end.requireNorm();

		return value.normalize(startOrig, startNorm, endOrig, endNorm);
	}

	static
	public Number denormalize(NormContinuous normContinuous, Number value){
		Value<Double> doubleValue = new DoubleValue(value);

		doubleValue = denormalize(normContinuous, doubleValue);

		return doubleValue.getValue();
	}

	static
	public <V extends Number> Value<V> denormalize(NormContinuous normContinuous, Value<V> value){
		List<LinearNorm> linearNorms = ensureLinearNorms(normContinuous);

		LinearNorm start = linearNorms.get(0);
		LinearNorm end = linearNorms.get(linearNorms.size() - 1);

		Number startNorm = start.requireNorm();
		Number endNorm = end.requireNorm();

		for(int i = 1, max = (linearNorms.size() - 1); i < max; i++){
			LinearNorm linearNorm = linearNorms.get(i);

			Number norm = linearNorm.requireNorm();

			if(value.compareTo(norm) >= 0){
				start = linearNorm;

				startNorm = norm;
			} else

			if(value.compareTo(norm) <= 0){
				end = linearNorm;

				endNorm = norm;

				break;
			}
		}

		Number startOrig = start.requireOrig();
		Number endOrig = end.requireOrig();

		return value.denormalize(startOrig, startNorm, endOrig, endNorm);
	}

	static
	private List<LinearNorm> ensureLinearNorms(NormContinuous normContinuous){
		List<LinearNorm> linearNorms = normContinuous.requireLinearNorms();

		if(linearNorms.size() < 2){
			throw new InvalidElementListException(linearNorms);
		}

		return linearNorms;
	}
}