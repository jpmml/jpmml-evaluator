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
import java.util.Objects;

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
import org.dmg.pmml.Similarity;
import org.dmg.pmml.SimpleMatching;
import org.dmg.pmml.SquaredEuclidean;
import org.dmg.pmml.Tanimoto;

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
			denominator.add(a11).add(2d, (a10 + a01)).add(a00);
		} else

		if(measure instanceof BinarySimilarity){
			BinarySimilarity binarySimilarity = (BinarySimilarity)measure;

			numerator.add(binarySimilarity.getC11Parameter(), a11).add(binarySimilarity.getC10Parameter(), a10).add(binarySimilarity.getC01Parameter(), a01).add(binarySimilarity.getC00Parameter(), a00);
			denominator.add(binarySimilarity.getD11Parameter(), a11).add(binarySimilarity.getD10Parameter(), a10).add(binarySimilarity.getD01Parameter(), a01).add(binarySimilarity.getD00Parameter(), a00);
		} else

		{
			throw new UnsupportedElementException(measure);
		} // End if

		if(denominator.equals(0d)){
			throw new UndefinedResultException();
		}

		return numerator.divide(denominator);
	}

	static
	public BitSet toBitSet(List<FieldValue> values){
		BitSet result = new BitSet(values.size());

		for(int i = 0; i < values.size(); i++){
			FieldValue value = values.get(i);

			if((FieldValues.CONTINUOUS_DOUBLE_ZERO).equalsValue(value)){
				result.set(i, false);
			} else

			if((FieldValues.CONTINUOUS_DOUBLE_ONE).equalsValue(value)){
				result.set(i, true);
			} else

			{
				throw new EvaluationException("Expected " + PMMLException.formatValue(FieldValues.CONTINUOUS_DOUBLE_ZERO) + " or " + PMMLException.formatValue(FieldValues.CONTINUOUS_DOUBLE_ONE) + ", got " + PMMLException.formatValue(value));
			}
		}

		return result;
	}

	static
	public <V extends Number> Value<V> evaluateDistance(ValueFactory<V> valueFactory, ComparisonMeasure comparisonMeasure, List<? extends ComparisonField<?>> comparisonFields, List<FieldValue> values, List<FieldValue> referenceValues, Value<V> adjustment){
		Distance measure = TypeUtil.cast(Distance.class, comparisonMeasure.getMeasure());

		double innerPower;
		double outerPower;

		if(measure instanceof Euclidean){
			innerPower = outerPower = 2d;
		} else

		if(measure instanceof SquaredEuclidean){
			innerPower = 2d;
			outerPower = 1d;
		} else

		if(measure instanceof Chebychev || measure instanceof CityBlock){
			innerPower = outerPower = 1d;
		} else

		if(measure instanceof Minkowski){
			Minkowski minkowski = (Minkowski)measure;

			double p = minkowski.getPParameter();
			if(p < 0d){
				throw new InvalidAttributeException(minkowski, PMMLAttributes.MINKOWSKI_PPARAMETER, p);
			}

			innerPower = outerPower = p;
		} else

		{
			throw new UnsupportedElementException(measure);
		}

		Vector<V> distances = valueFactory.newVector(0);

		for(int i = 0, max = comparisonFields.size(); i < max; i++){
			ComparisonField comparisonField = comparisonFields.get(i);

			FieldValue value = values.get(i);
			if(Objects.equals(FieldValues.MISSING_VALUE, value)){
				continue;
			}

			FieldValue referenceValue = referenceValues.get(i);

			Value<V> distance = evaluateInnerFunction(valueFactory, comparisonMeasure, comparisonField, value, referenceValue, innerPower);

			distances.add(distance.doubleValue());
		}

		if(measure instanceof Euclidean || measure instanceof SquaredEuclidean || measure instanceof CityBlock || measure instanceof Minkowski){
			Value<V> result = distances.sum();

			if(!adjustment.equals(1d)){
				result.multiply(adjustment);
			} // End if

			if(outerPower != 1d){
				result.inversePower(outerPower);
			}

			return result;
		} else

		if(measure instanceof Chebychev){
			Value<V> result = distances.max();

			if(!adjustment.equals(1d)){
				result.multiply(adjustment);
			}

			return result;
		} else

		{
			throw new UnsupportedElementException(measure);
		}
	}

	static
	private <V extends Number> Value<V> evaluateInnerFunction(ValueFactory<V> valueFactory, ComparisonMeasure comparisonMeasure, ComparisonField<?> comparisonField, FieldValue value, FieldValue referenceValue, double power){
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
					distance = valueFactory.newValue((value.asNumber()).doubleValue()).subtract((referenceValue.asNumber()).doubleValue());

					distance.abs();
				}
				break;
			case GAUSS_SIM:
				{
					Double similarityScale = comparisonField.getSimilarityScale();
					if(similarityScale == null){
						throw new InvalidElementException(comparisonField);
					}

					distance = valueFactory.newValue((value.asNumber()).doubleValue()).subtract((referenceValue.asNumber()).doubleValue());

					distance.gaussSim(similarityScale);
				}
				break;
			case DELTA:
				{
					boolean equals = (value).equalsValue(referenceValue);

					distance = valueFactory.newValue(equals ? 0d : 1d);
				}
				break;
			case EQUAL:
				{
					boolean equals = (value).equalsValue(referenceValue);

					distance = valueFactory.newValue(equals ? 1d : 0d);
				}
				break;
			case TABLE:
				throw new UnsupportedAttributeException(comparisonField, compareFunction);
			default:
				throw new UnsupportedAttributeException(comparisonField, compareFunction);
		}

		if(power != 1d){
			distance.power(power);
		}

		Double fieldWeight = comparisonField.getFieldWeight();
		if(fieldWeight != null && fieldWeight != 1d){
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
			double adjustmentValue = (adjustmentValues != null ? (adjustmentValues.get(i)).doubleValue() : 1d);

			if(adjustmentValue != 0d){
				sum.add(adjustmentValue);

				if(value != null){
					nonmissingSum.add(adjustmentValue);
				}
			}
		}

		if(nonmissingSum.equals(0d)){
			throw new UndefinedResultException();
		}

		return sum.divide(nonmissingSum);
	}
}