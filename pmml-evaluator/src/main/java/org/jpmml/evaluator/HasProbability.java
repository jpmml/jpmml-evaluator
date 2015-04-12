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
 * A marker interface for <a href="http://en.wikipedia.org/wiki/Probabilistic_classification">probabilistic classification</a> results.
 * </p>
 *
 * <p>
 * Probability represents a degree of certainty that the sample belongs to a particular category.
 * Probabilities are required to sum to 1 across all categories.
 * Ideally, the probability of the predicted category should approach 1.0, and the probabilities of all other categories should approach 0.0.
 * </p>
 *
 * <p>
 * Classification is a supervised learning task.
 * The set of all categories is defined by the valid values of the {@link Evaluator#getTargetField() target field}.
 *
 * The {@link #getCategoryValues() set of known categories} may be smaller than the set of all categories.
 * It is assumed that the probability of missing categories is 0.0.
 * </p>
 *
 * @see ResultFeatureType#PROBABILITY
 */
public interface HasProbability extends CategoricalResultFeature {

	/**
	 * Gets the probability of the specified category.
	 *
	 * @return A probability in the range from 0.0 to 1.0.
	 * The probability of an unknown category is 0.0.
	 *
	 * @see #getCategoryValues()
	 */
	Double getProbability(String value);
}