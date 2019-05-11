/*
 * Copyright (c) 2019 Villu Ruusmann
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
package org.jpmml.evaluator.support_vector_machine;

import org.dmg.pmml.support_vector_machine.Kernel;
import org.dmg.pmml.support_vector_machine.LinearKernel;
import org.dmg.pmml.support_vector_machine.PolynomialKernel;
import org.dmg.pmml.support_vector_machine.RadialBasisKernel;
import org.dmg.pmml.support_vector_machine.SigmoidKernel;
import org.jpmml.evaluator.UnsupportedElementException;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;

public class KernelUtil {

	private KernelUtil(){
	}

	static
	public <V extends Number> Value<V> evaluate(Kernel kernel, ValueFactory<V> valueFactory, Object input, Object vector){

		if(kernel instanceof LinearKernel){
			return evaluateLinearKernel((LinearKernel)kernel, valueFactory, input, vector);
		} else

		if(kernel instanceof PolynomialKernel){
			return evaluatePolynomialKernel((PolynomialKernel)kernel, valueFactory, input, vector);
		} else

		if(kernel instanceof RadialBasisKernel){
			return evaluateRadialBasisKernel((RadialBasisKernel)kernel, valueFactory, input, vector);
		} else

		if(kernel instanceof SigmoidKernel){
			return evaluateSigmoidKernel((SigmoidKernel)kernel, valueFactory, input, vector);
		}

		throw new UnsupportedElementException(kernel);
	}

	static
	public <V extends Number> Value<V> evaluateLinearKernel(LinearKernel linearKernel, ValueFactory<V> valueFactory, Object input, Object vector){
		Value<V> result = valueFactory.newValue(dotProduct(input, vector));

		return result;
	}

	static
	public <V extends Number> Value<V> evaluatePolynomialKernel(PolynomialKernel polynomialKernel, ValueFactory<V> valueFactory, Object input, Object vector){
		Value<V> result = valueFactory.newValue(dotProduct(input, vector))
			.multiply(polynomialKernel.getGamma())
			.add(polynomialKernel.getCoef0())
			.power(polynomialKernel.getDegree());

		return result;
	}

	static
	public <V extends Number> Value<V> evaluateRadialBasisKernel(RadialBasisKernel radialBasisKernel, ValueFactory<V> valueFactory, Object input, Object vector){
		Value<V> result = valueFactory.newValue(negativeSquaredDistance(input, vector))
			.multiply(radialBasisKernel.getGamma())
			.exp();

		return result;
	}

	static
	public <V extends Number> Value<V> evaluateSigmoidKernel(SigmoidKernel sigmoidKernel, ValueFactory<V> valueFactory, Object input, Object vector){
		Value<V> result = valueFactory.newValue(dotProduct(input, vector))
			.multiply(sigmoidKernel.getGamma())
			.add(sigmoidKernel.getCoef0())
			.tanh();

		return result;
	}

	static
	private Number dotProduct(Object left, Object right){

		if((left instanceof float[]) && (right instanceof float[])){
			return dotProduct((float[])left, (float[])right);
		} else

		if((left instanceof double[]) && (right instanceof double[])){
			return dotProduct((double[])left, (double[])right);
		} else

		{
			throw new IllegalArgumentException();
		}
	}

	static
	private float dotProduct(float[] left, float[] right){

		if(left.length != right.length){
			throw new IllegalArgumentException();
		}

		float sum = 0f;

		for(int i = 0, max = left.length; i < max; i++){
			sum += (left[i] * right[i]);
		}

		return sum;
	}

	static
	private double dotProduct(double[] left, double[] right){

		if(left.length != right.length){
			throw new IllegalArgumentException();
		}

		double sum = 0d;

		for(int i = 0, max = left.length; i < max; i++){
			sum += (left[i] * right[i]);
		}

		return sum;
	}

	static
	private Number negativeSquaredDistance(Object left, Object right){

		if((left instanceof float[]) && (right instanceof float[])){
			return -squaredDistance((float[])left, (float[])right);
		} else

		if((left instanceof double[]) && (right instanceof double[])){
			return -squaredDistance((double[])left, (double[])right);
		} else

		{
			throw new IllegalArgumentException();
		}
	}

	static
	private float squaredDistance(float[] left, float[] right){

		if(left.length != right.length){
			throw new IllegalArgumentException();
		}

		float sum = 0f;

		for(int i = 0, max = left.length; i < max; i++){
			float diff = (left[i] - right[i]);

			sum += (diff * diff);
		}

		return sum;
	}

	static
	private double squaredDistance(double[] left, double[] right){

		if(left.length != right.length){
			throw new IllegalArgumentException();
		}

		double sum = 0d;

		for(int i = 0, max = left.length; i < max; i++){
			double diff = (left[i] - right[i]);

			sum += (diff * diff);
		}

		return sum;
	}
}