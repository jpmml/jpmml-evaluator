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
 * A common superinterface for all result features.
 * </p>
 *
 * <p>
 * Every {@link org.dmg.pmml.ResultFeature result feature} is mapped to a specialized subinterface.
 * A target value may implement any number of subinterfaces.
 * Application developers should use the <code>instanceof</code> type comparison operator to check if the target value implements a particular subinterface or not.
 * </p>
 *
 * @see Computable
 */
public interface ResultFeature {
}