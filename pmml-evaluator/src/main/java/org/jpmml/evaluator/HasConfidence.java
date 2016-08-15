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
 * A marker interface for classification results that provide a confidence distribution.
 * </p>
 *
 * <p>
 * Confidences are similar to probabilities, but more relaxed.
 * Most notably, the confidences are not required to sum to 1 across all categories, whereas the probabilities are.
 * </p>
 */
public interface HasConfidence extends CategoricalResultFeature {

	/**
	 * <p>
	 * Gets the confidence of the specified category.
	 * </p>
	 *
	 * @return A confidence that normally lies in the range from 0.0 to 1.0.
	 * The confidence of an unknown category is 0.0.
	 *
	 * @see #getCategoryValues()
	 */
	Double getConfidence(String value);
}