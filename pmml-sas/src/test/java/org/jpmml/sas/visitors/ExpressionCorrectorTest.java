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

import java.util.List;

import org.dmg.pmml.Apply;
import org.dmg.pmml.Constant;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExpressionCorrectorTest {

	@Test
	public void correct(){
		Apply apply = new Apply("substring")
			.addExpressions(new FieldRef(FieldName.create("AnyCInput")), new Constant("1"), new Constant("FMTWIDTH"));

		ExpressionCorrector expressionCorrector = new ExpressionCorrector();
		expressionCorrector.applyTo(apply);

		List<Expression> expressions = apply.getExpressions();

		Expression string = expressions.get(0);
		Expression position = expressions.get(1);
		Expression length = expressions.get(2);

		assertTrue(string instanceof FieldRef);
		assertEquals(FieldName.create("AnyCInput"), ((FieldRef)string).getField());

		assertTrue(position instanceof Constant);
		assertEquals("1", ((Constant)position).getValue());

		assertTrue(length instanceof FieldRef);
		assertEquals(FieldName.create("FMTWIDTH"), ((FieldRef)length).getField());
	}
}