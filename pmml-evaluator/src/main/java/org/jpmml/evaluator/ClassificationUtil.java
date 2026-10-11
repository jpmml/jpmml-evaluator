/*
 * Copyright (c) 2026 Villu Ruusmann
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

import java.util.Set;

import org.dmg.pmml.ResultFeature;

public class ClassificationUtil {

	private ClassificationUtil(){
	}

	static
	public <V extends Number> Classification<Object, V> createClassification(ModelManager<?> modelManager, ValueMap<Object, V> values){
		return createClassification(modelManager.getResultFeatures(), values);
	}

	static
	public <V extends Number> Classification<Object, V> createClassification(Set<ResultFeature> resultFeatures, ValueMap<Object, V> values){

		if(resultFeatures.contains(ResultFeature.PROBABILITY) || resultFeatures.contains(ResultFeature.RESIDUAL)){
			return new ProbabilityDistribution<>(values);
		} else

		if(resultFeatures.contains(ResultFeature.CONFIDENCE)){
			return new ConfidenceDistribution<>(values);
		} else

		{
			return new VoteDistribution<>(values);
		}
	}
}