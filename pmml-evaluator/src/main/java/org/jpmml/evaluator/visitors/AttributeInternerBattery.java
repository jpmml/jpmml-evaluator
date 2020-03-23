/*
 * Copyright (c) 2020 Villu Ruusmann
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
package org.jpmml.evaluator.visitors;

import org.jpmml.model.visitors.DoubleInterner;
import org.jpmml.model.visitors.FloatInterner;
import org.jpmml.model.visitors.IntegerInterner;
import org.jpmml.model.visitors.StringInterner;
import org.jpmml.model.visitors.VisitorBattery;

public class AttributeInternerBattery extends VisitorBattery {

	public AttributeInternerBattery(){
		add(StringInterner.class);
		add(IntegerInterner.class);
		add(FloatInterner.class);
		add(DoubleInterner.class);
	}
}