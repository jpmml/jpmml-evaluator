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
package org.jpmml.sas.visitors;

import org.dmg.pmml.Constant;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.jpmml.model.visitors.ExpressionFilterer;

/**
 * <p>
 * A Visitor that corrects invalid PMML expressions.
 * </p>
 *
 * Summary of corrections:
 * <ul>
 *   <li>Replace all occurrences of <code>&lt;Constant&gt;FMTWIDTH&lt;/Constant&gt;</code> with <code>&lt;FieldRef field=&quot;FMTWIDTH&quot;/&gt;</li>
 * </ul>
 */
public class ExpressionCorrector extends ExpressionFilterer {

	@Override
	public Expression filter(Expression expression){

		if(expression instanceof Constant){
			return filterConstant((Constant)expression);
		}

		return expression;
	}

	private Expression filterConstant(Constant constant){

		if(("FMTWIDTH").equals(constant.getValue())){
			FieldRef fieldRef = new FieldRef(FieldName.create("FMTWIDTH"));

			return fieldRef;
		}

		return constant;
	}
}