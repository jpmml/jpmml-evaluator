/*
 * Copyright (c) 2016 Villu Ruusmann
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
package org.jpmml.evaluator.association;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.ModelEvaluationContext;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CategoricalValueTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		AssociationModelEvaluator evaluator = (AssociationModelEvaluator)createModelEvaluator();

		Map<FieldName, ?> arguments = createArguments("Water", true, "Cracker", "false", "Bread", "true");

		checkActiveItems(ImmutableSet.of("1", "3"), evaluator, arguments);

		arguments = createArguments("Area", "suburban", "Day", "Friday");

		checkActiveItems(ImmutableSet.of("5", "11"), evaluator, arguments);

		arguments = createArguments("Day=Weekday", true);

		checkActiveItems(ImmutableSet.of("14"), evaluator, arguments);
	}

	static
	private void checkActiveItems(Set<String> items, AssociationModelEvaluator evaluator, Map<FieldName, ?> arguments){
		ModelEvaluationContext context = new ModelEvaluationContext(evaluator);
		context.setArguments(arguments);

		assertEquals(items, evaluator.getActiveItems(context));
	}
}