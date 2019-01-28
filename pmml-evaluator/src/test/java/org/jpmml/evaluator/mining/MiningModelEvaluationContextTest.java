/*
 * Copyright (c) 2014 Villu Ruusmann
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
package org.jpmml.evaluator.mining;

import java.util.Map;

import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MiningModelEvaluationContextTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		MiningModelEvaluator evaluator = (MiningModelEvaluator)createModelEvaluator();

		MiningModelEvaluationContext context = new MiningModelEvaluationContext(evaluator);
		context.declare(FieldName.create("Age"), 35);

		Map<FieldName, ?> results = evaluator.evaluate(context);

		Classification<?> targetValue = (Classification<?>)results.get(evaluator.getTargetName());

		assertEquals("under 50", targetValue.getResult());
	}
}