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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.Aggregate;
import org.dmg.pmml.Apply;
import org.dmg.pmml.Constant;
import org.dmg.pmml.DataType;
import org.dmg.pmml.DefineFunction;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Discretize;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldColumnPair;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.HasExpression;
import org.dmg.pmml.HasType;
import org.dmg.pmml.InvalidValueTreatmentMethod;
import org.dmg.pmml.MapValues;
import org.dmg.pmml.NormContinuous;
import org.dmg.pmml.NormDiscrete;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMMLFunctions;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.ParameterField;
import org.dmg.pmml.TextIndex;
import org.jpmml.model.InvalidAttributeException;
import org.jpmml.model.PMMLException;
import org.jpmml.model.UnsupportedAttributeException;
import org.jpmml.model.UnsupportedElementException;

public class ExpressionUtil {

	private ExpressionUtil(){
	}

	static
	public <E extends PMMLObject & HasExpression<E>> FieldValue evaluateExpressionContainer(E hasExpression, EvaluationContext context){
		return evaluate(hasExpression.requireExpression(), context);
	}

	static
	public <E extends PMMLObject & HasType<E> & HasExpression<E>> FieldValue evaluateTypedExpressionContainer(E hasTypedExpression, EvaluationContext context){
		FieldValue value = evaluateExpressionContainer(hasTypedExpression, context);

		if(FieldValueUtil.isMissing(value)){
			return FieldValues.MISSING_VALUE;
		}

		return value.cast(hasTypedExpression);
	}

	static
	public FieldValue evaluate(DerivedField derivedField, EvaluationContext context){
		String name = derivedField.requireName();

		SymbolTable<String> symbolTable = EvaluationContext.DERIVEDFIELD_GUARD_PROVIDER.get();

		if(symbolTable != null){
			symbolTable.lock(name);
		}

		try {
			return evaluateTypedExpressionContainer(derivedField, context);
		} finally {

			if(symbolTable != null){
				symbolTable.release(name);
			}
		}
	}

	static
	public FieldValue evaluate(DefineFunction defineFunction, List<FieldValue> values, EvaluationContext context){
		List<ParameterField> parameterFields = defineFunction.getParameterFields();

		if(parameterFields.size() != values.size()){
			throw new EvaluationException("Function " + EvaluationException.formatName(defineFunction.getName()) + " expects " + parameterFields.size() + " arguments, got " + values.size() + " arguments");
		}

		DefineFunctionEvaluationContext functionContext = new DefineFunctionEvaluationContext(defineFunction, context);

		for(int i = 0, max = parameterFields.size(); i < max; i++){
			ParameterField parameterField = parameterFields.get(i);
			FieldValue value = values.get(i);

			if(FieldValueUtil.isMissing(value)){
				value = FieldValues.MISSING_VALUE;
			} else

			{
				value = value.cast(parameterField);
			}

			String name = parameterField.requireName();

			functionContext.declare(name, value);
		}

		return ExpressionUtil.evaluateTypedExpressionContainer(defineFunction, functionContext);
	}

	static
	public FieldValue evaluate(Expression expression, EvaluationContext context){

		try {
			return evaluateExpression(expression, context);
		} catch(PMMLException pe){
			throw pe.ensureContext(expression);
		}
	}

	static
	FieldValue evaluateExpression(Expression expression, EvaluationContext context){

		if(expression instanceof Constant){
			return evaluateConstant((Constant)expression);
		} else

		if(expression instanceof FieldRef){
			return evaluateFieldRef((FieldRef)expression, context);
		} else

		if(expression instanceof NormContinuous){
			return evaluateNormContinuous((NormContinuous)expression, context);
		} else

		if(expression instanceof NormDiscrete){
			return evaluateNormDiscrete((NormDiscrete)expression, context);
		} else

		if(expression instanceof Discretize){
			return evaluateDiscretize((Discretize)expression, context);
		} else

		if(expression instanceof MapValues){
			return evaluateMapValues((MapValues)expression, context);
		} else

		if(expression instanceof TextIndex){
			return evaluateTextIndex((TextIndex)expression, context);
		} else

		if(expression instanceof Apply){
			return evaluateApply((Apply)expression, context);
		} else

		if(expression instanceof Aggregate){
			return evaluateAggregate((Aggregate)expression, context);
		} // End if

		if(expression instanceof JavaExpression){
			return evaluateJavaExpression((JavaExpression)expression, context);
		}

		throw new UnsupportedElementException(expression);
	}

	static
	public FieldValue evaluateConstant(Constant constant){
		boolean missing = constant.isMissing();
		if(missing){
			return FieldValues.MISSING_VALUE;
		}

		Object value = constant.getValue();

		DataType dataType = constant.getDataType();

		// The dataType attribute is set
		if(dataType != null){

			if(isEmptyContent(value)){

				switch(dataType){
					// "If the data type is string, then the empty content will be interpreted as an empty string"
					case STRING:
						return FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, "");
					// "If the data type is something other than string, then the empty content will be interpreted as a missing value of the specified data type"
					default:
						return FieldValues.MISSING_VALUE;
				}
			}
		} else

		// The dataType attribute is not set
		{
			// "If the content is empty, then the constant will be interpreted as an empty string"
			if(isEmptyContent(value)){
				return FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, "");
			}

			dataType = TypeUtil.getConstantDataType(value);

		}

		OpType opType = TypeUtil.getOpType(dataType);

		return FieldValueUtil.create(opType, dataType, value);
	}

	static
	public FieldValue evaluateFieldRef(FieldRef fieldRef, EvaluationContext context){
		FieldValue value = context.evaluate(fieldRef.requireField());

		if(FieldValueUtil.isMissing(value)){
			return FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, fieldRef.getMapMissingTo());
		}

		return value;
	}

	static
	public FieldValue evaluateNormContinuous(NormContinuous normContinuous, EvaluationContext context){
		FieldValue value = context.evaluate(normContinuous.requireField());

		if(FieldValueUtil.isMissing(value)){
			return FieldValueUtil.create(TypeInfos.CONTINUOUS_DOUBLE, normContinuous.getMapMissingTo());
		}

		return NormalizationUtil.normalize(normContinuous, value);
	}

	static
	public FieldValue evaluateNormDiscrete(NormDiscrete normDiscrete, EvaluationContext context){
		FieldValue value = context.evaluate(normDiscrete.requireField());

		if(FieldValueUtil.isMissing(value)){
			return FieldValueUtil.create(TypeInfos.CATEGORICAL_DOUBLE, normDiscrete.getMapMissingTo());
		}

		NormDiscrete.Method method = normDiscrete.getMethod();
		switch(method){
			case INDICATOR:
				{
					boolean equals = value.equals(normDiscrete);

					return (equals ? FieldValues.CATEGORICAL_DOUBLE_ONE : FieldValues.CATEGORICAL_DOUBLE_ZERO);
				}
			default:
				throw new UnsupportedAttributeException(normDiscrete, method);
		}
	}

	static
	public FieldValue evaluateDiscretize(Discretize discretize, EvaluationContext context){
		FieldValue value = context.evaluate(discretize.requireField());

		if(FieldValueUtil.isMissing(value)){
			return FieldValueUtil.create(OpType.CATEGORICAL, discretize.getDataType(DataType.STRING), discretize.getMapMissingTo());
		}

		return DiscretizationUtil.discretize(discretize, value);
	}

	static
	public FieldValue evaluateMapValues(MapValues mapValues, EvaluationContext context){
		Map<String, FieldValue> values = new LinkedHashMap<>();

		List<FieldColumnPair> fieldColumnPairs = mapValues.getFieldColumnPairs();
		for(int i = 0, max = fieldColumnPairs.size(); i < max; i++){
			FieldColumnPair fieldColumnPair = fieldColumnPairs.get(i);

			FieldValue value = context.evaluate(fieldColumnPair.requireField());

			if(FieldValueUtil.isMissing(value)){
				return FieldValueUtil.create(OpType.CATEGORICAL, mapValues.getDataType(DataType.STRING), mapValues.getMapMissingTo());
			}

			values.put(fieldColumnPair.requireColumn(), value);
		}

		return DiscretizationUtil.mapValue(mapValues, values);
	}

	static
	public FieldValue evaluateTextIndex(TextIndex textIndex, EvaluationContext context){
		FieldValue textValue = context.evaluate(textIndex.requireTextField());

		FieldValue termValue = ExpressionUtil.evaluateExpressionContainer(textIndex, context);

		// See http://mantis.dmg.org/view.php?id=171
		if(FieldValueUtil.isMissing(textValue) || FieldValueUtil.isMissing(termValue)){
			return FieldValues.MISSING_VALUE;
		}

		TextUtil.TextProcessor textProcessor = new TextUtil.TextProcessor(textIndex, textValue.asString());

		TokenizedString textTokens = textProcessor.process();

		TextUtil.TermProcessor termProcessor = new TextUtil.TermProcessor(textIndex, termValue.asString());

		TokenizedString termTokens = termProcessor.process();

		int termFrequency = TextUtil.termFrequency(textIndex, textTokens, termTokens);

		TextIndex.LocalTermWeights localTermWeights = textIndex.getLocalTermWeights();
		switch(localTermWeights){
			case BINARY:
			case TERM_FREQUENCY:
				return FieldValueUtil.create(TypeInfos.CONTINUOUS_INTEGER, termFrequency);
			case LOGARITHMIC:
				return FieldValueUtil.create(TypeInfos.CONTINUOUS_DOUBLE, Math.log10(1d + termFrequency));
			default:
				throw new UnsupportedAttributeException(textIndex, localTermWeights);
		}
	}

	static
	public FieldValue evaluateApply(Apply apply, EvaluationContext context){
		Object mapMissingTo = apply.getMapMissingTo();

		String function = apply.requireFunction();
		List<Expression> expressions = apply.getExpressions();

		List<FieldValue> values = new ArrayList<>(expressions.size());

		int max = expressions.size();

		if((PMMLFunctions.IF).equals(function)){

			if(max > 0){
				FieldValue flag = evaluate(expressions.get(0), context);

				if(flag == null && mapMissingTo != null){
					return FieldValueUtil.create(mapMissingTo);
				}

				values.add(flag);

				// Skip both THEN and ELSE parts
				if(flag == null){

					if(max > 1){
						values.add(FieldValues.MISSING_VALUE);

						if(max > 2){
							values.add(FieldValues.MISSING_VALUE);
						}
					}
				} else

				// Evaluate THEN part, skip ELSE part
				if(flag.asBoolean()){

					if(max > 1){
						FieldValue trueValue = evaluate(expressions.get(1), context);

						if(FieldValueUtil.isMissing(trueValue) && mapMissingTo != null){
							return FieldValueUtil.create(mapMissingTo);
						}

						values.add(trueValue);

						if(max > 2){
							values.add(FieldValues.MISSING_VALUE);
						}
					}
				} else

				// Skip THEN part, evaluate ELSE part
				{
					if(max > 1){
						values.add(FieldValues.MISSING_VALUE);

						if(max > 2){
							FieldValue falseValue = evaluate(expressions.get(2), context);

							if(FieldValueUtil.isMissing(falseValue) && mapMissingTo != null){
								return FieldValueUtil.create(mapMissingTo);
							}

							values.add(falseValue);
						}
					}
				}
			}
		}

		for(int i = values.size(); i < max; i++){
			Expression expression = expressions.get(i);

			FieldValue value = evaluate(expression, context);

			// "If a mapMissingTo value is specified and any of the input values of the function are missing, then the function is not applied at all and the mapMissingTo value is returned instead"
			if(FieldValueUtil.isMissing(value) && mapMissingTo != null){
				return FieldValueUtil.create(mapMissingTo);
			}

			values.add(value);
		}

		Object defaultValue = apply.getDefaultValue();

		FieldValue result;

		SymbolTable<String> symbolTable = EvaluationContext.FUNCTION_GUARD_PROVIDER.get();

		if(symbolTable != null){
			symbolTable.lock(function);
		}

		try {
			result = evaluateFunction(function, values, context);
		} catch(InvalidResultException ire){
			InvalidValueTreatmentMethod invalidValueTreatmentMethod = apply.getInvalidValueTreatment();

			switch(invalidValueTreatmentMethod){
				case RETURN_INVALID:
					throw new EvaluationException("Function " + EvaluationException.formatName(function) + " returned invalid value", apply)
						.initCause(ire);
				case AS_IS:
					// Re-throw the given InvalidResultException instance
					throw ire;
				case AS_MISSING:
					return FieldValueUtil.create(defaultValue);
				case AS_VALUE:
					throw new InvalidAttributeException(apply, invalidValueTreatmentMethod);
				default:
					throw new UnsupportedAttributeException(apply, invalidValueTreatmentMethod);
			}
		} finally {

			if(symbolTable != null){
				symbolTable.release(function);
			}
		}

		// "If a defaultValue value is specified and the function produced a missing value, then the defaultValue is returned"
		if(FieldValueUtil.isMissing(result) && defaultValue != null){
			return FieldValueUtil.create(defaultValue);
		}

		return result;
	}

	static
	private FieldValue evaluateFunction(String name, List<FieldValue> values, EvaluationContext context){
		Function function = FunctionRegistry.getFunction(name);
		if(function != null){
			return function.evaluate(values);
		}

		DefineFunction defineFunction = context.getDefineFunction(name);
		if(defineFunction != null){
			return evaluate(defineFunction, values, context);
		}

		throw new EvaluationException("Function " + EvaluationException.formatName(name) + " is not defined");
	}

	@SuppressWarnings("unchecked")
	static
	public FieldValue evaluateAggregate(Aggregate aggregate, EvaluationContext context){
		FieldValue value = context.evaluate(aggregate.requireField());

		if(FieldValueUtil.isMissing(value)){
			return FieldValues.MISSING_VALUE;
		}

		// The JPMML library operates with single records, so it's impossible to implement "proper" aggregation over multiple records.
		// It is assumed that application developers have performed the aggregation beforehand
		Collection<?> objects = value.asCollection();

		String groupName = aggregate.getGroupField();
		if(groupName != null){
			FieldValue groupValue = context.evaluate(groupName);

			// Ensure that the group value is a simple type, not a collection type
			@SuppressWarnings("unused")
			DataType dataType = TypeUtil.getDataType(FieldValueUtil.getValue(groupValue));
		}

		List<FieldValue> values = new ArrayList<>(objects.size());

		for(Object object : objects){

			// "Missing values are ignored"
			if(FieldValueUtil.isMissing(object)){
				continue;
			}

			values.add(FieldValueUtil.create(value, object));
		}

		Aggregate.Function function = aggregate.requireFunction();
		switch(function){
			case COUNT:
				return FieldValueUtil.create(TypeInfos.CONTINUOUS_INTEGER, values.size());
			case SUM:
				return Functions.SUM.evaluate(values);
			case AVERAGE:
				return Functions.AVG.evaluate(values);
			case MIN:
				return Collections.<ScalarValue>min((List)values);
			case MAX:
				return Collections.<ScalarValue>max((List)values);
			default:
				throw new UnsupportedAttributeException(aggregate, function);
		}
	}

	static
	public FieldValue evaluateJavaExpression(JavaExpression javaExpression, EvaluationContext context){
		FieldValue value = javaExpression.evaluate(context);

		return value;
	}

	static
	public boolean isEmptyContent(Object value){
		return (value == null) || ("").equals(value);
	}
}