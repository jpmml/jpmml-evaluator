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
import java.util.Iterator;
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
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.HasExpression;
import org.dmg.pmml.HasFieldReference;
import org.dmg.pmml.HasType;
import org.dmg.pmml.InvalidValueTreatmentMethod;
import org.dmg.pmml.MapValues;
import org.dmg.pmml.NormContinuous;
import org.dmg.pmml.NormDiscrete;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMMLAttributes;
import org.dmg.pmml.PMMLFunctions;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.ParameterField;
import org.dmg.pmml.TextIndex;
import org.jpmml.model.XPathUtil;

public class ExpressionUtil {

	private ExpressionUtil(){
	}

	static
	public <E extends Expression & HasFieldReference<E>> FieldName ensureField(E hasField){
		FieldName name = hasField.getField();
		if(name == null){
			throw new MissingAttributeException(MissingAttributeException.formatMessage(XPathUtil.formatElement(hasField.getClass()) + "@field"), hasField);
		}

		return name;
	}

	static
	public <E extends PMMLObject & HasExpression<E>> Expression ensureExpression(E hasExpression){
		Expression expression = hasExpression.getExpression();
		if(expression == null){
			throw new MissingElementException(MissingElementException.formatMessage(XPathUtil.formatElement(hasExpression.getClass()) + "/<Expression>"), hasExpression);
		}

		return expression;
	}

	static
	public <E extends PMMLObject & HasExpression<E>> FieldValue evaluateExpressionContainer(E hasExpression, EvaluationContext context){
		return evaluate(ensureExpression(hasExpression), context);
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
		FieldName name = derivedField.getName();
		if(name == null){
			throw new MissingAttributeException(derivedField, PMMLAttributes.DERIVEDFIELD_NAME);
		}

		SymbolTable<FieldName> symbolTable = EvaluationContext.DERIVEDFIELD_GUARD_PROVIDER.get();

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
			throw new EvaluationException("Function " + PMMLException.formatKey(defineFunction.getName()) + " expects " + parameterFields.size() + " arguments, got " + values.size() + " arguments");
		}

		DefineFunctionEvaluationContext functionContext = new DefineFunctionEvaluationContext(defineFunction, context);

		for(int i = 0; i < parameterFields.size(); i++){
			ParameterField parameterField = parameterFields.get(i);
			FieldValue value = values.get(i);

			FieldName name = parameterField.getName();
			if(name == null){
				throw new MissingAttributeException(parameterField, PMMLAttributes.PARAMETERFIELD_NAME);
			}

			value = value.cast(parameterField);

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

		{
			if(isEmptyContent(value)){
				return FieldValues.MISSING_VALUE;
			}

			dataType = TypeUtil.getConstantDataType(value);

		}

		OpType opType = TypeUtil.getOpType(dataType);

		return FieldValueUtil.create(dataType, opType, value);
	}

	static
	public FieldValue evaluateFieldRef(FieldRef fieldRef, EvaluationContext context){
		FieldValue value = context.evaluate(ensureField(fieldRef));

		if(FieldValueUtil.isMissing(value)){
			return FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, fieldRef.getMapMissingTo());
		}

		return value;
	}

	static
	public FieldValue evaluateNormContinuous(NormContinuous normContinuous, EvaluationContext context){
		FieldValue value = context.evaluate(ensureField(normContinuous));

		if(FieldValueUtil.isMissing(value)){
			return FieldValueUtil.create(TypeInfos.CONTINUOUS_DOUBLE, normContinuous.getMapMissingTo());
		}

		return NormalizationUtil.normalize(normContinuous, value);
	}

	static
	public FieldValue evaluateNormDiscrete(NormDiscrete normDiscrete, EvaluationContext context){
		FieldValue value = context.evaluate(ensureField(normDiscrete));

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
		FieldValue value = context.evaluate(ensureField(discretize));

		if(FieldValueUtil.isMissing(value)){
			return FieldValueUtil.create(discretize.getDataType(DataType.STRING), OpType.CATEGORICAL, discretize.getMapMissingTo());
		}

		return DiscretizationUtil.discretize(discretize, value);
	}

	static
	public FieldValue evaluateMapValues(MapValues mapValues, EvaluationContext context){
		Map<String, FieldValue> values = new LinkedHashMap<>();

		List<FieldColumnPair> fieldColumnPairs = mapValues.getFieldColumnPairs();
		for(FieldColumnPair fieldColumnPair : fieldColumnPairs){
			FieldName name = fieldColumnPair.getField();
			if(name == null){
				throw new MissingAttributeException(fieldColumnPair, PMMLAttributes.FIELDCOLUMNPAIR_FIELD);
			}

			String column = fieldColumnPair.getColumn();
			if(column == null){
				throw new MissingAttributeException(fieldColumnPair, PMMLAttributes.FIELDCOLUMNPAIR_COLUMN);
			}

			FieldValue value = context.evaluate(name);
			if(FieldValueUtil.isMissing(value)){
				return FieldValueUtil.create(mapValues.getDataType(DataType.STRING), OpType.CATEGORICAL, mapValues.getMapMissingTo());
			}

			values.put(column, value);
		}

		return DiscretizationUtil.mapValue(mapValues, values);
	}

	static
	public FieldValue evaluateTextIndex(TextIndex textIndex, EvaluationContext context){
		FieldName textName = textIndex.getTextField();
		if(textName == null){
			throw new MissingAttributeException(textIndex, PMMLAttributes.TEXTINDEX_TEXTFIELD);
		}

		FieldValue textValue = context.evaluate(textName);

		FieldValue termValue = ExpressionUtil.evaluateExpressionContainer(textIndex, context);

		// See http://mantis.dmg.org/view.php?id=171
		if(FieldValueUtil.isMissing(textValue) || FieldValueUtil.isMissing(termValue)){
			return FieldValues.MISSING_VALUE;
		}

		TextUtil.TextProcessor textProcessor = new TextUtil.TextProcessor(textIndex, textValue.asString());

		List<String> textTokens = textProcessor.process();

		TextUtil.TermProcessor termProcessor = new TextUtil.TermProcessor(textIndex, termValue.asString());

		List<String> termTokens = termProcessor.process();

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
		String mapMissingTo = apply.getMapMissingTo();

		List<Expression> expressions = apply.getExpressions();

		List<FieldValue> values = new ArrayList<>(expressions.size());

		Iterator<Expression> arguments = expressions.iterator();

		String function = apply.getFunction();
		if(function == null){
			throw new MissingAttributeException(apply, PMMLAttributes.APPLY_FUNCTION);
		}

		condition:
		if((PMMLFunctions.IF).equals(function)){

			if(arguments.hasNext()){
				FieldValue flag = evaluate(arguments.next(), context);

				if(flag == null && mapMissingTo != null){
					return FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, mapMissingTo);
				}

				values.add(flag);

				// Skip both THEN and ELSE parts
				if(flag == null){

					if(arguments.hasNext()){
						arguments.next();

						values.add(FieldValues.MISSING_VALUE);

						if(arguments.hasNext()){
							arguments.next();

							values.add(FieldValues.MISSING_VALUE);
						}
					}

					break condition;
				} // End if

				// Evaluate THEN part, skip ELSE part
				if(flag.asBoolean()){

					if(arguments.hasNext()){
						FieldValue trueValue = evaluate(arguments.next(), context);

						if(FieldValueUtil.isMissing(trueValue) && mapMissingTo != null){
							return FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, mapMissingTo);
						}

						values.add(trueValue);

						if(arguments.hasNext()){
							arguments.next();

							values.add(FieldValues.MISSING_VALUE);
						}
					}
				} else

				// Skip THEN part, evaluate ELSE part
				{
					if(arguments.hasNext()){
						arguments.next();

						values.add(FieldValues.MISSING_VALUE);

						if(arguments.hasNext()){
							FieldValue falseValue = evaluate(arguments.next(), context);

							if(FieldValueUtil.isMissing(falseValue) && mapMissingTo != null){
								return FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, mapMissingTo);
							}

							values.add(falseValue);
						}
					}
				}
			}
		}

		while(arguments.hasNext()){
			FieldValue value = evaluate(arguments.next(), context);

			// "If a mapMissingTo value is specified and any of the input values of the function are missing, then the function is not applied at all and the mapMissingTo value is returned instead"
			if(FieldValueUtil.isMissing(value) && mapMissingTo != null){
				return FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, mapMissingTo);
			}

			values.add(value);
		}

		String defaultValue = apply.getDefaultValue();

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
					throw new InvalidResultException("Function application yielded an invalid result", apply)
						.initCause(ire);
				case AS_IS:
					// Re-throw the given InvalidResultException instance
					throw ire;
				case AS_MISSING:
					return FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, defaultValue);
				default:
					throw new UnsupportedAttributeException(apply, invalidValueTreatmentMethod);
			}
		} finally {

			if(symbolTable != null){
				symbolTable.release(function);
			}
		}

		// "If a defaultValue value is specified and the function produced a missing value, then the defaultValue is returned"
		if(result == null && defaultValue != null){
			return FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, defaultValue);
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

		throw new EvaluationException("Function " + PMMLException.formatKey(name) + " is not defined");
	}

	@SuppressWarnings (
		value = {"unchecked"}
	)
	static
	public FieldValue evaluateAggregate(Aggregate aggregate, EvaluationContext context){
		FieldValue value = context.evaluate(ensureField(aggregate));

		if(FieldValueUtil.isMissing(value)){
			return FieldValues.MISSING_VALUE;
		}

		// The JPMML library operates with single records, so it's impossible to implement "proper" aggregation over multiple records.
		// It is assumed that application developers have performed the aggregation beforehand
		Collection<?> objects = value.asCollection();

		FieldName groupName = aggregate.getGroupField();
		if(groupName != null){
			FieldValue groupValue = context.evaluate(groupName);

			// Ensure that the group value is a simple type, not a collection type
			TypeUtil.getDataType(FieldValueUtil.getValue(groupValue));
		}

		List<FieldValue> values = new ArrayList<>(objects.size());

		for(Object object : objects){

			// "Missing values are ignored"
			if(FieldValueUtil.isMissing(object)){
				continue;
			}

			values.add(FieldValueUtil.create(value, object));
		}

		Aggregate.Function function = aggregate.getFunction();
		if(function == null){
			throw new MissingAttributeException(aggregate, PMMLAttributes.AGGREGATE_FUNCTION);
		}

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