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
import java.util.Objects;
import java.util.stream.Collectors;

import org.dmg.pmml.Aggregate;
import org.dmg.pmml.Apply;
import org.dmg.pmml.Constant;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Discretize;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldColumnPair;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.HasDataType;
import org.dmg.pmml.HasExpression;
import org.dmg.pmml.HasFieldReference;
import org.dmg.pmml.HasType;
import org.dmg.pmml.InvalidValueTreatmentMethod;
import org.dmg.pmml.MapValues;
import org.dmg.pmml.NormContinuous;
import org.dmg.pmml.NormDiscrete;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.TextIndex;

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
	public <E extends PMMLObject & HasType<E> & HasExpression<E>> FieldValue evaluateTypedExpressionContainer(E hasTypedExpression, EvaluationContext context){
		FieldValue value = evaluateExpressionContainer(hasTypedExpression, context);

		if(Objects.equals(FieldValues.MISSING_VALUE, value)){
			return FieldValues.MISSING_VALUE;
		}

		return value.cast(hasTypedExpression.getDataType(), hasTypedExpression.getOpType());
	}

	static
	public <E extends PMMLObject & HasExpression<E>> FieldValue evaluateExpressionContainer(E hasExpression, EvaluationContext context){
		return evaluate(ensureExpression(hasExpression), context);
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
		DataType dataType = getConstantDataType(constant);
		OpType opType = TypeUtil.getOpType(dataType);

		if(constant instanceof HasParsedValue){
			HasParsedValue<?> hasParsedValue = (HasParsedValue<?>)constant;

			TypeInfo typeInfo = new TypeInfo(){

				@Override
				public DataType getDataType(){
					return dataType;
				}

				@Override
				public OpType getOpType(){
					return opType;
				}
			};

			return hasParsedValue.getValue(typeInfo);
		}

		return FieldValueUtil.create(dataType, opType, constant.getValue());
	}

	static
	public FieldValue evaluateFieldRef(FieldRef fieldRef, EvaluationContext context){
		FieldValue value = context.evaluate(ensureField(fieldRef));

		if(Objects.equals(FieldValues.MISSING_VALUE, value)){
			return FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, fieldRef.getMapMissingTo());
		}

		return value;
	}

	static
	public FieldValue evaluateNormContinuous(NormContinuous normContinuous, EvaluationContext context){
		FieldValue value = context.evaluate(ensureField(normContinuous));

		if(Objects.equals(FieldValues.MISSING_VALUE, value)){
			return FieldValueUtil.create(TypeInfos.CONTINUOUS_DOUBLE, normContinuous.getMapMissingTo());
		}

		return NormalizationUtil.normalize(normContinuous, value);
	}

	static
	public FieldValue evaluateNormDiscrete(NormDiscrete normDiscrete, EvaluationContext context){
		FieldValue value = context.evaluate(ensureField(normDiscrete));

		if(Objects.equals(FieldValues.MISSING_VALUE, value)){
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

		if(Objects.equals(FieldValues.MISSING_VALUE, value)){
			return FieldValueUtil.create(getDataType(discretize, DataType.STRING), OpType.CATEGORICAL, discretize.getMapMissingTo());
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
			if(Objects.equals(FieldValues.MISSING_VALUE, value)){
				return FieldValueUtil.create(getDataType(mapValues, DataType.STRING), OpType.CATEGORICAL, mapValues.getMapMissingTo());
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
		if(Objects.equals(FieldValues.MISSING_VALUE, textIndex) || Objects.equals(FieldValues.MISSING_VALUE, termValue)){
			return FieldValues.MISSING_VALUE;
		}

		TextUtil.TextProcessor textProcessor = new TextUtil.TextProcessor(textIndex, textValue);

		List<String> textTokens = textProcessor.process();

		TextUtil.TermProcessor termProcessor = new TextUtil.TermProcessor(textIndex, termValue);

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

		condition:
		if(("if").equals(function)){

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

						if(Objects.equals(FieldValues.MISSING_VALUE, trueValue) && mapMissingTo != null){
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

							if(Objects.equals(FieldValues.MISSING_VALUE, falseValue) && mapMissingTo != null){
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
			if(Objects.equals(FieldValues.MISSING_VALUE, value) && mapMissingTo != null){
				return FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, mapMissingTo);
			}

			values.add(value);
		}

		String defaultValue = apply.getDefaultValue();

		FieldValue result;

		try {
			result = FunctionUtil.evaluate(apply, values, context);
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
		}

		// "If a defaultValue value is specified and the function produced a missing value, then the defaultValue is returned"
		if(result == null && defaultValue != null){
			return FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, defaultValue);
		}

		return result;
	}

	@SuppressWarnings (
		value = {"unchecked"}
	)
	static
	public FieldValue evaluateAggregate(Aggregate aggregate, EvaluationContext context){
		FieldValue fieldValue = context.evaluate(ensureField(aggregate));

		// The JPMML library operates with single records, so it's impossible to implement "proper" aggregation over multiple records.
		// It is assumed that application developers have performed the aggregation beforehand
		Collection<?> values = FieldValueUtil.getValue(Collection.class, fieldValue);

		FieldName groupName = aggregate.getGroupField();
		if(groupName != null){
			FieldValue groupValue = context.evaluate(groupName);

			// Ensure that the group value is a simple type, not a collection type
			TypeUtil.getDataType(FieldValueUtil.getValue(groupValue));
		}

		values = values.stream()
			// "Missing values are ignored"
			.filter(Objects::nonNull)
			.map(value -> FieldValueUtil.create(fieldValue, value))
			.collect(Collectors.toList());

		Aggregate.Function function = aggregate.getFunction();
		if(function == null){
			throw new MissingAttributeException(aggregate, PMMLAttributes.AGGREGATE_FUNCTION);
		}

		switch(function){
			case COUNT:
				return FieldValueUtil.create(TypeInfos.CONTINUOUS_INTEGER, values.size());
			case SUM:
				return Functions.SUM.evaluate((List<FieldValue>)values);
			case AVERAGE:
				return Functions.AVG.evaluate((List<FieldValue>)values);
			case MIN:
				return Collections.min((List<FieldValue>)values);
			case MAX:
				return Collections.max((List<FieldValue>)values);
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
	public DataType getConstantDataType(Constant constant){
		DataType dataType = constant.getDataType();

		if(dataType == null){
			dataType = TypeUtil.getConstantDataType(constant.getValue());
		}

		return dataType;
	}

	static
	public <E extends Expression & HasDataType<E>> DataType getDataType(E expression, DataType defaultDataType){
		DataType dataType = expression.getDataType();

		if(dataType != null){
			return dataType;
		}

		return defaultDataType;
	}
}