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

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.dmg.pmml.Aggregate;
import org.dmg.pmml.Apply;
import org.dmg.pmml.Constant;
import org.dmg.pmml.DataType;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Discretize;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldColumnPair;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.InvalidValueTreatmentMethod;
import org.dmg.pmml.MapValues;
import org.dmg.pmml.NormContinuous;
import org.dmg.pmml.NormDiscrete;
import org.dmg.pmml.OpType;
import org.dmg.pmml.TextIndex;
import org.dmg.pmml.TypeDefinitionField;

public class ExpressionUtil {

	private ExpressionUtil(){
	}

	static
	public FieldValue evaluate(DerivedField derivedField, EvaluationContext context){
		Expression expression = derivedField.getExpression();
		if(expression == null){
			throw new InvalidFeatureException(derivedField);
		}

		FieldValue value = evaluate(expression, context);

		return FieldValueUtil.refine(derivedField, value);
	}

	static
	public FieldValue evaluate(Expression expression, EvaluationContext context){

		try {
			return evaluateExpression(expression, context);
		} catch(PMMLException pe){
			pe.ensureContext(expression);

			throw pe;
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

		throw new UnsupportedFeatureException(expression);
	}

	/**
	 * @throws TypeAnalysisException If the data type cannot be determined.
	 */
	static
	public DataType getDataType(Expression expression, ModelEvaluator<?> modelEvaluator){

		if(expression instanceof Constant){
			Constant constant = (Constant)expression;

			return getConstantDataType(constant);
		} else

		if(expression instanceof FieldRef){
			FieldRef fieldRef = (FieldRef)expression;

			FieldName name = fieldRef.getField();

			TypeDefinitionField field = modelEvaluator.resolveField(name);
			if(field == null){
				throw new MissingFieldException(name, expression);
			}

			return field.getDataType();
		} else

		if(expression instanceof NormContinuous){
			return DataType.DOUBLE;
		} else

		if(expression instanceof NormDiscrete){
			return DataType.DOUBLE;
		} else

		if(expression instanceof Discretize){
			Discretize discretize = (Discretize)expression;

			DataType dataType = discretize.getDataType();
			if(dataType == null){
				dataType = DataType.STRING;
			}

			return dataType;
		} else

		if(expression instanceof MapValues){
			MapValues mapValues = (MapValues)expression;

			DataType dataType = mapValues.getDataType();
			if(dataType == null){
				dataType = DataType.STRING;
			}

			return dataType;
		} else

		if(expression instanceof TextIndex){
			TextIndex textIndex = (TextIndex)expression;

			return getTextIndexDataType(textIndex);
		} else

		if(expression instanceof Apply){
			throw new TypeAnalysisException(expression);
		} else

		if(expression instanceof Aggregate){
			throw new TypeAnalysisException(expression);
		}

		throw new UnsupportedFeatureException(expression);
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
	public DataType getTextIndexDataType(TextIndex textIndex){
		TextIndex.LocalTermWeights localTermWeights = textIndex.getLocalTermWeights();

		switch(localTermWeights){
			case BINARY:
			case TERM_FREQUENCY:
				return DataType.INTEGER;
			case LOGARITHMIC:
				return DataType.DOUBLE;
			default:
				throw new UnsupportedFeatureException(textIndex, localTermWeights);
		}
	}

	static
	public FieldValue evaluateConstant(Constant constant){

		if(constant instanceof HasParsedValue){
			HasParsedValue<?> hasParsedValue = (HasParsedValue<?>)constant;

			return hasParsedValue.getValue(null, null);
		}

		return FieldValueUtil.create(getConstantDataType(constant), null, constant.getValue());
	}

	static
	public FieldValue evaluateFieldRef(FieldRef fieldRef, EvaluationContext context){
		FieldValue value = context.evaluate(fieldRef.getField());

		if(value == null){
			return FieldValueUtil.create(DataType.STRING, OpType.CATEGORICAL, fieldRef.getMapMissingTo());
		}

		return value;
	}

	static
	public FieldValue evaluateNormContinuous(NormContinuous normContinuous, EvaluationContext context){
		FieldValue value = context.evaluate(normContinuous.getField());

		if(value == null){
			return FieldValueUtil.create(DataType.DOUBLE, OpType.CONTINUOUS, normContinuous.getMapMissingTo());
		}

		return NormalizationUtil.normalize(normContinuous, value);
	}

	static
	public FieldValue evaluateNormDiscrete(NormDiscrete normDiscrete, EvaluationContext context){
		FieldValue value = context.evaluate(normDiscrete.getField());

		if(value == null){
			return FieldValueUtil.create(DataType.DOUBLE, OpType.CATEGORICAL, normDiscrete.getMapMissingTo());
		}

		NormDiscrete.Method method = normDiscrete.getMethod();
		switch(method){
			case INDICATOR:
				{
					boolean equals = value.equals(normDiscrete);

					return (equals ? FieldValues.CATEGORICAL_DOUBLE_ONE : FieldValues.CATEGORICAL_DOUBLE_ZERO);
				}
			default:
				throw new UnsupportedFeatureException(normDiscrete, method);
		}
	}

	static
	public FieldValue evaluateDiscretize(Discretize discretize, EvaluationContext context){
		FieldValue value = context.evaluate(discretize.getField());

		if(value == null){
			return FieldValueUtil.create(discretize.getDataType(), null, discretize.getMapMissingTo());
		}

		return DiscretizationUtil.discretize(discretize, value);
	}

	static
	public FieldValue evaluateMapValues(MapValues mapValues, EvaluationContext context){
		Map<String, FieldValue> values = new LinkedHashMap<>();

		List<FieldColumnPair> fieldColumnPairs = mapValues.getFieldColumnPairs();
		for(FieldColumnPair fieldColumnPair : fieldColumnPairs){
			FieldValue value = context.evaluate(fieldColumnPair.getField());

			if(value == null){
				return FieldValueUtil.create(mapValues.getDataType(), null, mapValues.getMapMissingTo());
			}

			values.put(fieldColumnPair.getColumn(), value);
		}

		return DiscretizationUtil.mapValue(mapValues, values);
	}

	static
	public FieldValue evaluateTextIndex(TextIndex textIndex, EvaluationContext context){
		FieldValue textValue = context.evaluate(textIndex.getTextField());

		Expression expression = textIndex.getExpression();
		if(expression == null){
			throw new InvalidFeatureException(textIndex);
		}

		FieldValue termValue = ExpressionUtil.evaluate(expression, context);

		// See http://mantis.dmg.org/view.php?id=171
		if(textValue == null || termValue == null){
			return null;
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
				return FieldValueUtil.create(DataType.INTEGER, OpType.CONTINUOUS, termFrequency);
			case LOGARITHMIC:
				return FieldValueUtil.create(DataType.DOUBLE, OpType.CONTINUOUS, Math.log10(1d + termFrequency));
			default:
				throw new UnsupportedFeatureException(textIndex, localTermWeights);
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
					return FieldValueUtil.create(DataType.STRING, OpType.CATEGORICAL, mapMissingTo);
				}

				values.add(flag);

				// Skip both THEN and ELSE parts
				if(flag == null){

					if(arguments.hasNext()){
						arguments.next();

						values.add(null);

						if(arguments.hasNext()){
							arguments.next();

							values.add(null);
						}
					}

					break condition;
				} // End if

				// Evaluate THEN part, skip ELSE part
				if(flag.asBoolean()){

					if(arguments.hasNext()){
						FieldValue trueValue = evaluate(arguments.next(), context);

						if(trueValue == null && mapMissingTo != null){
							return FieldValueUtil.create(DataType.STRING, OpType.CATEGORICAL, mapMissingTo);
						}

						values.add(trueValue);

						if(arguments.hasNext()){
							arguments.next();

							values.add(null);
						}
					}
				} else

				// Skip THEN part, evaluate ELSE part
				{
					if(arguments.hasNext()){
						arguments.next();

						values.add(null);

						if(arguments.hasNext()){
							FieldValue falseValue = evaluate(arguments.next(), context);

							if(falseValue == null && mapMissingTo != null){
								return FieldValueUtil.create(DataType.STRING, OpType.CATEGORICAL, mapMissingTo);
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
			if(value == null && mapMissingTo != null){
				return FieldValueUtil.create(DataType.STRING, OpType.CATEGORICAL, mapMissingTo);
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
					throw new InvalidResultException(apply);
				case AS_IS:
					// Re-throw the given InvalidResultException instance
					throw ire;
				case AS_MISSING:
					return FieldValueUtil.create(DataType.STRING, OpType.CATEGORICAL, defaultValue);
				default:
					throw new UnsupportedFeatureException(apply, invalidValueTreatmentMethod);
			}
		}

		// "If a defaultValue value is specified and the function produced a missing value, then the defaultValue is returned"
		if(result == null && defaultValue != null){
			return FieldValueUtil.create(DataType.STRING, OpType.CATEGORICAL, defaultValue);
		}

		return result;
	}

	@SuppressWarnings (
		value = {"rawtypes", "unchecked"}
	)
	static
	public FieldValue evaluateAggregate(Aggregate aggregate, EvaluationContext context){
		FieldValue value = context.evaluate(aggregate.getField());

		// The JPMML library operates with single records, so it's impossible to implement "proper" aggregation over multiple records.
		// It is assumed that application developers have performed the aggregation beforehand
		Collection<?> values = FieldValueUtil.getValue(Collection.class, value);

		FieldName groupName = aggregate.getGroupField();
		if(groupName != null){
			FieldValue groupValue = context.evaluate(groupName);

			// Ensure that the group value is a simple type, not a collection type
			TypeUtil.getDataType(FieldValueUtil.getValue(groupValue));
		}

		// Remove missing values
		values = Lists.newArrayList(Iterables.filter(values, Predicates.notNull()));

		Aggregate.Function function = aggregate.getFunction();
		switch(function){
			case COUNT:
				return FieldValueUtil.create(DataType.INTEGER, OpType.CONTINUOUS, values.size());
			case SUM:
				return Functions.SUM.evaluate(FieldValueUtil.createAll(null, null, (List<?>)values));
			case AVERAGE:
				return Functions.AVG.evaluate(FieldValueUtil.createAll(null, null, (List<?>)values));
			case MIN:
				return FieldValueUtil.create(null, null, Collections.min((List<Comparable>)values));
			case MAX:
				return FieldValueUtil.create(null, null, Collections.max((List<Comparable>)values));
			default:
				throw new UnsupportedFeatureException(aggregate, function);
		}
	}

	static
	public FieldValue evaluateJavaExpression(JavaExpression javaExpression, EvaluationContext context){
		FieldValue value = javaExpression.evaluate(context);

		return value;
	}
}