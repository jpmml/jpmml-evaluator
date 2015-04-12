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

import org.dmg.pmml.ClusteringModel;
import org.dmg.pmml.NearestNeighborModel;
import org.dmg.pmml.ResultFeatureType;

/**
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
 * <p>
 * Clustering is an unsupervised learning task.
 * The set of all categories is defined by the identifiers of reference entities.
 *
 * For {@link ClusteringModel clustering models} this includes all {@link Cluster clusters}.
 * For {@link NearestNeighborModel k-nearest neighbor models} this includes <code>k</code> most optimal training instances.
 * </p>
 *
 * @see ResultFeatureType#AFFINITY
 */
public interface HasAffinity extends CategoricalResultFeature {

	/**
	 * Gets the affinity towards the specified category.
	 *
	 * @return An affinity in the range from 0.0 to positive infinity.
	 * The affinity of an unknown category is 0.0.
	 *
	 * @see #getCategoryValues()
	 */
	Double getAffinity(String value);
}