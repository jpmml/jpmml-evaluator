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

import org.jpmml.evaluator.visitors.ElementInternerBattery;
import org.jpmml.evaluator.visitors.ElementOptimizerBattery;
import org.jpmml.model.VisitorBattery;
import org.jpmml.model.visitors.AttributeInternerBattery;
import org.jpmml.model.visitors.AttributeOptimizerBattery;
import org.jpmml.model.visitors.ListFinalizerBattery;

/**
 * <p>
 * A top-level Visitor battery that combines all known mid- and low-level Visitor batteries.
 * </p>
 *
 * @see LoadingModelEvaluatorBuilder#setVisitors(VisitorBattery)
 */
public class DefaultVisitorBattery extends VisitorBattery {

	public DefaultVisitorBattery(){
		// Convert PMML attributes and elements from their original representation to optimized representation.
		// The optimization pass should improve performance
		addAll(new AttributeOptimizerBattery());
		addAll(new ElementOptimizerBattery());

		// Identify unique PMML attributes and elements.
		// Keep the first occurrence as a "master copy", and replace all subsequent occurrences with a reference to it.
		// The interning pass should reduce memory consumption
		addAll(new AttributeInternerBattery());
		addAll(new ElementInternerBattery());

		// Replace mutable data structures with immutaable ones
		addAll(new ListFinalizerBattery());
	}
}