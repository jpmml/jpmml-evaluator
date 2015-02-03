/*
 * Copyright (c) 2013 Villu Ruusmann
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.Aggregate;
import org.dmg.pmml.Apply;
import org.dmg.pmml.Constant;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Discretize;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldColumnPair;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.InvalidValueTreatmentMethodType;
import org.dmg.pmml.MapValues;
import org.dmg.pmml.NormContinuous;
import org.dmg.pmml.NormDiscrete;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExpressionUtilTest {

	@Test
	public void evaluateConstant(){
		Constant constant = new Constant("3")
			.withDataType(DataType.STRING);
		assertEquals("3", evaluate(constant));

		Constant integerThree = new Constant("3")
			.withDataType(DataType.INTEGER);
		assertEquals(3, evaluate(integerThree));

		Constant floatThree = new Constant("3")
			.withDataType(DataType.FLOAT);
		assertEquals(3f, evaluate(floatThree));
	}

	@Test
	public void evaluateFieldRef(){
		FieldName name = new FieldName("x");

		FieldRef fieldRef = new FieldRef(name);
		assertEquals("3", evaluate(fieldRef, name, "3"));

		assertEquals(null, evaluate(fieldRef, name, null));

		fieldRef = fieldRef.withMapMissingTo("Missing");
		assertEquals("Missing", evaluate(fieldRef, name, null));
	}

	@Test
	public void evaluateNormContinuous(){
		FieldName name = new FieldName("x");

		NormContinuous normContinuous = new NormContinuous(name, null)
			.withMapMissingTo(5d);

		assertEquals(5d, evaluate(normContinuous, name, null));
	}

	@Test
	public void evaluateNormDiscrete(){
		FieldName name = new FieldName("x");

		Double equals = 1d;
		Double notEquals = 0d;

		NormDiscrete stringThree = new NormDiscrete(name, "3");
		assertEquals(equals, evaluate(stringThree, name, "3"));
		assertEquals(notEquals, evaluate(stringThree, name, "1"));

		stringThree = stringThree.withMapMissingTo(5d);
		assertEquals(5d, evaluate(stringThree, name, null));

		NormDiscrete integerThree = new NormDiscrete(name, "3");
		assertEquals(equals, evaluate(integerThree, name, 3));
		assertEquals(notEquals, evaluate(integerThree, name, 1));

		NormDiscrete floatThree = new NormDiscrete(name, "3.0");
		assertEquals(equals, evaluate(floatThree, name, 3f));
		assertEquals(notEquals, evaluate(floatThree, name, 1f));
	}

	@Test
	public void evaluateDiscretize(){
		FieldName name = new FieldName("x");

		Discretize discretize = new Discretize(name);

		assertEquals(null, evaluate(discretize));

		discretize = discretize.withMapMissingTo("Missing");
		assertEquals("Missing", evaluate(discretize));

		assertEquals(null, evaluate(discretize, name, 3));

		discretize = discretize.withDefaultValue("Default");
		assertEquals("Default", evaluate(discretize, name, 3));
	}

	@Test
	public void evaluateMapValues(){
		FieldName name = new FieldName("x");

		MapValues mapValues = new MapValues(null)
			.withFieldColumnPairs(new FieldColumnPair(name, null));

		assertEquals(null, evaluate(mapValues));

		mapValues = mapValues.withMapMissingTo("Missing");
		assertEquals("Missing", evaluate(mapValues));

		assertEquals(null, evaluate(mapValues, name, "3"));

		mapValues = mapValues.withDefaultValue("Default");
		assertEquals("Default", evaluate(mapValues, name, "3"));
	}


	@Test
	public void evaluateApply(){
		FieldName name = new FieldName("x");

		Apply apply = new Apply("/")
			.withExpressions(new FieldRef(name), new Constant("0"));

		assertEquals(null, evaluate(apply, name, null));

		apply = apply.withDefaultValue("-1");
		assertEquals("-1", evaluate(apply, name, null));

		apply = apply.withMapMissingTo("missing");
		assertEquals("missing", evaluate(apply, name, null));

		apply = apply.withInvalidValueTreatment(InvalidValueTreatmentMethodType.RETURN_INVALID);

		try {
			evaluate(apply, name, 1);

			fail();
		} catch(InvalidResultException ire){
			// Ignored
		}

		apply = apply.withInvalidValueTreatment(InvalidValueTreatmentMethodType.AS_IS);

		try {
			evaluate(apply, name, 1);

			fail();
		} catch(InvalidResultException ire){
			// Ignored
		}

		apply = apply.withInvalidValueTreatment(InvalidValueTreatmentMethodType.AS_MISSING);
		assertEquals("-1", evaluate(apply, name, 1));
	}

	@Test
	public void evaluateApplyCondition(){
		FieldName name = new FieldName("x");

		Apply condition = new Apply("isNotMissing")
			.withExpressions(new FieldRef(name));

		Apply apply = new Apply("if")
			.withExpressions(condition);

		try {
			evaluate(apply, name, null);

			fail();
		} catch(FunctionException fe){
			// Ignored
		}

		Expression thenPart = new Apply("abs")
			.withExpressions(new FieldRef(name));

		apply = apply.withExpressions(thenPart);

		assertEquals(1, evaluate(apply, name, 1));
		assertEquals(1, evaluate(apply, name, -1));

		assertEquals(null, evaluate(apply, name, null));

		Expression elsePart = new Constant("-1")
			.withDataType(DataType.DOUBLE);

		apply = apply.withExpressions(elsePart);

		assertEquals(-1d, evaluate(apply, name, null));

		apply = apply.withExpressions(new FieldRef(name));

		try {
			evaluate(apply, name, null);

			fail();
		} catch(FunctionException fe){
			// Ignored
		}
	}

	@Test
	public void evaluateAggregate(){
		FieldName name = new FieldName("x");

		List<?> values = Arrays.asList(TypeUtil.parse(DataType.DATE, "2013-01-01"), TypeUtil.parse(DataType.DATE, "2013-02-01"), TypeUtil.parse(DataType.DATE, "2013-03-01"));

		Aggregate aggregate = new Aggregate(name, Aggregate.Function.COUNT);
		assertEquals(3, evaluate(aggregate, name, values));

		aggregate = aggregate.withFunction(Aggregate.Function.MIN);
		assertEquals(values.get(0), evaluate(aggregate, name, values));

		aggregate = aggregate.withFunction(Aggregate.Function.MAX);
		assertEquals(values.get(2), evaluate(aggregate, name, values));
	}

	static
	private Object evaluate(Expression expression){
		Map<FieldName, ?> arguments = Collections.emptyMap();

		return evaluate(expression, arguments);
	}

	static
	private Object evaluate(Expression expression, FieldName field, Object value){
		Map<FieldName, ?> arguments = Collections.singletonMap(field, value);

		return evaluate(expression, arguments);
	}

	static
	private Object evaluate(Expression expression, Map<FieldName, ?> arguments){
		EvaluationContext context = new LocalEvaluationContext();
		context.declareAll(arguments);

		FieldValue result = ExpressionUtil.evaluate(expression, context);

		return FieldValueUtil.getValue(result);
	}
}