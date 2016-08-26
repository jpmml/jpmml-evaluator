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
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MiningModelEvaluationContextTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		MiningModelEvaluator evaluator = (MiningModelEvaluator)createModelEvaluator();

		MiningModelEvaluationContext context = new MiningModelEvaluationContext(evaluator);
		context.declare(FieldName.create("Age"), 35);

		Map<FieldName, ?> result = evaluator.evaluate(context);

		Classification targetValue = (Classification)result.get(evaluator.getTargetFieldName());

		assertEquals("under 50", targetValue.getResult());

		FieldValue value = context.getField(FieldName.create("Age_missing"));

		assertNotNull(value);
	}
}