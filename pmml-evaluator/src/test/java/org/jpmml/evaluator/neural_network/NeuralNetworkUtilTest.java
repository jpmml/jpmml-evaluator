/*
 * Copyright (c) 2020 Villu Ruusmann
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

import org.dmg.pmml.neural_network.NeuralNetwork;
import org.jpmml.evaluator.DoubleValue;
import org.jpmml.evaluator.FloatValue;
import org.jpmml.evaluator.Value;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NeuralNetworkUtilTest {

	@Test
	public void thresholdActivation(){
		assertEquals(new FloatValue(1f), threshold(new FloatValue(2f), 1f));
		assertEquals(new FloatValue(0f), threshold(new FloatValue(2f), 3f));
	}

	@Test
	public void rectifierActivation(){
		assertEquals(new DoubleValue(0d), relu(new DoubleValue(-1d), null));
		assertEquals(new DoubleValue(-1d * 0.01d), relu(new DoubleValue(-1d), 0.01d));

		assertEquals(new DoubleValue(2d), relu(new DoubleValue(2d), null));
		assertEquals(new DoubleValue(2d), relu(new DoubleValue(2d), 0.01d));
	}

	static
	private <V extends Number> Value<V> threshold(Value<V> value, Number threshold){
		return NeuralNetworkUtil.activateNeuronOutput(NeuralNetwork.ActivationFunction.THRESHOLD, threshold, null, value);
	}

	static
	private <V extends Number> Value<V> relu(Value<V> value, Number leakage){
		return NeuralNetworkUtil.activateNeuronOutput(NeuralNetwork.ActivationFunction.RECTIFIER, null, leakage, value);
	}
}