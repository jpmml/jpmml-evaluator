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

import org.dmg.pmml.ResultFeatureType;

/**
 * <p>
 * If the model does not declare an Output element, or if the declared Output element is incomplete,
 * then it is possible to enhance the evaluation result with proper output fields programmatically.
 * </p>
 *
 * <p>
 * Every {@link ResultFeatureType result feature} is mapped to a specialized subinterface.
 * A target value may implement any number of subinterfaces.
 * Application developer should use the <code>instanceof</code> type comparison operator to check if the target value implements a particular subinterface or not.
 * </p>
 *
 * <p>
 * For example, handling the target value of a binary probabilistic classification:
 * <pre>
 * Object targetValue = result.get(evaluator.getTargetField());
 *
 * if(targetValue instanceof HasProbabilities){
 *   HasProbabilities hasProbabilities = (HasProbabilities)targetValue;
 *
 *   Double probabilityYes = hasProbabilities.getProbability("yes");
 *   Double probabilityNo = hasProbabilities.getProbability("no");
 * }
 * </pre>
 * </p>
 *
 * @see Computable
 */
public interface ResultFeature {
}