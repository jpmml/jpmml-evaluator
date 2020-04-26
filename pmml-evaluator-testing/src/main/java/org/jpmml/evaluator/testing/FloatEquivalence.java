/*
 * Copyright (c) 2017 Villu Ruusmann
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
package org.jpmml.evaluator.testing;

import org.dmg.pmml.DataType;
import org.jpmml.evaluator.Computable;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.TypeUtil;

public class FloatEquivalence extends RealNumberEquivalence {

	public FloatEquivalence(int tolerance){
		super(tolerance);
	}

	@Override
	public boolean doEquivalent(Object expected, Object actual){

		if(actual instanceof Computable){
			actual = EvaluatorUtil.decode(actual);
		} // End if

		if(actual instanceof Number){
			actual = TypeUtil.parseOrCast(DataType.FLOAT, actual);
			expected = TypeUtil.parseOrCast(DataType.FLOAT, expected);
		}

		return super.doEquivalent(expected, actual);
	}
}