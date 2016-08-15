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

/**
 * <p>
 * A marker interface for classification or clustering results that provide an affinity distribution.
 * </p>
 *
 * <p>
 * Affinity represents a degree of attraction between the sample and a particular category.
 * </p>
 *
 * <p>
 * PMML deals with two kinds of affinities:
 * <ul>
 *   <li>Distance between two points in an n-dimensional feature space. Smaller distance values indicate more optimal fit.</li>
 *   <li>Similarity between two feature vectors. Greater similarity values indicate more optimal fit.</li>
 * </ul>
 * </p>
 *
 * @see org.dmg.pmml.ResultFeature#AFFINITY
 */
public interface HasAffinity extends CategoricalResultFeature {

	/**
	 * <p>
	 * Gets the affinity towards the specified category.
	 * </p>
	 *
	 * @return An affinity in the range from 0.0 to positive infinity.
	 * The affinity of an unknown category is the least optimal value in the range of valid values (ie. {@link Double#POSITIVE_INFINITY} for distance measures and 0.0 for similarity measures).
	 *
	 * @see #getCategoryValues()
	 */
	Double getAffinity(String value);
}