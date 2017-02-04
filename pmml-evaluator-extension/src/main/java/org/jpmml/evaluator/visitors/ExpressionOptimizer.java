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
package org.jpmml.evaluator.visitors;

import org.dmg.pmml.Constant;
import org.dmg.pmml.Expression;
import org.dmg.pmml.NormDiscrete;
import org.jpmml.evaluator.ExtensionUtil;
import org.jpmml.evaluator.RichConstant;
import org.jpmml.evaluator.RichNormDiscrete;
import org.jpmml.model.visitors.ExpressionFilterer;

public class ExpressionOptimizer extends ExpressionFilterer {

	@Override
	public Expression filter(Expression expression){

		if(expression == null || ExtensionUtil.hasExtensions(expression)){
			return expression;
		}

		return optimize(expression);
	}

	public Expression optimize(Expression expression){

		if(expression instanceof Constant){
			Constant constant = (Constant)expression;

			return new RichConstant(constant);
		} else

		if(expression instanceof NormDiscrete){
			NormDiscrete normDiscrete = (NormDiscrete)expression;

			return new RichNormDiscrete(normDiscrete);
		}

		return expression;
	}
}