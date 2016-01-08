/*
 * Copyright (c) 2012 University of Tartu
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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.dmg.pmml.ActivationFunctionType;
import org.dmg.pmml.Connection;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Entity;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.MiningFunctionType;
import org.dmg.pmml.NeuralInput;
import org.dmg.pmml.NeuralInputs;
import org.dmg.pmml.NeuralLayer;
import org.dmg.pmml.NeuralNetwork;
import org.dmg.pmml.NeuralOutput;
import org.dmg.pmml.NeuralOutputs;
import org.dmg.pmml.Neuron;
import org.dmg.pmml.NnNormalizationMethodType;
import org.dmg.pmml.NormContinuous;
import org.dmg.pmml.NormDiscrete;
import org.dmg.pmml.PMML;
import org.dmg.pmml.PMMLObject;

public class NeuralNetworkEvaluator extends ModelEvaluator<NeuralNetwork> implements HasEntityRegistry<Entity> {

	public NeuralNetworkEvaluator(PMML pmml){
		super(pmml, NeuralNetwork.class);
	}

	public NeuralNetworkEvaluator(PMML pmml, NeuralNetwork neuralNetwork){
		super(pmml, neuralNetwork);
	}

	@Override
	public String getSummary(){
		return "Neural network";
	}

	@Override
	public BiMap<String, Entity> getEntityRegistry(){
		return getValue(NeuralNetworkEvaluator.entityCache);
	}

	@Override
	public Map<FieldName, ?> evaluate(ModelEvaluationContext context){
		NeuralNetwork neuralNetwork = getModel();
		if(!neuralNetwork.isScorable()){
			throw new InvalidResultException(neuralNetwork);
		}

		Map<FieldName, ?> predictions;

		MiningFunctionType miningFunction = neuralNetwork.getFunctionName();
		switch(miningFunction){
			case REGRESSION:
				predictions = evaluateRegression(context);
				break;
			case CLASSIFICATION:
				predictions = evaluateClassification(context);
				break;
			default:
				throw new UnsupportedFeatureException(neuralNetwork, miningFunction);
		}

		return OutputUtil.evaluate(predictions, context);
	}

	private Map<FieldName, ?> evaluateRegression(ModelEvaluationContext context){
		NeuralNetwork neuralNetwork = getModel();

		Map<String, Double> entityOutputs = evaluateRaw(context);
		if(entityOutputs == null){
			return TargetUtil.evaluateRegressionDefault(context);
		}

		Map<FieldName, Double> result = new LinkedHashMap<>();

		NeuralOutputs neuralOutputs = neuralNetwork.getNeuralOutputs();
		if(neuralOutputs == null){
			throw new InvalidFeatureException(neuralNetwork);
		}

		for(NeuralOutput neuralOutput : neuralOutputs){
			String id = neuralOutput.getOutputNeuron();

			Expression expression = getOutputExpression(neuralOutput);

			if(expression instanceof FieldRef){
				FieldRef fieldRef = (FieldRef)expression;

				FieldName name = fieldRef.getField();

				Double value = entityOutputs.get(id);

				result.put(name, value);
			} else

			if(expression instanceof NormContinuous){
				NormContinuous normContinuous = (NormContinuous)expression;

				FieldName name = normContinuous.getField();

				Double value = NormalizationUtil.denormalize(normContinuous, entityOutputs.get(id));

				result.put(name, value);
			} else

			{
				throw new UnsupportedFeatureException(expression);
			}
		}

		return TargetUtil.evaluateRegression(result, context);
	}

	private Map<FieldName, ? extends Classification> evaluateClassification(ModelEvaluationContext context){
		NeuralNetwork neuralNetwork = getModel();

		BiMap<String, Entity> entityRegistry = getEntityRegistry();

		Map<String, Double> entityOutputs = evaluateRaw(context);
		if(entityOutputs == null){
			return TargetUtil.evaluateClassificationDefault(context);
		}

		Map<FieldName, EntityProbabilityDistribution<Entity>> result = new LinkedHashMap<>();

		NeuralOutputs neuralOutputs = neuralNetwork.getNeuralOutputs();
		if(neuralOutputs == null){
			throw new InvalidFeatureException(neuralNetwork);
		}

		for(NeuralOutput neuralOutput : neuralOutputs){
			String id = neuralOutput.getOutputNeuron();

			Entity entity = entityRegistry.get(id);

			Expression expression = getOutputExpression(neuralOutput);

			if(expression instanceof NormDiscrete){
				NormDiscrete normDiscrete = (NormDiscrete)expression;

				FieldName name = normDiscrete.getField();

				EntityProbabilityDistribution<Entity> values = result.get(name);
				if(values == null){
					values = new EntityProbabilityDistribution<>(entityRegistry);

					result.put(name, values);
				}

				Double value = entityOutputs.get(id);

				values.put(entity, normDiscrete.getValue(), value);
			} else

			{
				throw new UnsupportedFeatureException(expression);
			}
		}

		return TargetUtil.evaluateClassification(result, context);
	}

	private Expression getOutputExpression(NeuralOutput neuralOutput){

		DerivedField derivedField = neuralOutput.getDerivedField();
		if(derivedField == null){
			throw new InvalidFeatureException(neuralOutput);
		}

		Expression expression = derivedField.getExpression();
		if(expression == null){
			throw new InvalidFeatureException(derivedField);
		}

		return expression;
	}

	/**
	 * @return A map between {@link Entity#getId() Entity identifiers} and their outputs.
	 *
	 * @see NeuralInput#getId()
	 * @see Neuron#getId()
	 */
	private Map<String, Double> evaluateRaw(EvaluationContext context){
		NeuralNetwork neuralNetwork = getModel();

		Map<String, Double> result = new HashMap<>();

		NeuralInputs neuralInputs = neuralNetwork.getNeuralInputs();
		if(neuralInputs == null){
			throw new InvalidFeatureException(neuralNetwork);
		}

		for(NeuralInput neuralInput : neuralInputs){
			DerivedField derivedField = neuralInput.getDerivedField();

			FieldValue value = ExpressionUtil.evaluate(derivedField, context);
			if(value == null){
				return null;
			}

			result.put(neuralInput.getId(), (value.asNumber()).doubleValue());
		}

		List<NeuralLayer> neuralLayers = neuralNetwork.getNeuralLayers();
		for(NeuralLayer neuralLayer : neuralLayers){
			Map<String, Double> outputs = new HashMap<>();

			List<Neuron> neurons = neuralLayer.getNeurons();
			for(Neuron neuron : neurons){
				double z = neuron.getBias();

				List<Connection> connections = neuron.getConnections();
				for(Connection connection : connections){
					double input = result.get(connection.getFrom());

					z += input * connection.getWeight();
				}

				double output = activation(z, neuralLayer);

				outputs.put(neuron.getId(), output);
			}

			normalizeNeuronOutputs(neuralLayer, outputs);

			result.putAll(outputs);
		}

		return result;
	}

	private void normalizeNeuronOutputs(NeuralLayer neuralLayer, Map<String, Double> values){
		NeuralNetwork neuralNetwork = getModel();

		PMMLObject locatable = neuralLayer;

		NnNormalizationMethodType normalizationMethod = neuralLayer.getNormalizationMethod();
		if(normalizationMethod == null){
			locatable = neuralNetwork;

			normalizationMethod = neuralNetwork.getNormalizationMethod();
		}

		switch(normalizationMethod){
			case NONE:
				break;
			case SIMPLEMAX:
				Classification.normalize(values);
				break;
			case SOFTMAX:
				Classification.normalizeSoftMax(values);
				break;
			default:
				throw new UnsupportedFeatureException(locatable, normalizationMethod);
		}
	}

	private double activation(double z, NeuralLayer neuralLayer){
		NeuralNetwork neuralNetwork = getModel();

		PMMLObject locatable = neuralLayer;

		ActivationFunctionType activationFunction = neuralLayer.getActivationFunction();
		if(activationFunction == null){
			locatable = neuralNetwork;

			activationFunction = neuralNetwork.getActivationFunction();
		} // End if

		if(activationFunction == null){
			throw new InvalidFeatureException(locatable);
		}

		switch(activationFunction){
			case THRESHOLD:
				Double threshold = neuralLayer.getThreshold();
				if(threshold == null){
					threshold = Double.valueOf(neuralNetwork.getThreshold());
				}
				return z > threshold.doubleValue() ? 1d : 0d;
			case LOGISTIC:
				return 1d / (1d + Math.exp(-z));
			case TANH:
				return Math.tanh(z);
			case IDENTITY:
				return z;
			case EXPONENTIAL:
				return Math.exp(z);
			case RECIPROCAL:
				return 1d / z;
			case SQUARE:
				return z * z;
			case GAUSS:
				return Math.exp(-(z * z));
			case SINE:
				return Math.sin(z);
			case COSINE:
				return Math.cos(z);
			case ELLIOTT:
				return z / (1d + Math.abs(z));
			case ARCTAN:
				return Math.atan(z);
			default:
				throw new UnsupportedFeatureException(locatable, activationFunction);
		}
	}

	private static final LoadingCache<NeuralNetwork, BiMap<String, Entity>> entityCache = CacheUtil.buildLoadingCache(new CacheLoader<NeuralNetwork, BiMap<String, Entity>>(){

		@Override
		public BiMap<String, Entity> load(NeuralNetwork neuralNetwork){
			ImmutableBiMap.Builder<String, Entity> builder = new ImmutableBiMap.Builder<>();

			AtomicInteger index = new AtomicInteger(1);

			NeuralInputs neuralInputs = neuralNetwork.getNeuralInputs();
			if(neuralInputs == null){
				throw new InvalidFeatureException(neuralNetwork);
			}

			for(NeuralInput neuralInput : neuralInputs){
				builder = EntityUtil.put(neuralInput, index, builder);
			}

			List<NeuralLayer> neuralLayers = neuralNetwork.getNeuralLayers();
			for(NeuralLayer neuralLayer : neuralLayers){
				List<Neuron> neurons = neuralLayer.getNeurons();

				for(Neuron neuron : neurons){
					builder = EntityUtil.put(neuron, index, builder);
				}
			}

			return builder.build();
		}
	});
}