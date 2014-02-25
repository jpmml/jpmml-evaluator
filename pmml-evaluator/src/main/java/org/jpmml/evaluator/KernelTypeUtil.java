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
package org.jpmml.evaluator;

import org.jpmml.manager.*;

import org.dmg.pmml.*;

public class KernelTypeUtil {

	private KernelTypeUtil(){
	}

	static
	public double evaluate(KernelType kernelType, double[] input, double[] vector){

		if(kernelType instanceof LinearKernelType){
			return evaluateLinearKernel((LinearKernelType)kernelType, input, vector);
		} else

		if(kernelType instanceof PolynomialKernelType){
			return evaluatePolynomialKernel((PolynomialKernelType)kernelType, input, vector);
		} else

		if(kernelType instanceof RadialBasisKernelType){
			return evaluateRadialBasisKernel((RadialBasisKernelType)kernelType, input, vector);
		} else

		if(kernelType instanceof SigmoidKernelType){
			return evaluateSigmoidKernel((SigmoidKernelType)kernelType, input, vector);
		}

		throw new UnsupportedFeatureException(kernelType);
	}

	static
	public double evaluateLinearKernel(LinearKernelType linearKernelType, double[] input, double[] vector){
		return dotProduct(input, vector);
	}

	static
	public double evaluatePolynomialKernel(PolynomialKernelType polynomialKernelType, double[] input, double[] vector){
		return Math.pow(polynomialKernelType.getGamma() * dotProduct(input, vector) + polynomialKernelType.getCoef0(), polynomialKernelType.getDegree());
	}

	static
	public double evaluateRadialBasisKernel(RadialBasisKernelType radialBasisKernelType, double[] input, double[] vector){
		return Math.exp(-radialBasisKernelType.getGamma() * squaredDistance(input, vector));
	}

	static
	public double evaluateSigmoidKernel(SigmoidKernelType sigmoidKernelType, double[] input, double[] vector){
		return Math.tanh(sigmoidKernelType.getGamma() * dotProduct(input, vector) + sigmoidKernelType.getCoef0());
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