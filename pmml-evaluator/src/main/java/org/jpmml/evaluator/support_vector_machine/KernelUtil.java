/*
 * Copyright (c) 2013 KNIME.com AG, Zurich, Switzerland
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
package org.jpmml.evaluator.support_vector_machine;

import org.dmg.pmml.support_vector_machine.Kernel;
import org.dmg.pmml.support_vector_machine.LinearKernel;
import org.dmg.pmml.support_vector_machine.PolynomialKernel;
import org.dmg.pmml.support_vector_machine.RadialBasisKernel;
import org.dmg.pmml.support_vector_machine.SigmoidKernel;
import org.jpmml.evaluator.EvaluationException;
import org.jpmml.evaluator.UnsupportedFeatureException;

public class KernelUtil {

	private KernelUtil(){
	}

	static
	public double evaluate(Kernel kernel, double[] input, double[] vector){

		if(kernel instanceof LinearKernel){
			return evaluateLinearKernel((LinearKernel)kernel, input, vector);
		} else

		if(kernel instanceof PolynomialKernel){
			return evaluatePolynomialKernel((PolynomialKernel)kernel, input, vector);
		} else

		if(kernel instanceof RadialBasisKernel){
			return evaluateRadialBasisKernel((RadialBasisKernel)kernel, input, vector);
		} else

		if(kernel instanceof SigmoidKernel){
			return evaluateSigmoidKernel((SigmoidKernel)kernel, input, vector);
		}

		throw new UnsupportedFeatureException(kernel);
	}

	static
	public double evaluateLinearKernel(LinearKernel linearKernel, double[] input, double[] vector){
		return dotProduct(input, vector);
	}

	static
	public double evaluatePolynomialKernel(PolynomialKernel polynomialKernel, double[] input, double[] vector){
		return Math.pow(polynomialKernel.getGamma() * dotProduct(input, vector) + polynomialKernel.getCoef0(), polynomialKernel.getDegree());
	}

	static
	public double evaluateRadialBasisKernel(RadialBasisKernel radialBasisKernel, double[] input, double[] vector){
		return Math.exp(-radialBasisKernel.getGamma() * squaredDistance(input, vector));
	}

	static
	public double evaluateSigmoidKernel(SigmoidKernel sigmoidKernel, double[] input, double[] vector){
		return Math.tanh(sigmoidKernel.getGamma() * dotProduct(input, vector) + sigmoidKernel.getCoef0());
	}

	static
	private double dotProduct(double[] left, double[] right){
		double sum = 0d;

		if(left.length != right.length){
			throw new EvaluationException();
		}

		for(int i = 0; i < left.length; i++){
			sum += (left[i] * right[i]);
		}

		return sum;
	}

	static
	private double squaredDistance(double[] left, double[] right){
		double sum = 0d;

		if(left.length != right.length){
			throw new EvaluationException();
		}

		for(int i = 0; i < left.length; i++){
			double diff = (left[i] - right[i]);

			sum += (diff * diff);
		}

		return sum;
	}
}