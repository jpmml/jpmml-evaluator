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

import java.util.BitSet;
import java.util.List;

import org.dmg.pmml.BinarySimilarity;
import org.dmg.pmml.Chebychev;
import org.dmg.pmml.CityBlock;
import org.dmg.pmml.CompareFunction;
import org.dmg.pmml.ComparisonField;
import org.dmg.pmml.ComparisonMeasure;
import org.dmg.pmml.Distance;
import org.dmg.pmml.Euclidean;
import org.dmg.pmml.Jaccard;
import org.dmg.pmml.Measure;
import org.dmg.pmml.Minkowski;
import org.dmg.pmml.PMMLAttributes;
import org.dmg.pmml.Similarity;
import org.dmg.pmml.SimpleMatching;
import org.dmg.pmml.SquaredEuclidean;
import org.dmg.pmml.Tanimoto;
import org.jpmml.model.XPathUtil;

public class MeasureUtil {

	private MeasureUtil(){
	}

	static
	public Measure ensureMeasure(ComparisonMeasure comparisonMeasure){
		Measure measure = comparisonMeasure.getMeasure();
		if(measure == null){
			throw new MissingElementException(MissingElementException.formatMessage(XPathUtil.formatElement(comparisonMeasure.getClass()) + "/<Measure>"), comparisonMeasure);
		}

		return measure;
	}

	static
	public <V extends Number> Value<V> evaluateSimilarity(ValueFactory<V> valueFactory, ComparisonMeasure comparisonMeasure, List<? extends ComparisonField<?>> comparisonFields, BitSet flags, BitSet referenceFlags){
		Similarity measure = TypeUtil.cast(Similarity.class, comparisonMeasure.getMeasure());

		int a11 = 0;
		int a10 = 0;
		int a01 = 0;
		int a00 = 0;

		for(int i = 0; i < comparisonFields.size(); i++){

			if(flags.get(i)){

				if(referenceFlags.get(i)){
					a11 += 1;
				} else

				{
					a10 += 1;
				}
			} else

			{
				if(referenceFlags.get(i)){
					a01 += 1;
				} else

				{
					a00 += 1;
				}
			}
		}

		Value<V> numerator = valueFactory.newValue();
		Value<V> denominator = valueFactory.newValue();

		if(measure instanceof SimpleMatching){
			numerator.add(a11 + a00);
			denominator.add(a11 + a10 + a01 + a00);
		} else

		if(measure instanceof Jaccard){
			numerator.add(a11);
			denominator.add(a11 + a10 + a01);
		} else

		if(measure instanceof Tanimoto){
			numerator.add(a11 + a00);
			denominator
				.add(a11)
				.add(Numbers.DOUBLE_TWO, (a10 + a01))
				.add(a00);
		} else

		if(measure instanceof BinarySimilarity){
			BinarySimilarity binarySimilarity = (BinarySimilarity)measure;

			Number c00 = binarySimilarity.getC00Parameter();
			if(c00 == null){
				throw new MissingAttributeException(binarySimilarity, PMMLAttributes.BINARYSIMILARITY_C00PARAMETER);
			}

			Number c01 = binarySimilarity.getC01Parameter();
			if(c01 == null){
				throw new MissingAttributeException(binarySimilarity, PMMLAttributes.BINARYSIMILARITY_C01PARAMETER);
			}

			Number c10 = binarySimilarity.getC10Parameter();
			if(c10 == null){
				throw new MissingAttributeException(binarySimilarity, PMMLAttributes.BINARYSIMILARITY_C10PARAMETER);
			}

			Number c11 = binarySimilarity.getC11Parameter();
			if(c11 == null){
				throw new MissingAttributeException(binarySimilarity, PMMLAttributes.BINARYSIMILARITY_C11PARAMETER);
			}

			numerator
				.add(c11, a11)
				.add(c10, a10)
				.add(c01, a01)
				.add(c00, a00);

			Number d00 = binarySimilarity.getD00Parameter();
			if(d00 == null){
				throw new MissingAttributeException(binarySimilarity, PMMLAttributes.BINARYSIMILARITY_D00PARAMETER);
			}

			Number d01 = binarySimilarity.getD01Parameter();
			if(d01 == null){
				throw new MissingAttributeException(binarySimilarity, PMMLAttributes.BINARYSIMILARITY_D01PARAMETER);
			}

			Number d10 = binarySimilarity.getD10Parameter();
			if(d10 == null){
				throw new MissingAttributeException(binarySimilarity, PMMLAttributes.BINARYSIMILARITY_D10PARAMETER);
			}

			Number d11 = binarySimilarity.getD11Parameter();
			if(d11 == null){
				throw new MissingAttributeException(binarySimilarity, PMMLAttributes.BINARYSIMILARITY_D11PARAMETER);
			}

			denominator
				.add(d11, a11)
				.add(d10, a10)
				.add(d01, a01)
				.add(d00, a00);
		} else

		{
			throw new UnsupportedElementException(measure);
		} // End if

		if(denominator.isZero()){
			throw new UndefinedResultException();
		}

		return numerator.divide(denominator);
	}

	static
	public BitSet toBitSet(List<FieldValue> values){
		BitSet result = new BitSet(values.size());

		for(int i = 0; i < values.size(); i++){
			FieldValue value = values.get(i);

			if(value.equalsValue(Boolean.FALSE)){
				result.set(i, false);
			} else

			if(value.equalsValue(Boolean.TRUE)){
				result.set(i, true);
			} else

			{
				throw new EvaluationException("Expected " + PMMLException.formatValue(Boolean.FALSE) + " or " + PMMLException.formatValue(Boolean.TRUE) + ", got " + PMMLException.formatValue(value));
			}
		}

		return result;
	}

	static
	public <V extends Number> Value<V> evaluateDistance(ValueFactory<V> valueFactory, ComparisonMeasure comparisonMeasure, List<? extends ComparisonField<?>> comparisonFields, List<FieldValue> values, List<FieldValue> referenceValues, Value<V> adjustment){
		Distance measure = TypeUtil.cast(Distance.class, comparisonMeasure.getMeasure());

		Number innerPower;
		Number outerPower;

		if(measure instanceof Euclidean){
			innerPower = outerPower = Numbers.DOUBLE_TWO;
		} else

		if(measure instanceof SquaredEuclidean){
			innerPower = Numbers.DOUBLE_TWO;
			outerPower = Numbers.DOUBLE_ONE;
		} else

		if(measure instanceof Chebychev || measure instanceof CityBlock){
			innerPower = outerPower = Numbers.DOUBLE_ONE;
		} else

		if(measure instanceof Minkowski){
			Minkowski minkowski = (Minkowski)measure;

			Number p = minkowski.getPParameter();
			if(p == null){
				throw new MissingAttributeException(minkowski, PMMLAttributes.MINKOWSKI_PPARAMETER);
			} // End if

			if(p.doubleValue() < 0d){
				throw new InvalidAttributeException(minkowski, PMMLAttributes.MINKOWSKI_PPARAMETER, p);
			}

			innerPower = outerPower = p;
		} else

		{
			throw new UnsupportedElementException(measure);
		}

		Vector<V> distances = valueFactory.newVector(0);

		for(int i = 0, max = comparisonFields.size(); i < max; i++){
			ComparisonField<?> comparisonField = comparisonFields.get(i);

			FieldValue value = values.get(i);
			if(FieldValueUtil.isMissing(value)){
				continue;
			}

			FieldValue referenceValue = referenceValues.get(i);

			Value<V> distance = evaluateInnerFunction(valueFactory, comparisonMeasure, comparisonField, value, referenceValue, innerPower);

			distances.add(distance);
		}

		if(measure instanceof Euclidean || measure instanceof SquaredEuclidean || measure instanceof CityBlock || measure instanceof Minkowski){
			Value<V> result = distances.sum()
				.multiply(adjustment)
				.inversePower(outerPower);

			return result;
		} else

		if(measure instanceof Chebychev){
			Value<V> result = distances.max()
				.multiply(adjustment);

			return result;
		} else

		{
			throw new UnsupportedElementException(measure);
		}
	}

	static
	private <V extends Number> Value<V> evaluateInnerFunction(ValueFactory<V> valueFactory, ComparisonMeasure comparisonMeasure, ComparisonField<?> comparisonField, FieldValue value, FieldValue referenceValue, Number power){
		CompareFunction compareFunction = comparisonField.getCompareFunction();

		if(compareFunction == null){
			compareFunction = comparisonMeasure.getCompareFunction();

			// The ComparisonMeasure element is limited to "attribute-less" comparison functions
			switch(compareFunction){
				case ABS_DIFF:
				case DELTA:
				case EQUAL:
					break;
				case GAUSS_SIM:
				case TABLE:
					throw new InvalidAttributeException(comparisonMeasure, compareFunction);
				default:
					throw new UnsupportedAttributeException(comparisonMeasure, compareFunction);
			}
		}

		Value<V> distance;

		switch(compareFunction){
			case ABS_DIFF:
				{
					distance = valueFactory.newValue(value.asNumber())
						.subtract(referenceValue.asNumber())
						.abs();
				}
				break;
			case GAUSS_SIM:
				{
					Number similarityScale = comparisonField.getSimilarityScale();
					if(similarityScale == null){
						throw new InvalidElementException(comparisonField);
					}

					distance = valueFactory.newValue(value.asNumber())
						.subtract(referenceValue.asNumber())
						.gaussSim(similarityScale);
				}
				break;
			case DELTA:
				{
					boolean equals = (value).equalsValue(referenceValue);

					distance = valueFactory.newValue(equals ? Numbers.DOUBLE_ZERO : Numbers.DOUBLE_ONE);
				}
				break;
			case EQUAL:
				{
					boolean equals = (value).equalsValue(referenceValue);

					distance = valueFactory.newValue(equals ? Numbers.DOUBLE_ONE : Numbers.DOUBLE_ZERO);
				}
				break;
			case TABLE:
				throw new UnsupportedAttributeException(comparisonField, compareFunction);
			default:
				throw new UnsupportedAttributeException(comparisonField, compareFunction);
		}

		distance.power(power);

		Number fieldWeight = comparisonField.getFieldWeight();
		if(fieldWeight != null){
			distance.multiply(fieldWeight);
		}

		return distance;
	}

	static
	public <V extends Number> Value<V> calculateAdjustment(ValueFactory<V> valueFactory, List<FieldValue> values){
		return calculateAdjustment(valueFactory, values, null);
	}

	static
	public <V extends Number> Value<V> calculateAdjustment(ValueFactory<V> valueFactory, List<FieldValue> values, List<? extends Number> adjustmentValues){
		Value<V> sum = valueFactory.newValue();
		Value<V> nonmissingSum = valueFactory.newValue();

		for(int i = 0; i < values.size(); i++){
			FieldValue value = values.get(i);
			Number adjustmentValue = (adjustmentValues != null ? adjustmentValues.get(i) : Numbers.DOUBLE_ONE);

			sum.add(adjustmentValue);

			if(!FieldValueUtil.isMissing(value)){
				nonmissingSum.add(adjustmentValue);
			}
		}

		if(nonmissingSum.isZero()){
			throw new UndefinedResultException();
		}

		return sum.divide(nonmissingSum);
	}
}