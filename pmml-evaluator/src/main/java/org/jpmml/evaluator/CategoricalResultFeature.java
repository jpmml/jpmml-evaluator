/*
 * Copyright (c) 2015 Villu Ruusmann
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

import org.dmg.pmml.clustering.Cluster;
import org.dmg.pmml.clustering.ClusteringModel;
import org.dmg.pmml.nearest_neighbor.NearestNeighborModel;

/**
 * <p>
 * A common superinterface for categorical result features.
 * </p>
 *
 * <p>
 * Classification is a supervised learning task.
 * The set of all categories is defined by the valid values of the {@link Evaluator#getTargetField() target field}.
 * </p>
 *
 * <p>
 * Clustering is an unsupervised learning task.
 * The set of all categories is defined by the identifiers of reference entities.
 * For {@link ClusteringModel clustering models} this includes all {@link Cluster clusters}.
 * For {@link NearestNeighborModel k-nearest neighbor models} this includes <em>k</em> nearest training instances.
 * </p>
 */
public interface CategoricalResultFeature extends ResultFeature {

	/**
	 * <p>
	 * Gets the set of known categories.
	 * </p>
	 *
	 * <p>
	 * The set of known categories either equals the set of all categories, or is a proper subset of it.
	 * </p>
	 */
	Set<String> getCategoryValues();
}