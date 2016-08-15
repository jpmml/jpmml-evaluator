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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Entity;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.NormContinuous;
import org.dmg.pmml.NormDiscrete;
import org.dmg.pmml.PMML;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.TypeDefinitionField;
import org.dmg.pmml.neural_network.Connection;
import org.dmg.pmml.neural_network.NeuralInput;
import org.dmg.pmml.neural_network.NeuralInputs;
import org.dmg.pmml.neural_network.NeuralLayer;
import org.dmg.pmml.neural_network.NeuralNetwork;
import org.dmg.pmml.neural_network.NeuralOutput;
import org.dmg.pmml.neural_network.NeuralOutputs;
import org.dmg.pmml.neural_network.Neuron;

public class NeuralNetworkEvaluator extends ModelEvaluator<NeuralNetwork> implements HasEntityRegistry<Entity> {

	transient
	private BiMap<String, Entity> entityRegistry = null;


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

		if(this.entityRegistry == null){
			this.entityRegistry = getValue(NeuralNetworkEvaluator.entityCache);
		}

		return this.entityRegistry;
	}

	@Override
	public Map<FieldName, ?> evaluate(ModelEvaluationContext context){
		NeuralNetwork neuralNetwork = getModel();
		if(!neuralNetwork.isScorable()){
			throw new InvalidResultException(neuralNetwork);
		}

		Map<FieldName, ?> predictions;

		MiningFunction miningFunction = neuralNetwork.getMiningFunction();
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

		Map<FieldName, Object> result = new LinkedHashMap<>();

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

		Collection<Map.Entry<FieldName, Object>> entries = result.entrySet();
		for(Map.Entry<FieldName, Object> entry : entries){
			entry.setValue(TargetUtil.evaluateRegressionInternal(entry.getKey(), entry.getValue(), context));
		}

		return result;
	}

	@SuppressWarnings (
		value = {"unchecked"}
	)
	private Map<FieldName, ? extends Classification> evaluateClassification(ModelEvaluationContext context){
		NeuralNetwork neuralNetwork = getModel();

		BiMap<String, Entity> entityRegistry = getEntityRegistry();

		Map<String, Double> entityOutputs = evaluateRaw(context);
		if(entityOutputs == null){
			return TargetUtil.evaluateClassificationDefault(context);
		}

		Map<FieldName, Classification> result = new LinkedHashMap<>();

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

				EntityProbabilityDistribution<Entity> values = (EntityProbabilityDistribution<Entity>)result.get(name);
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

		Collection<Map.Entry<FieldName, Classification>> entries = result.entrySet();
		for(Map.Entry<FieldName, Classification> entry : entries){
			entry.setValue(TargetUtil.evaluateClassificationInternal(entry.getKey(), entry.getValue(), context));
		}

		return result;
	}

	private Expression getOutputExpression(NeuralOutput neuralOutput){
		DerivedField derivedField = neuralOutput.getDerivedField();
		if(derivedField == null){
			throw new InvalidFeatureException(neuralOutput);
		}

		Expression expression = derivedField.getExpression();
		if(expression == null){
			throw new InvalidFeatureException(derivedField);
		} // End if

		if(expression instanceof FieldRef){
			FieldRef fieldRef = (FieldRef)expression;

			FieldName name = fieldRef.getField();

			TypeDefinitionField field = resolveField(name);
			if(field == null){
				throw new MissingFieldException(name, fieldRef);
			} // End if

			if(field instanceof DataField){
				return expression;
			} else

			if(field instanceof DerivedField){
				DerivedField targetDerivedField = (DerivedField)field;

				Expression targetExpression = targetDerivedField.getExpression();
				if(targetExpression == null){
					throw new InvalidFeatureException(targetDerivedField);
				}

				return targetExpression;
			} else

			{
				throw new InvalidFeatureException(fieldRef);
			}
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

		BiMap<String, Entity> entityRegistry = getEntityRegistry();

		Map<String, Double> result = new HashMap<>(entityRegistry.size());

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

			result.put(neuralInput.getId(), value.asDouble());
		}

		Map<String, Double> outputs = new HashMap<>();

		List<NeuralLayer> neuralLayers = neuralNetwork.getNeuralLayers();
		for(NeuralLayer neuralLayer : neuralLayers){
			outputs.clear();

			List<Neuron> neurons = neuralLayer.getNeurons();
			for(int i = 0; i < neurons.size(); i++){
				Neuron neuron = neurons.get(i);

				double z = 0d;

				List<Connection> connections = neuron.getConnections();
				for(int j = 0; j < connections.size(); j++){
					Connection connection = connections.get(j);

					double input = result.get(connection.getFrom());

					z += input * connection.getWeight();
				}

				Double bias = neuron.getBias();
				if(bias != null){
					z += bias;
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

		NeuralNetwork.NormalizationMethod normalizationMethod = neuralLayer.getNormalizationMethod();
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

		NeuralNetwork.ActivationFunction activationFunction = neuralLayer.getActivationFunction();
		if(activationFunction == null){
			locatable = neuralNetwork;

			activationFunction = neuralNetwork.getActivationFunction();
		} // End if

		if(activationFunction == null){
			throw new InvalidFeatureException(neuralLayer);
		}

		switch(activationFunction){
			case THRESHOLD:
				Double threshold = neuralLayer.getThreshold();
				if(threshold == null){
					threshold = neuralNetwork.getThreshold();
				} // End if

				if(threshold == null){
					throw new InvalidFeatureException(neuralLayer);
				}

				return z > threshold ? 1d : 0d;
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

				for(int i = 0; i < neurons.size(); i++){
					Neuron neuron = neurons.get(i);

					builder = EntityUtil.put(neuron, index, builder);
				}
			}

			return builder.build();
		}
	});
}