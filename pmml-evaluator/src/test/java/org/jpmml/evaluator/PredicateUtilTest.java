/*
 * Copyright (c) 2009 University of Tartu
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jpmml.evaluator;

import java.util.Map;

import org.dmg.pmml.Array;
import org.dmg.pmml.CompoundPredicate;
import org.dmg.pmml.False;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.SimpleSetPredicate;
import org.dmg.pmml.True;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PredicateUtilTest {

	@Test
	public void evaluateSimplePredicate(){
		FieldName age = FieldName.create("age");

		SimplePredicate simplePredicate = new SimplePredicate(age, SimplePredicate.Operator.IS_MISSING);

		assertEquals(Boolean.FALSE, evaluate(simplePredicate, age, 30));
		assertEquals(Boolean.TRUE, evaluate(simplePredicate, age, null));

		simplePredicate.setOperator(SimplePredicate.Operator.IS_NOT_MISSING);

		assertEquals(Boolean.TRUE, evaluate(simplePredicate, age, 30));
		assertEquals(Boolean.FALSE, evaluate(simplePredicate, age, null));

		simplePredicate.setValue("30");

		simplePredicate.setOperator(SimplePredicate.Operator.EQUAL);

		assertEquals(Boolean.TRUE, evaluate(simplePredicate, age, 30));
		assertEquals(null, evaluate(simplePredicate, age, null));

		simplePredicate.setOperator(SimplePredicate.Operator.NOT_EQUAL);

		assertEquals(Boolean.FALSE, evaluate(simplePredicate, age, 30));
		assertEquals(null, evaluate(simplePredicate, age, null));

		simplePredicate.setOperator(SimplePredicate.Operator.LESS_THAN);

		assertEquals(Boolean.FALSE, evaluate(simplePredicate, age, 30));

		simplePredicate.setOperator(SimplePredicate.Operator.LESS_OR_EQUAL);

		assertEquals(Boolean.TRUE, evaluate(simplePredicate, age, 30));

		simplePredicate.setOperator(SimplePredicate.Operator.GREATER_OR_EQUAL);

		assertEquals(Boolean.TRUE, evaluate(simplePredicate, age, 30));

		simplePredicate.setOperator(SimplePredicate.Operator.GREATER_THAN);

		assertEquals(Boolean.FALSE, evaluate(simplePredicate, age, 30));
	}

	@Test
	public void evaluateBooleanSimplePredicate(){
		FieldName flag = FieldName.create("flag");

		SimplePredicate simplePredicate = new SimplePredicate(flag, SimplePredicate.Operator.EQUAL)
			.setValue("true");

		assertEquals(Boolean.TRUE, evaluate(simplePredicate, flag, true));
		assertEquals(Boolean.FALSE, evaluate(simplePredicate, flag, false));

		simplePredicate.setOperator(SimplePredicate.Operator.NOT_EQUAL);

		assertEquals(Boolean.FALSE, evaluate(simplePredicate, flag, true));
		assertEquals(Boolean.TRUE, evaluate(simplePredicate, flag, false));

		simplePredicate.setValue("0.5");

		simplePredicate.setOperator(SimplePredicate.Operator.LESS_OR_EQUAL);

		assertEquals(Boolean.FALSE, evaluate(simplePredicate, flag, true));
		assertEquals(Boolean.TRUE, evaluate(simplePredicate, flag, false));

		simplePredicate.setOperator(SimplePredicate.Operator.GREATER_THAN);

		assertEquals(Boolean.TRUE, evaluate(simplePredicate, flag, true));
		assertEquals(Boolean.FALSE, evaluate(simplePredicate, flag, false));
	}

	@Test
	public void evaluateSimpleSetPredicate(){
		FieldName fruit = FieldName.create("fruit");

		SimpleSetPredicate simpleSetPredicate = new SimpleSetPredicate(fruit, SimpleSetPredicate.BooleanOperator.IS_IN, new Array(Array.Type.STRING, "apple orange"));

		assertEquals(null, evaluate(simpleSetPredicate, fruit, null));

		assertEquals(Boolean.TRUE, evaluate(simpleSetPredicate, fruit, "apple"));
		assertEquals(Boolean.FALSE, evaluate(simpleSetPredicate, fruit, "pineapple"));

		simpleSetPredicate.setBooleanOperator(SimpleSetPredicate.BooleanOperator.IS_NOT_IN);

		assertEquals(Boolean.FALSE, evaluate(simpleSetPredicate, fruit, "apple"));
		assertEquals(Boolean.TRUE, evaluate(simpleSetPredicate, fruit, "pineapple"));
	}

	@Test
	public void evaluateSurrogateCompoundPredicate(){
		FieldName temperature = FieldName.create("temperature");
		FieldName humidity = FieldName.create("humidity");

		CompoundPredicate temperaturePredicate = new CompoundPredicate(CompoundPredicate.BooleanOperator.AND)
			.addPredicates(
				new SimplePredicate(temperature, SimplePredicate.Operator.LESS_THAN).setValue("90"),
				new SimplePredicate(temperature, SimplePredicate.Operator.GREATER_THAN).setValue("50")
			);

		SimplePredicate humidityPredicate = new SimplePredicate(humidity, SimplePredicate.Operator.GREATER_OR_EQUAL).setValue("80");

		CompoundPredicate compoundPredicate = new CompoundPredicate(CompoundPredicate.BooleanOperator.SURROGATE)
			.addPredicates(temperaturePredicate, humidityPredicate);

		assertEquals(Boolean.TRUE, evaluate(compoundPredicate, temperature, 70, humidity, null));
		assertEquals(Boolean.FALSE, evaluate(compoundPredicate, temperature, 40, humidity, null));
		assertEquals(Boolean.FALSE, evaluate(compoundPredicate,  temperature, 100, humidity, null));

		assertEquals(Boolean.TRUE, evaluate(compoundPredicate, temperature, null, humidity, 90));
		assertEquals(Boolean.FALSE, evaluate(compoundPredicate, temperature, null, humidity, 70));

		assertEquals(null, evaluate(compoundPredicate, temperature, null, humidity, null));
	}

	@Test
	public void evaluateBooleanCompoundPredicate(){
		CompoundPredicate compoundPredicate = new CompoundPredicate()
			.addPredicates(new True(), new False());

		compoundPredicate.setBooleanOperator(CompoundPredicate.BooleanOperator.AND);

		assertEquals(Boolean.FALSE, evaluate(compoundPredicate));

		compoundPredicate.setBooleanOperator(CompoundPredicate.BooleanOperator.OR);

		assertEquals(Boolean.TRUE, evaluate(compoundPredicate));

		compoundPredicate.setBooleanOperator(CompoundPredicate.BooleanOperator.XOR);

		assertEquals(Boolean.TRUE, evaluate(compoundPredicate));
	}

	@Test
	public void evaluateTrue(){
		True truePredicate = new True();

		assertEquals(Boolean.TRUE, evaluate(truePredicate));
	}

	@Test
	public void evaluateFalse(){
		False falsePredicate = new False();

		assertEquals(Boolean.FALSE, evaluate(falsePredicate));
	}

	@Test
	public void binaryAnd(){
		assertEquals(Boolean.TRUE, PredicateUtil.binaryAnd(Boolean.TRUE, Boolean.TRUE));
		assertEquals(Boolean.FALSE, PredicateUtil.binaryAnd(Boolean.TRUE, Boolean.FALSE));
		assertEquals(null, PredicateUtil.binaryAnd(Boolean.TRUE, null));
		assertEquals(Boolean.FALSE, PredicateUtil.binaryAnd(Boolean.FALSE, Boolean.TRUE));
		assertEquals(Boolean.FALSE, PredicateUtil.binaryAnd(Boolean.FALSE, Boolean.FALSE));
		assertEquals(Boolean.FALSE, PredicateUtil.binaryAnd(Boolean.FALSE, null));
		assertEquals(null, PredicateUtil.binaryAnd(null, Boolean.TRUE));
		assertEquals(Boolean.FALSE, PredicateUtil.binaryAnd(null, Boolean.FALSE));
		assertEquals(null, PredicateUtil.binaryAnd(null, null));
	}

	@Test
	public void binaryOr(){
		assertEquals(Boolean.TRUE, PredicateUtil.binaryOr(Boolean.TRUE, Boolean.TRUE));
		assertEquals(Boolean.TRUE, PredicateUtil.binaryOr(Boolean.TRUE, Boolean.FALSE));
		assertEquals(Boolean.TRUE, PredicateUtil.binaryOr(Boolean.TRUE, null));
		assertEquals(Boolean.TRUE, PredicateUtil.binaryOr(Boolean.FALSE, Boolean.TRUE));
		assertEquals(Boolean.FALSE, PredicateUtil.binaryOr(Boolean.FALSE, Boolean.FALSE));
		assertEquals(null, PredicateUtil.binaryOr(Boolean.FALSE, null));
		assertEquals(Boolean.TRUE, PredicateUtil.binaryOr(null, Boolean.TRUE));
		assertEquals(null, PredicateUtil.binaryOr(null, Boolean.FALSE));
		assertEquals(null, PredicateUtil.binaryOr(null, null));
	}

	@Test
	public void binaryXor(){
		assertEquals(Boolean.FALSE, PredicateUtil.binaryXor(Boolean.TRUE, Boolean.TRUE));
		assertEquals(Boolean.TRUE, PredicateUtil.binaryXor(Boolean.TRUE, Boolean.FALSE));
		assertEquals(null, PredicateUtil.binaryXor(Boolean.TRUE, null));
		assertEquals(Boolean.TRUE, PredicateUtil.binaryXor(Boolean.FALSE, Boolean.TRUE));
		assertEquals(Boolean.FALSE, PredicateUtil.binaryXor(Boolean.FALSE, Boolean.FALSE));
		assertEquals(null, PredicateUtil.binaryXor(Boolean.FALSE, null));
		assertEquals(null, PredicateUtil.binaryXor(null, Boolean.TRUE));
		assertEquals(null, PredicateUtil.binaryXor(null, Boolean.FALSE));
		assertEquals(null, PredicateUtil.binaryXor(null, null));
	}

	static
	private Boolean evaluate(Predicate predicate, Object... objects){
		Map<FieldName, ?> arguments = ModelEvaluatorTest.createArguments(objects);

		return evaluate(predicate, arguments);
	}

	static
	private Boolean evaluate(Predicate predicate, Map<FieldName, ?> arguments){
		EvaluationContext context = new VirtualEvaluationContext();
		context.declareAll(arguments);

		return PredicateUtil.evaluate(predicate, context);
	}
}