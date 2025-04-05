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

import com.google.common.base.Function;
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

		LinearNorm start;
		LinearNorm end;

		int index = binarySearch(linearNorms, LinearNorm::requireOrig, value);
		if(index < 0 || index == (linearNorms.size() - 1)){
			OutlierTreatmentMethod outlierTreatment = normContinuous.getOutlierTreatment();

			switch(outlierTreatment){
				case AS_IS:
					// "Extrapolate from the first interval"
					if(index < 0){
						start = linearNorms.get(0);
						end = linearNorms.get(1);
					} else

					// "Extrapolate from the last interval"
					{
						start = linearNorms.get(linearNorms.size() - 2);
						end = linearNorms.get(linearNorms.size() - 1);
					}
					break;
				case AS_MISSING_VALUES:
					// "Map to a missing value"
					return null;
				case AS_EXTREME_VALUES:
					// "Map to the value of the first interval"
					if(index < 0){
						start = linearNorms.get(0);

						return value.reset(start.requireNorm());
					} else

					// "Map to the value of the last interval"
					{
						end = linearNorms.get(linearNorms.size() - 1);

						return value.reset(end.requireNorm());
					}
				default:
					throw new UnsupportedAttributeException(normContinuous, outlierTreatment);
			}
		} else

		{
			start = linearNorms.get(index);
			end = linearNorms.get(index + 1);
		}

		return value.normalize(start.requireOrig(), start.requireNorm(), end.requireOrig(), end.requireNorm());
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

		LinearNorm start;
		LinearNorm end;

		int index = binarySearch(linearNorms, LinearNorm::requireNorm, value);
		if(index < 0 || index == (linearNorms.size() - 1)){
			throw new NotImplementedException();
		} else

		{
			start = linearNorms.get(index);
			end = linearNorms.get(index + 1);
		}

		return value.denormalize(start.requireOrig(), start.requireNorm(), end.requireOrig(), end.requireNorm());
	}

	static
	private <V extends Number> int binarySearch(List<LinearNorm> linearNorms, Function<LinearNorm, Number> thresholdFunction, Value<V> value){
		int low = 0;
		int high = linearNorms.size() - 1;

		while(low <= high){
			int mid = low + (high - low) / 2;

			LinearNorm linearNorm = linearNorms.get(mid);

			Number threshold = thresholdFunction.apply(linearNorm);

			if(value.compareTo(threshold) >= 0){

				if(mid < (linearNorms.size() - 1)){
					LinearNorm nextLinearNorm = linearNorms.get(mid + 1);

					Number nextThreshold = thresholdFunction.apply(nextLinearNorm);

					// Assume a closed-closed range, rather than a closed-open range.
					// If the value matches some threshold value exactly,
					// then it does not matter which bin (ie. this or the next) is used for interpolation.
					if(value.compareTo(nextThreshold) <= 0){
						return mid;
					}
				} else

				// The last element
				{
					return mid;
				}

				low = (mid + 1);
			} else

			{
				high = (mid - 1);
			}
		}

		return -1;
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