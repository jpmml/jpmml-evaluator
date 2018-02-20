/*
 * Copyright (c) 2018 Villu Ruusmann
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
package org.jpmml.evaluator.java;

import java.util.Arrays;
import java.util.Map;

import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.FieldValues;
import org.jpmml.evaluator.JavaModel;
import org.jpmml.evaluator.MissingValueException;
import org.jpmml.evaluator.ModelEvaluationContext;
import org.jpmml.evaluator.NotImplementedException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class JavaModelEvaluationContextTest {

	@Test
	public void evaluate(){
		FieldName name = FieldName.create("x");

		MiningSchema miningSchema = new MiningSchema()
			.addMiningFields(new MiningField(name));

		JavaModel javaModel = new JavaModel(MiningFunction.REGRESSION, miningSchema){

			@Override
			public Map<FieldName, ?> evaluate(ModelEvaluationContext context){
				throw new NotImplementedException();
			}
		};

		DataDictionary dataDictionary = new DataDictionary()
			.addDataFields(new DataField(name, OpType.CONTINUOUS, DataType.INTEGER));

		PMML pmml = new PMML()
			.setDataDictionary(dataDictionary)
			.addModels(javaModel);

		JavaModelEvaluator javaModelEvaluator = new JavaModelEvaluator(pmml, javaModel);

		JavaModelEvaluationContext context = new JavaModelEvaluationContext(javaModelEvaluator);

		assertEquals(FieldValues.MISSING_VALUE, context.evaluate(name));

		try {
			context.evaluateRequired(name);

			fail();
		} catch(MissingValueException mve){
			// Ignored
		}

		try {
			context.evaluate(0);

			fail();
		} catch(IllegalStateException ise){
			// Ignored
		}

		context.cache(Arrays.asList(name));

		assertEquals(FieldValues.MISSING_VALUE, context.evaluate(0));

		try {
			context.evaluateRequired(0);

			fail();
		} catch(MissingValueException mve){
			// Ignored
		}

		context.cache(null);

		try {
			context.evaluate(0);

			fail();
		} catch(IllegalStateException ise){
			// Ignored
		}
	}
}