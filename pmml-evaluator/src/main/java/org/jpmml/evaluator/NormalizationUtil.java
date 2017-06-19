/*
 * Copyright (c) 2011 University of Tartu
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jpmml.evaluator;

import java.util.List;

import org.dmg.pmml.DataType;
import org.dmg.pmml.LinearNorm;
import org.dmg.pmml.NormContinuous;
import org.dmg.pmml.OpType;
import org.dmg.pmml.OutlierTreatmentMethod;

public class NormalizationUtil {

	private NormalizationUtil(){
	}

	static
	public FieldValue normalize(NormContinuous normContinuous, FieldValue value){
		double result = normalize(normContinuous, (value.asNumber()).doubleValue());

		return FieldValueUtil.create(DataType.DOUBLE, OpType.CONTINUOUS, result);
	}

	static
	public double normalize(NormContinuous normContinuous, double value){
		List<LinearNorm> linearNorms = normContinuous.getLinearNorms();
		if(linearNorms.size() < 2){
			throw new InvalidFeatureException(normContinuous);
		}

		LinearNorm rangeStart = linearNorms.get(0);
		LinearNorm rangeEnd = linearNorms.get(linearNorms.size() - 1);

		// Select proper interval for normalization
		if(value >= rangeStart.getOrig() && value <= rangeEnd.getOrig()){

			for(int i = 1; i < linearNorms.size() - 1; i++){
				LinearNorm linearNorm = linearNorms.get(i);

				if(value >= linearNorm.getOrig()){
					rangeStart = linearNorm;
				} else

				if(value <= linearNorm.getOrig()){
					rangeEnd = linearNorm;

					break;
				}
			}
		} else

		// Deal with outliers
		{
			OutlierTreatmentMethod outlierTreatmentMethod = normContinuous.getOutliers();

			switch(outlierTreatmentMethod){
				case AS_MISSING_VALUES:
					Double missing = normContinuous.getMapMissingTo();
					if(missing == null){
						throw new InvalidFeatureException(normContinuous);
					}
					return missing;
				case AS_IS:
					if(value < rangeStart.getOrig()){
						rangeEnd = linearNorms.get(1);
					} else

					{
						rangeStart = linearNorms.get(linearNorms.size() - 2);
					}
					break;
				case AS_EXTREME_VALUES:
					if(value < rangeStart.getOrig()){
						return rangeStart.getNorm();
					} else

					{
						return rangeEnd.getNorm();
					}
				default:
					throw new UnsupportedFeatureException(normContinuous, outlierTreatmentMethod);
			}
		}

		double origRange = rangeEnd.getOrig() - rangeStart.getOrig();
		double normRange = rangeEnd.getNorm() - rangeStart.getNorm();

		return rangeStart.getNorm() + (value - rangeStart.getOrig()) / origRange * normRange;
	}

	static
	public double denormalize(NormContinuous normContinuous, double value){
		DoubleValue doubleValue = new DoubleValue(value);

		denormalize(normContinuous, doubleValue);

		return doubleValue.doubleValue();
	}

	static
	public <V extends Number> Value<V> denormalize(NormContinuous normContinuous, Value<V> value){
		List<LinearNorm> linearNorms = normContinuous.getLinearNorms();
		if(linearNorms.size() < 2){
			throw new InvalidFeatureException(normContinuous);
		}

		LinearNorm rangeStart = linearNorms.get(0);
		LinearNorm rangeEnd = linearNorms.get(linearNorms.size() - 1);

		for(int i = 1; i < linearNorms.size() - 1; i++){
			LinearNorm linearNorm = linearNorms.get(i);

			if(value.doubleValue() >= linearNorm.getNorm()){
				rangeStart = linearNorm;
			} else

			if(value.doubleValue() <= linearNorm.getNorm()){
				rangeEnd = linearNorm;

				break;
			}
		}

		return value.denormalize(rangeStart.getOrig(), rangeStart.getNorm(), rangeEnd.getOrig(), rangeEnd.getNorm());
	}

}