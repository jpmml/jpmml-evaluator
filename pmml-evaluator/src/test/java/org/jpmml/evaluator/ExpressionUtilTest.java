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

import java.util.*;

import org.dmg.pmml.*;

import org.junit.*;

import static org.junit.Assert.*;

public class ExpressionUtilTest {

	@Test
	public void evaluateConstant(){
		Constant constant = new Constant("3");
		constant.setDataType(DataType.STRING);
		assertEquals("3", evaluate(constant, null));

		Constant integerThree = new Constant("3");
		integerThree.setDataType(DataType.INTEGER);
		assertEquals(3, evaluate(integerThree, null));

		Constant floatThree = new Constant("3");
		floatThree.setDataType(DataType.FLOAT);
		assertEquals(3f, evaluate(floatThree, null));
	}

	@Test
	public void evaluateFieldRef(){
		FieldName name = new FieldName("x");

		FieldRef fieldRef = new FieldRef(name);
		assertEquals("3", evaluate(fieldRef, createContext(name, "3")));

		assertEquals(null, evaluate(fieldRef, createContext(name, null)));
		fieldRef.setMapMissingTo("Missing");
		assertEquals("Missing", evaluate(fieldRef, createContext(name, null)));
	}

	@Test
	public void evaluateNormContinuous(){
		FieldName name = new FieldName("x");

		NormContinuous normContinuous = new NormContinuous(name);

		normContinuous.setMapMissingTo(5d);

		assertEquals(5d, evaluate(normContinuous, createContext(name, null)));
	}

	@Test
	public void evaluateNormDiscrete(){
		FieldName name = new FieldName("x");

		Double equals = 1d;
		Double notEquals = 0d;

		NormDiscrete stringThree = new NormDiscrete(name, "3");
		assertEquals(equals, evaluate(stringThree, createContext(name, "3")));
		assertEquals(notEquals, evaluate(stringThree, createContext(name, "1")));

		stringThree.setMapMissingTo(5d);

		assertEquals(5d, evaluate(stringThree, createContext(name, null)));

		NormDiscrete integerThree = new NormDiscrete(name, "3");
		assertEquals(equals, evaluate(integerThree, createContext(name, 3)));
		assertEquals(notEquals, evaluate(integerThree, createContext(name, 1)));

		NormDiscrete floatThree = new NormDiscrete(name, "3.0");
		assertEquals(equals, evaluate(floatThree, createContext(name, 3f)));
		assertEquals(notEquals, evaluate(floatThree, createContext(name, 1f)));
	}

	@Test
	public void evaluateDiscretize(){
		FieldName name = new FieldName("x");

		Discretize discretize = new Discretize(name);

		assertEquals(null, evaluate(discretize, createContext()));
		discretize.setMapMissingTo("Missing");
		assertEquals("Missing", evaluate(discretize, createContext()));

		assertEquals(null, evaluate(discretize, createContext(name, 3)));
		discretize.setDefaultValue("Default");
		assertEquals("Default", evaluate(discretize, createContext(name, 3)));
	}

	@Test
	public void evaluateMapValues(){
		FieldName name = new FieldName("x");

		MapValues mapValues = new MapValues(null);
		mapValues.withFieldColumnPairs(new FieldColumnPair(name, null));

		assertEquals(null, evaluate(mapValues, createContext()));
		mapValues.setMapMissingTo("Missing");
		assertEquals("Missing", evaluate(mapValues, createContext()));

		assertEquals(null, evaluate(mapValues, createContext(name, "3")));
		mapValues.setDefaultValue("Default");
		assertEquals("Default", evaluate(mapValues, createContext(name, "3")));
	}


	@Test
	public void evaluateApply(){
		FieldName name = new FieldName("x");

		Apply apply = new Apply("/");
		apply.withExpressions(new FieldRef(name), new Constant("0"));

		assertEquals(null, evaluate(apply, createContext(name, null)));
		apply.setDefaultValue("1");
		assertEquals("1", evaluate(apply, createContext(name, null)));
		apply.setMapMissingTo("missing");
		assertEquals("missing", evaluate(apply, createContext(name, null)));

		apply.setInvalidValueTreatment(InvalidValueTreatmentMethodType.RETURN_INVALID);

		try {
			evaluate(apply, createContext(name, 1));

			Assert.fail();
		} catch(InvalidResultException ire){
			// Ignored
		}

		apply.setInvalidValueTreatment(InvalidValueTreatmentMethodType.AS_MISSING);

		assertEquals("1", evaluate(apply, createContext(name, 1)));
	}

	@Test
	public void evaluateAggregate(){
		FieldName name = new FieldName("x");

		List<?> values = Arrays.asList(TypeUtil.parse(DataType.DATE, "2013-01-01"), TypeUtil.parse(DataType.DATE, "2013-02-01"), TypeUtil.parse(DataType.DATE, "2013-03-01"));

		EvaluationContext context = createContext(name, values);

		Aggregate aggregate = new Aggregate(name, Aggregate.Function.COUNT);
		assertEquals(3, evaluate(aggregate, context));

		aggregate.setFunction(Aggregate.Function.MIN);
		assertEquals(values.get(0), evaluate(aggregate, context));

		aggregate.setFunction(Aggregate.Function.MAX);
		assertEquals(values.get(2), evaluate(aggregate, context));
	}

	static
	private EvaluationContext createContext(){
		EvaluationContext context = new LocalEvaluationContext();

		return context;
	}

	static
	private EvaluationContext createContext(FieldName name, Object value){
		EvaluationContext context = new LocalEvaluationContext();
		context.declare(name, value);

		return context;
	}

	static
	private Object evaluate(Expression expression, EvaluationContext context){
		FieldValue result = ExpressionUtil.evaluate(expression, context);

		return FieldValueUtil.getValue(result);
	}
}