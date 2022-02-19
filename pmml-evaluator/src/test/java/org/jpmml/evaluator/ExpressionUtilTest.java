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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import org.dmg.pmml.Aggregate;
import org.dmg.pmml.Apply;
import org.dmg.pmml.Constant;
import org.dmg.pmml.DataType;
import org.dmg.pmml.DefineFunction;
import org.dmg.pmml.Discretize;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldColumnPair;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.InlineTable;
import org.dmg.pmml.InvalidValueTreatmentMethod;
import org.dmg.pmml.MapValues;
import org.dmg.pmml.NamespacePrefixes;
import org.dmg.pmml.NormContinuous;
import org.dmg.pmml.NormDiscrete;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMMLFunctions;
import org.dmg.pmml.ParameterField;
import org.dmg.pmml.TextIndex;
import org.dmg.pmml.TextIndex.CountHits;
import org.dmg.pmml.TextIndexNormalization;
import org.jpmml.evaluator.functions.EchoFunction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ExpressionUtilTest {

	@Test
	public void evaluateConstant(){
		Constant emptyString = new Constant();

		assertEquals("", evaluate(emptyString));

		emptyString.setDataType(DataType.STRING);

		assertEquals("", evaluate(emptyString));

		emptyString.setMissing(true);

		assertEquals(null, evaluate(emptyString));

		Constant stringThree = new Constant("3")
			.setDataType(DataType.STRING);

		assertEquals("3", evaluate(stringThree));

		stringThree.setMissing(true);

		assertEquals(null, evaluate(stringThree));

		Constant emptyInteger = new Constant()
			.setDataType(DataType.INTEGER);

		assertEquals(null, evaluate(emptyInteger));

		Constant integerThree = new Constant("3")
			.setDataType(DataType.INTEGER);

		assertEquals(3, evaluate(integerThree));

		integerThree.setMissing(true);

		assertEquals(null, evaluate(integerThree));

		Constant floatThree = new Constant("3")
			.setDataType(DataType.FLOAT);

		assertEquals(3f, evaluate(floatThree));

		floatThree.setMissing(true);

		assertEquals(null, evaluate(floatThree));

		Constant doubleThree = new Constant("3")
			.setDataType(DataType.DOUBLE);

		assertEquals(3d, evaluate(doubleThree));

		doubleThree.setMissing(true);

		assertEquals(null, evaluate(doubleThree));
	}

	@Test
	public void evaluateConstantNaN(){
		Constant constant = new Constant("NaN");

		assertEquals(Double.NaN, evaluate(constant));

		constant.setDataType(DataType.FLOAT);

		assertEquals(Float.NaN, evaluate(constant));
	}

	@Test
	public void evaluateFieldRef(){
		FieldRef fieldRef = new FieldRef("x");

		assertEquals("3", evaluate(fieldRef, "x", "3"));
		assertEquals(null, evaluate(fieldRef, "x", null));

		fieldRef.setMapMissingTo("Missing");

		assertEquals("Missing", evaluate(fieldRef, "x", null));
	}

	@Test
	public void evaluateNormContinuous(){
		NormContinuous normContinuous = new NormContinuous("x", null)
			.setMapMissingTo(5d);

		assertEquals(5d, evaluate(normContinuous, "x", null));
	}

	@Test
	public void evaluateNormDiscrete(){
		Double equals = 1d;
		Double notEquals = 0d;

		NormDiscrete stringThree = new NormDiscrete("x", "3");

		assertEquals(equals, evaluate(stringThree, "x", "3"));
		assertEquals(notEquals, evaluate(stringThree, "x", "1"));

		stringThree.setMapMissingTo(5d);

		assertEquals(5d, evaluate(stringThree, "x", null));

		NormDiscrete integerThree = new NormDiscrete("x", "3");

		assertEquals(equals, evaluate(integerThree, "x", 3));
		assertEquals(notEquals, evaluate(integerThree, "x", 1));

		NormDiscrete floatThree = new NormDiscrete("x", "3.0");

		assertEquals(equals, evaluate(floatThree, "x", 3f));
		assertEquals(notEquals, evaluate(floatThree, "x", 1f));
	}

	@Test
	public void evaluateDiscretize(){
		Discretize discretize = new Discretize("x");

		assertEquals(null, evaluate(discretize, "x", null));

		discretize.setMapMissingTo("Missing");

		assertEquals("Missing", evaluate(discretize, "x", null));
		assertEquals(null, evaluate(discretize, "x", 3));

		discretize.setDefaultValue("Default");

		assertEquals("Default", evaluate(discretize, "x", 3));
	}

	@Test
	public void evaluateMapValues(){
		List<List<String>> rows = Arrays.asList(
			Arrays.asList("0", "zero"),
			Arrays.asList("1", "one")
		);

		MapValues mapValues = new MapValues(NamespacePrefixes.JPMML_INLINETABLE + ":output", createInlineTable(rows, Arrays.asList(NamespacePrefixes.JPMML_INLINETABLE + ":input", NamespacePrefixes.JPMML_INLINETABLE + ":output")))
			.addFieldColumnPairs(new FieldColumnPair("x", NamespacePrefixes.JPMML_INLINETABLE + ":input"));

		assertEquals("zero", evaluate(mapValues, "x", "0"));
		assertEquals("one", evaluate(mapValues, "x", "1"));
		assertEquals(null, evaluate(mapValues, "x", "3"));

		assertEquals(null, evaluate(mapValues, "x", null));

		mapValues.setMapMissingTo("Missing");

		assertEquals("Missing", evaluate(mapValues, "x", null));

		mapValues.setDefaultValue("Default");

		assertEquals("Default", evaluate(mapValues, "x", "3"));
	}

	@Test
	public void evaluateTextIndex(){
		TextIndex textIndex = new TextIndex("x", new Constant("user friendly"))
			.setWordSeparatorCharacterRE("[\\s\\-]");

		assertEquals(null, evaluate(textIndex, "x", null));

		assertEquals(1, evaluate(textIndex, "x", "user friendly"));
		assertEquals(1, evaluate(textIndex, "x", "user-friendly"));

		textIndex = new TextIndex("x", new Constant("brown fox"));

		String text = "The quick browny foxy jumps over the lazy dog. The brown fox runs away and to be with another brown foxy.";

		textIndex.setMaxLevenshteinDistance(0);

		assertEquals(1, evaluate(textIndex, "x", text));

		textIndex.setMaxLevenshteinDistance(1);

		assertEquals(2, evaluate(textIndex, "x", text));

		textIndex.setMaxLevenshteinDistance(2);

		assertEquals(3, evaluate(textIndex, "x", text));

		textIndex = new TextIndex("x", new Constant("dog"))
			.setMaxLevenshteinDistance(1);

		text = "I have a doog. My dog is white. The doog is friendly.";

		textIndex.setCountHits(CountHits.ALL_HITS);

		assertEquals(3, evaluate(textIndex, "x", text));

		textIndex.setCountHits(CountHits.BEST_HITS);

		assertEquals(1, evaluate(textIndex, "x", text));

		textIndex = new TextIndex("x", new Constant("sun"))
			.setCaseSensitive(false);

		text = "The Sun was setting while the captain's son reached the bounty island, minutes after their ship had sunk to the bottom of the ocean.";

		textIndex.setMaxLevenshteinDistance(0);

		assertEquals(1, evaluate(textIndex, "x", text));

		textIndex.setMaxLevenshteinDistance(1);

		assertEquals(3, evaluate(textIndex, "x", text));
	}

	@Test
	public void evaluateTextIndexNormalization(){
		TextIndexNormalization stepOne = new TextIndexNormalization();

		List<List<String>> cells = Arrays.asList(
			Arrays.asList("interfaces?", "interface", "true"),
			Arrays.asList("is|are|seem(ed|s?)|were", "be", "true"),
			Arrays.asList("user friendl(y|iness)", "user_friendly", "true")
		);

		stepOne.setInlineTable(createInlineTable(cells, stepOne));

		TextIndexNormalization stepTwo = new TextIndexNormalization()
			.setInField("re")
			.setOutField("feature");

		cells = Arrays.asList(
			Arrays.asList("interface be (user_friendly|well designed|excellent)", "ui_good", "true")
		);

		stepTwo.setInlineTable(createInlineTable(cells, stepTwo));

		TextIndex textIndex = new TextIndex("x", new Constant("ui_good"))
			.setLocalTermWeights(TextIndex.LocalTermWeights.BINARY)
			.setCaseSensitive(false)
			.addTextIndexNormalizations(stepOne, stepTwo);

		assertEquals(1, evaluate(textIndex, "x", "Testing the app for a few days convinced me the interfaces are excellent!"));
	}

	@Test
	public void evaluateApply(){
		Apply apply = new Apply(PMMLFunctions.DIVIDE)
			.addExpressions(new FieldRef("x"), new Constant("0"));

		assertEquals(null, evaluate(apply, "x", null));

		apply.setDefaultValue("-1");

		assertEquals("-1", evaluate(apply, "x", null));

		apply.setMapMissingTo("missing");

		assertEquals("missing", evaluate(apply, "x", null));

		apply.setInvalidValueTreatment(InvalidValueTreatmentMethod.RETURN_INVALID);

		try {
			evaluate(apply, "x", 1);

			fail();
		} catch(EvaluationException ee){
			Throwable cause = ee.getCause();

			assertTrue(cause instanceof UndefinedResultException);
		}

		apply.setInvalidValueTreatment(InvalidValueTreatmentMethod.AS_IS);

		try {
			evaluate(apply, "x", 1);

			fail();
		} catch(UndefinedResultException ure){
			// Ignored
		}

		apply.setInvalidValueTreatment(InvalidValueTreatmentMethod.AS_MISSING);

		assertEquals("-1", evaluate(apply, "x", 1));
	}

	@Test
	public void evaluateApplyCondition(){
		Apply condition = new Apply(PMMLFunctions.ISNOTMISSING)
			.addExpressions(new FieldRef("x"));

		Apply apply = new Apply(PMMLFunctions.IF)
			.addExpressions(condition);

		try {
			evaluate(apply, "x", null);

			fail();
		} catch(FunctionException fe){
			// Ignored
		}

		Expression thenPart = new Apply(PMMLFunctions.ABS)
			.addExpressions(new FieldRef("x"));

		apply.addExpressions(thenPart);

		assertEquals(1, evaluate(apply, "x", 1));
		assertEquals(1, evaluate(apply, "x", -1));

		assertEquals(null, evaluate(apply, "x", null));

		Expression elsePart = new Constant("-1")
			.setDataType(DataType.DOUBLE);

		apply.addExpressions(elsePart);

		assertEquals(-1d, evaluate(apply, "x", null));

		apply.addExpressions(new FieldRef("x"));

		try {
			evaluate(apply, "x", null);

			fail();
		} catch(FunctionException fe){
			// Ignored
		}
	}

	@Test
	public void evaluateApplyUserDefinedFunction(){
		ParameterField parameterField = new ParameterField("input");

		Expression expression = new Apply(PMMLFunctions.IF)
			.addExpressions(new Apply(PMMLFunctions.ISMISSING)
				.addExpressions(new FieldRef(parameterField)))
			.addExpressions(new Constant("missing"), new Constant("not missing"));

		DefineFunction defineFunction = new DefineFunction("format", OpType.CATEGORICAL, DataType.STRING, null, expression)
			.addParameterFields(parameterField);

		FieldRef fieldRef = new FieldRef("x");

		Apply apply = new Apply(defineFunction.requireName())
			.addExpressions(fieldRef);

		EvaluationContext context = new VirtualEvaluationContext(){

			@Override
			public DefineFunction getDefineFunction(String name){

				if((name).equals(defineFunction.requireName())){
					return defineFunction;
				}

				return super.getDefineFunction(name);
			}
		};

		context.declare("x", FieldValues.MISSING_VALUE);

		assertEquals("missing", evaluate(apply, context));

		context.reset(true);

		context.declare("x", FieldValues.CATEGORICAL_BOOLEAN_TRUE);

		assertEquals("not missing", evaluate(apply, context));

		try {
			context.declare("x", FieldValues.CATEGORICAL_BOOLEAN_FALSE);

			fail();
		} catch(DuplicateFieldValueException dfve){
			// Ignored
		}
	}

	@Test
	public void evaluateApplyJavaFunction(){
		FieldRef fieldRef = new FieldRef("x");

		Apply apply = new Apply(EchoFunction.class.getName())
			.addExpressions(fieldRef);

		try {
			evaluate(apply);

			fail();
		} catch(EvaluationException ee){
			assertEquals(fieldRef, ee.getContext());
		}

		assertEquals("Hello World!", evaluate(apply, "x", "Hello World!"));
	}

	@Test
	public void evaluateToMissingValue(){
		Constant falseConstant = new Constant(false)
			.setDataType(DataType.BOOLEAN);

		Apply apply = new Apply(PMMLFunctions.IF)
			.addExpressions(falseConstant, falseConstant);

		assertEquals(null, evaluate(apply));

		Constant integerOne = new Constant("1");
		Constant integerZero = new Constant("0");

		apply = new Apply(PMMLFunctions.DIVIDE)
			.setInvalidValueTreatment(InvalidValueTreatmentMethod.AS_MISSING)
			.addExpressions(integerOne, integerZero);

		assertEquals(null, evaluate(apply));

		integerOne
			.setValue(1)
			.setDataType(DataType.INTEGER);

		integerZero
			.setValue(0)
			.setDataType(DataType.INTEGER);

		assertEquals(null, evaluate(apply));
	}

	@Test
	public void evaluateAggregateArithmetic(){
		List<Integer> values = Arrays.asList(1, 2, 3);

		Aggregate aggregate = new Aggregate("x", Aggregate.Function.COUNT);

		assertEquals(3, evaluate(aggregate, "x", values));

		aggregate.setFunction(Aggregate.Function.SUM);

		assertEquals(6, evaluate(aggregate, "x", values));

		aggregate.setFunction(Aggregate.Function.AVERAGE);

		assertEquals(2d, evaluate(aggregate, "x", values));
	}

	@Test
	public void evaluateAggregate(){
		TypeInfo typeInfo = new SimpleTypeInfo(OpType.ORDINAL, DataType.DATE);

		List<?> values = Arrays.asList(TypeUtil.parse(DataType.DATE, "2013-01-01"), TypeUtil.parse(DataType.DATE, "2013-02-01"), TypeUtil.parse(DataType.DATE, "2013-03-01"));

		Map<String, FieldValue> arguments = Collections.singletonMap("x", FieldValue.create(typeInfo, values));

		Aggregate aggregate = new Aggregate("x", Aggregate.Function.COUNT);

		assertEquals(3, evaluate(aggregate, arguments));

		aggregate.setFunction(Aggregate.Function.MIN);

		assertEquals(values.get(0), evaluate(aggregate, arguments));

		aggregate.setFunction(Aggregate.Function.MAX);

		assertEquals(values.get(2), evaluate(aggregate, arguments));

		typeInfo = new SimpleTypeInfo(OpType.ORDINAL, DataType.DATE, Lists.reverse(values));

		arguments = Collections.singletonMap("x", FieldValue.create(typeInfo, values));

		aggregate.setFunction(Aggregate.Function.MIN);

		assertEquals(values.get(2), evaluate(aggregate, arguments));

		aggregate.setFunction(Aggregate.Function.MAX);

		assertEquals(values.get(0), evaluate(aggregate, arguments));
	}

	static
	InlineTable createInlineTable(List<List<String>> rows, TextIndexNormalization textIndexNormalization){
		return createInlineTable(rows, Arrays.asList(textIndexNormalization.getInField(), textIndexNormalization.getOutField(), textIndexNormalization.getRegexField()));
	}

	static
	InlineTable createInlineTable(List<List<String>> rows, List<String> columns){
		List<Integer> rowKeys = new ArrayList<>();

		for(int i = 0; i < rows.size(); i++){
			rowKeys.add(i + 1);
		}

		Table<Integer, String, String> table = ArrayTable.create(rowKeys, columns);

		for(int i = 0; i < rows.size(); i++){
			List<String> row = rows.get(i);

			for(int j = 0; j < columns.size(); j++){
				String column = columns.get(j);
				String value = row.get(j);

				if(value == null){
					continue;
				}

				table.put(rowKeys.get(i), column, value);
			}
		}

		return InlineTableUtil.format(table);
	}

	static
	private Object evaluate(Expression expression, Object... objects){
		Map<String, ?> arguments = ModelEvaluatorTest.createArguments(objects);

		return evaluate(expression, arguments);
	}

	static
	private Object evaluate(Expression expression, Map<String, ?> arguments){
		EvaluationContext context = new VirtualEvaluationContext();
		context.declareAll(arguments);

		return evaluate(expression, context);
	}

	static
	private Object evaluate(Expression expression, EvaluationContext context){
		FieldValue result = ExpressionUtil.evaluate(expression, context);

		return FieldValueUtil.getValue(result);
	}
}