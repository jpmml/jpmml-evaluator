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
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayTable;
import com.google.common.collect.Table;
import org.dmg.pmml.Aggregate;
import org.dmg.pmml.Apply;
import org.dmg.pmml.Constant;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Discretize;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldColumnPair;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.InlineTable;
import org.dmg.pmml.InvalidValueTreatmentMethod;
import org.dmg.pmml.MapValues;
import org.dmg.pmml.NormContinuous;
import org.dmg.pmml.NormDiscrete;
import org.dmg.pmml.TextIndex;
import org.dmg.pmml.TextIndex.CountHits;
import org.dmg.pmml.TextIndexNormalization;
import org.jpmml.evaluator.functions.EchoFunction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExpressionUtilTest {

	@Test
	public void evaluateConstant(){
		Constant constant = new Constant("3");

		assertEquals(DataType.INTEGER, getDataType(constant));

		assertEquals(3, evaluate(constant));

		Constant stringThree = new Constant("3")
			.setDataType(DataType.STRING);

		assertEquals(DataType.STRING, getDataType(stringThree));

		assertEquals("3", evaluate(stringThree));

		Constant integerThree = new Constant("3")
			.setDataType(DataType.INTEGER);

		assertEquals(3, evaluate(integerThree));

		Constant floatThree = new Constant("3")
			.setDataType(DataType.FLOAT);

		assertEquals(3f, evaluate(floatThree));
	}

	@Test
	public void evaluateFieldRef(){
		FieldName name = FieldName.create("x");

		FieldRef fieldRef = new FieldRef(name);

		assertEquals("3", evaluate(fieldRef, name, "3"));
		assertEquals(null, evaluate(fieldRef, name, null));

		fieldRef.setMapMissingTo("Missing");

		assertEquals("Missing", evaluate(fieldRef, name, null));
	}

	@Test
	public void evaluateNormContinuous(){
		FieldName name = FieldName.create("x");

		NormContinuous normContinuous = new NormContinuous(name, null)
			.setMapMissingTo(5d);

		assertEquals(DataType.DOUBLE, getDataType(normContinuous));

		assertEquals(5d, evaluate(normContinuous, name, null));
	}

	@Test
	public void evaluateNormDiscrete(){
		FieldName name = FieldName.create("x");

		Double equals = 1d;
		Double notEquals = 0d;

		NormDiscrete stringThree = new NormDiscrete(name, "3");

		assertEquals(DataType.DOUBLE, getDataType(stringThree));

		assertEquals(equals, evaluate(stringThree, name, "3"));
		assertEquals(notEquals, evaluate(stringThree, name, "1"));

		stringThree.setMapMissingTo(5d);

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
		FieldName name = FieldName.create("x");

		Discretize discretize = new Discretize(name);

		assertEquals(DataType.STRING, getDataType(discretize));

		assertEquals(null, evaluate(discretize, name, null));

		discretize.setMapMissingTo("Missing");

		assertEquals("Missing", evaluate(discretize, name, null));
		assertEquals(null, evaluate(discretize, name, 3));

		discretize.setDefaultValue("Default");

		assertEquals("Default", evaluate(discretize, name, 3));
	}

	@Test
	public void evaluateMapValues(){
		FieldName name = FieldName.create("x");

		MapValues mapValues = new MapValues("y")
			.addFieldColumnPairs(new FieldColumnPair(name, null));

		assertEquals(DataType.STRING, getDataType(mapValues));

		assertEquals(null, evaluate(mapValues, name, null));

		mapValues.setMapMissingTo("Missing");

		assertEquals("Missing", evaluate(mapValues, name, null));
		assertEquals(null, evaluate(mapValues, name, "3"));

		mapValues.setDefaultValue("Default");

		assertEquals("Default", evaluate(mapValues, name, "3"));
	}

	@Test
	public void evaluateTextIndex(){
		FieldName name = FieldName.create("x");

		TextIndex textIndex = new TextIndex(name)
			.setWordSeparatorCharacterRE("[\\s\\-]")
			.setExpression(new Constant("user friendly"));

		String text;

		assertEquals(DataType.INTEGER, getDataType(textIndex));

		assertEquals(1, evaluate(textIndex, name, "user friendly"));
		assertEquals(1, evaluate(textIndex, name, "user-friendly"));

		textIndex = new TextIndex(name)
			.setExpression(new Constant("brown fox"));

		text = "The quick browny foxy jumps over the lazy dog. The brown fox runs away and to be with another brown foxy.";

		textIndex.setMaxLevenshteinDistance(0);

		assertEquals(1, evaluate(textIndex, name, text));

		textIndex.setMaxLevenshteinDistance(1);

		assertEquals(2, evaluate(textIndex, name, text));

		textIndex.setMaxLevenshteinDistance(2);

		assertEquals(3, evaluate(textIndex, name, text));

		textIndex = new TextIndex(name)
			.setMaxLevenshteinDistance(1)
			.setExpression(new Constant("dog"));

		text = "I have a doog. My dog is white. The doog is friendly.";

		textIndex.setCountHits(CountHits.ALL_HITS);

		assertEquals(3, evaluate(textIndex, name, text));

		textIndex.setCountHits(CountHits.BEST_HITS);

		assertEquals(1, evaluate(textIndex, name, text));

		textIndex = new TextIndex(name)
			.setCaseSensitive(false)
			.setExpression(new Constant("sun"));

		text = "The Sun was setting while the captain's son reached the bounty island, minutes after their ship had sunk to the bottom of the ocean.";

		textIndex.setMaxLevenshteinDistance(0);

		assertEquals(1, evaluate(textIndex, name, text));

		textIndex.setMaxLevenshteinDistance(1);

		assertEquals(3, evaluate(textIndex, name, text));
	}

	@Test
	public void evaluateTextIndexNormalization(){
		FieldName name = FieldName.create("x");

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

		TextIndex textIndex = new TextIndex(name)
			.setLocalTermWeights(TextIndex.LocalTermWeights.BINARY)
			.setCaseSensitive(false)
			.addTextIndexNormalizations(stepOne, stepTwo)
			.setExpression(new Constant("ui_good"));

		assertEquals(1, evaluate(textIndex, name, "Testing the app for a few days convinced me the interfaces are excellent!"));
	}

	@Test
	public void evaluateApply(){
		FieldName name = FieldName.create("x");

		Apply apply = new Apply("/")
			.addExpressions(new FieldRef(name), new Constant("0"));

		assertEquals(null, evaluate(apply, name, null));

		apply.setDefaultValue("-1");

		assertEquals("-1", evaluate(apply, name, null));

		apply.setMapMissingTo("missing");

		assertEquals("missing", evaluate(apply, name, null));

		apply.setInvalidValueTreatment(InvalidValueTreatmentMethod.RETURN_INVALID);

		try {
			evaluate(apply, name, 1);

			fail();
		} catch(InvalidResultException ire){
			// Ignored
		}

		apply.setInvalidValueTreatment(InvalidValueTreatmentMethod.AS_IS);

		try {
			evaluate(apply, name, 1);

			fail();
		} catch(InvalidResultException ire){
			// Ignored
		}

		apply.setInvalidValueTreatment(InvalidValueTreatmentMethod.AS_MISSING);

		assertEquals("-1", evaluate(apply, name, 1));
	}

	@Test
	public void evaluateApplyCondition(){
		FieldName name = FieldName.create("x");

		Apply condition = new Apply("isNotMissing")
			.addExpressions(new FieldRef(name));

		Apply apply = new Apply("if")
			.addExpressions(condition);

		try {
			evaluate(apply, name, null);

			fail();
		} catch(FunctionException fe){
			// Ignored
		}

		Expression thenPart = new Apply("abs")
			.addExpressions(new FieldRef(name));

		apply.addExpressions(thenPart);

		assertEquals(1, evaluate(apply, name, 1));
		assertEquals(1, evaluate(apply, name, -1));

		assertEquals(null, evaluate(apply, name, null));

		Expression elsePart = new Constant("-1")
			.setDataType(DataType.DOUBLE);

		apply.addExpressions(elsePart);

		assertEquals(-1d, evaluate(apply, name, null));

		apply.addExpressions(new FieldRef(name));

		try {
			evaluate(apply, name, null);

			fail();
		} catch(FunctionException fe){
			// Ignored
		}
	}

	@Test
	public void evaluateApplyJavaFunction(){
		FieldName name = FieldName.create("x");

		FieldRef fieldRef = new FieldRef(name);

		Apply apply = new Apply(EchoFunction.class.getName())
			.addExpressions(fieldRef);

		try {
			evaluate(apply);

			fail();
		} catch(EvaluationException ee){
			assertEquals(fieldRef, ee.getContext());
		}

		assertEquals("Hello World!", evaluate(apply, name, "Hello World!"));
	}

	@Test
	public void evaluateAggregateArithmetic(){
		FieldName name = FieldName.create("x");

		List<Integer> values = Arrays.asList(1, 2, 3);

		Aggregate aggregate = new Aggregate(name, Aggregate.Function.COUNT);

		assertEquals(3, evaluate(aggregate, name, values));

		aggregate.setFunction(Aggregate.Function.SUM);

		assertEquals(6, evaluate(aggregate, name, values));

		aggregate.setFunction(Aggregate.Function.AVERAGE);

		assertEquals(2d, evaluate(aggregate, name, values));
	}

	@Test
	public void evaluateAggregate(){
		FieldName name = FieldName.create("x");

		List<?> values = Arrays.asList(TypeUtil.parse(DataType.DATE, "2013-01-01"), TypeUtil.parse(DataType.DATE, "2013-02-01"), TypeUtil.parse(DataType.DATE, "2013-03-01"));

		Aggregate aggregate = new Aggregate(name, Aggregate.Function.COUNT);

		assertEquals(3, evaluate(aggregate, name, values));

		aggregate.setFunction(Aggregate.Function.MIN);

		assertEquals(values.get(0), evaluate(aggregate, name, values));

		aggregate.setFunction(Aggregate.Function.MAX);

		assertEquals(values.get(2), evaluate(aggregate, name, values));
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
		Map<FieldName, ?> arguments = ModelEvaluatorTest.createArguments(objects);

		return evaluate(expression, arguments);
	}

	static
	private Object evaluate(Expression expression, Map<FieldName, ?> arguments){
		EvaluationContext context = new VirtualEvaluationContext();
		context.declareAll(arguments);

		FieldValue result = ExpressionUtil.evaluate(expression, context);

		return FieldValueUtil.getValue(result);
	}

	static
	private DataType getDataType(Expression expression){
		return ExpressionUtil.getDataType(expression, null);
	}
}