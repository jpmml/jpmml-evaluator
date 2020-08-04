/*
 * Copyright (c) 2017 Villu Ruusmann
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
package org.jpmml.evaluator.neural_network;

import java.util.Collection;

import org.dmg.pmml.neural_network.NeuralNetwork;
import org.jpmml.evaluator.Numbers;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueUtil;

public class NeuralNetworkUtil {

	private NeuralNetworkUtil(){
	}

	static
	public <V extends Number> Value<V> activateNeuronOutput(NeuralNetwork.ActivationFunction activationFunction, Number threshold, Number leakage, Value<V> value){

		switch(activationFunction){
			case THRESHOLD:
				if(threshold == null){
					throw new IllegalArgumentException();
				}
				return value.threshold(threshold);
			case LOGISTIC:
				return value.inverseLogit();
			case TANH:
				return value.tanh();
			case IDENTITY:
				return value;
			case EXPONENTIAL:
				return value.exp();
			case RECIPROCAL:
				return value.reciprocal();
			case SQUARE:
				return value.square();
			case GAUSS:
				return value.gauss();
			case SINE:
				return value.sin();
			case COSINE:
				return value.cos();
			case ELLIOTT:
				return value.elliott();
			case ARCTAN:
				return value.arctan();
			case RECTIFIER:
				if(leakage != null){

					if(value.compareTo(Numbers.DOUBLE_ZERO) > 0){
						return value;
					} else

					{
						return value.multiply(leakage);
					}
				}
				return value.relu();
			default:
				throw new IllegalArgumentException();
		}
	}

	static
	public <V extends Number> Collection<Value<V>> normalizeNeuralLayerOutputs(NeuralNetwork.NormalizationMethod normalizationMethod, Collection<Value<V>> values){

		switch(normalizationMethod){
			case NONE:
				break;
			case SIMPLEMAX:
				ValueUtil.normalizeSimpleMax(values);
				break;
			case SOFTMAX:
				ValueUtil.normalizeSoftMax(values);
				break;
			default:
				throw new IllegalArgumentException();
		}

		return values;
	}
}