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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ListMultimap;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Entity;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.MathContext;
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
import org.jpmml.evaluator.CacheUtil;
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.EntityUtil;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.ExpressionUtil;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.HasEntityRegistry;
import org.jpmml.evaluator.InvalidFeatureException;
import org.jpmml.evaluator.InvalidResultException;
import org.jpmml.evaluator.MissingFieldException;
import org.jpmml.evaluator.ModelEvaluationContext;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.NormalizationUtil;
import org.jpmml.evaluator.OutputUtil;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TargetUtil;
import org.jpmml.evaluator.UnsupportedFeatureException;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueMap;

public class NeuralNetworkEvaluator extends ModelEvaluator<NeuralNetwork> implements HasEntityRegistry<Entity> {

	transient
	private Map<FieldName, List<NeuralOutput>> neuralOutputMap = null;

	transient
	private BiMap<String, Entity> entityRegistry = null;


	public NeuralNetworkEvaluator(PMML pmml){
		this(pmml, selectModel(pmml, NeuralNetwork.class));
	}

	public NeuralNetworkEvaluator(PMML pmml, NeuralNetwork neuralNetwork){
		super(pmml, neuralNetwork);

		NeuralInputs neuralInputs = neuralNetwork.getNeuralInputs();
		if(neuralInputs == null){
			throw new InvalidFeatureException(neuralNetwork);
		} // End if

		if(!neuralInputs.hasNeuralInputs()){
			throw new InvalidFeatureException(neuralInputs);
		} // End if

		if(!neuralNetwork.hasNeuralLayers()){
			throw new InvalidFeatureException(neuralNetwork);
		}

		NeuralOutputs neuralOutputs = neuralNetwork.getNeuralOutputs();
		if(neuralOutputs == null){
			throw new InvalidFeatureException(neuralNetwork);
		} // End if

		if(!neuralOutputs.hasNeuralOutputs()){
			throw new InvalidFeatureException(neuralOutputs);
		}
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

		ValueFactory<?> valueFactory;

		MathContext mathContext = neuralNetwork.getMathContext();
		switch(mathContext){
			case FLOAT:
			case DOUBLE:
				valueFactory = getValueFactory();
				break;
			default:
				throw new UnsupportedFeatureException(neuralNetwork, mathContext);
		}

		Map<FieldName, ?> predictions;

		MiningFunction miningFunction = neuralNetwork.getMiningFunction();
		switch(miningFunction){
			case REGRESSION:
				predictions = evaluateRegression(valueFactory, context);
				break;
			case CLASSIFICATION:
				predictions = evaluateClassification(valueFactory, context);
				break;
			default:
				throw new UnsupportedFeatureException(neuralNetwork, miningFunction);
		}

		return OutputUtil.evaluate(predictions, context);
	}

	private <V extends Number> Map<FieldName, ?> evaluateRegression(ValueFactory<V> valueFactory, EvaluationContext context){
		NeuralNetwork neuralNetwork = getModel();

		List<TargetField> targetFields = getTargetFields();

		ValueMap<String, V> values = evaluateRaw(valueFactory, context);
		if(values == null){

			if(targetFields.size() == 1){
				TargetField targetField = targetFields.get(0);

				return TargetUtil.evaluateRegressionDefault(valueFactory, targetField);
			}

			Map<FieldName, Object> results = new LinkedHashMap<>();

			for(TargetField targetField : targetFields){
				results.putAll(TargetUtil.evaluateRegressionDefault(valueFactory, targetField));
			}

			return results;
		}

		Map<FieldName, List<NeuralOutput>> neuralOutputMap = getNeuralOutputMap();

		Map<FieldName, Object> results = null;

		for(TargetField targetField : targetFields){
			List<NeuralOutput> neuralOutputs = neuralOutputMap.get(targetField.getName());
			if(neuralOutputs == null || neuralOutputs.size() != 1){
				throw new InvalidFeatureException(neuralNetwork);
			}

			NeuralOutput neuralOutput = neuralOutputs.get(0);

			String id = neuralOutput.getOutputNeuron();

			Value<V> value = values.get(id);
			if(value == null){
				throw new InvalidFeatureException(neuralOutput);
			}

			value = value.copy();

			Expression expression = getOutputExpression(neuralOutput);

			if(expression instanceof FieldRef){
				// Ignored
			} else

			if(expression instanceof NormContinuous){
				NormContinuous normContinuous = (NormContinuous)expression;

				NormalizationUtil.denormalize(normContinuous, value);
			} else

			{
				throw new UnsupportedFeatureException(expression);
			} // End if

			if(targetFields.size() == 1){
				return TargetUtil.evaluateRegression(targetField, value);
			} // End if

			if(results == null){
				results = new LinkedHashMap<>();
			}

			results.putAll(TargetUtil.evaluateRegression(targetField, value));
		}

		return results;
	}

	private <V extends Number> Map<FieldName, ? extends Classification> evaluateClassification(ValueFactory<V> valueFactory, EvaluationContext context){
		NeuralNetwork neuralNetwork = getModel();

		List<TargetField> targetFields = getTargetFields();

		ValueMap<String, V> values = evaluateRaw(valueFactory, context);
		if(values == null){

			if(targetFields.size() == 1){
				TargetField targetField = targetFields.get(0);

				return TargetUtil.evaluateClassificationDefault(valueFactory, targetField);
			}

			Map<FieldName, Classification> results = new LinkedHashMap<>();

			for(TargetField targetField : targetFields){
				results.putAll(TargetUtil.evaluateClassificationDefault(valueFactory, targetField));
			}

			return results;
		}

		Map<FieldName, List<NeuralOutput>> neuralOutputMap = getNeuralOutputMap();

		BiMap<String, Entity> entityRegistry = getEntityRegistry();

		Map<FieldName, Classification> results = null;

		for(TargetField targetField : targetFields){
			List<NeuralOutput> neuralOutputs = neuralOutputMap.get(targetField.getName());
			if(neuralOutputs == null){
				throw new InvalidFeatureException(neuralNetwork);
			}

			NeuronProbabilityDistribution result = new NeuronProbabilityDistribution(entityRegistry);

			for(NeuralOutput neuralOutput : neuralOutputs){
				String id = neuralOutput.getOutputNeuron();

				Entity entity = entityRegistry.get(id);
				if(entity == null){
					throw new InvalidFeatureException(neuralOutput);
				}

				Value<V> value = values.get(id);
				if(value == null){
					throw new InvalidFeatureException(neuralOutput);
				}

				Expression expression = getOutputExpression(neuralOutput);

				if(expression instanceof NormDiscrete){
					NormDiscrete normDiscrete = (NormDiscrete)expression;

					String targetCategory = normDiscrete.getValue();
					if(targetCategory == null){
						throw new InvalidFeatureException(normDiscrete);
					}

					result.put(entity, targetCategory, value.doubleValue());
				} else

				{
					throw new UnsupportedFeatureException(expression);
				}
			}

			if(targetFields.size() == 1){
				return TargetUtil.evaluateClassification(targetField, result);
			} // End if

			if(results == null){
				results = new LinkedHashMap<>();
			}

			results.putAll(TargetUtil.evaluateClassification(targetField, result));
		}

		return results;
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

	private <V extends Number> ValueMap<String, V> evaluateRaw(ValueFactory<V> valueFactory, EvaluationContext context){
		NeuralNetwork neuralNetwork = getModel();

		BiMap<String, Entity> entityRegistry = getEntityRegistry();

		ValueMap<String, V> result = new ValueMap<>(2 * entityRegistry.size());

		NeuralInputs neuralInputs = neuralNetwork.getNeuralInputs();
		for(NeuralInput neuralInput : neuralInputs){
			DerivedField derivedField = neuralInput.getDerivedField();
			if(derivedField == null){
				throw new InvalidFeatureException(neuralInput);
			}

			FieldValue value = ExpressionUtil.evaluate(derivedField, context);
			if(value == null){
				return null;
			}

			Value<V> output = valueFactory.newValue(value.asNumber());

			result.put(neuralInput.getId(), output);
		}

		List<Value<V>> outputs = new ArrayList<>();

		List<NeuralLayer> neuralLayers = neuralNetwork.getNeuralLayers();
		for(NeuralLayer neuralLayer : neuralLayers){
			outputs.clear();

			PMMLObject locatable = neuralLayer;

			NeuralNetwork.ActivationFunction activationFunction = neuralLayer.getActivationFunction();
			if(activationFunction == null){
				locatable = neuralNetwork;

				activationFunction = neuralNetwork.getActivationFunction();
			} // End if

			if(activationFunction == null){
				throw new InvalidFeatureException(neuralLayer);
			}

			Double threshold = neuralLayer.getThreshold();
			if(threshold == null){
				threshold = neuralNetwork.getThreshold();
			}

			switch(activationFunction){
				case THRESHOLD:
					if(threshold == null){
						throw new InvalidFeatureException(neuralLayer);
					}
					break;
				case LOGISTIC:
				case TANH:
				case IDENTITY:
				case EXPONENTIAL:
				case RECIPROCAL:
				case SQUARE:
				case GAUSS:
				case SINE:
				case COSINE:
				case ELLIOTT:
				case ARCTAN:
				case RECTIFIER:
					break;
				default:
					throw new UnsupportedFeatureException(locatable, activationFunction);
			}

			List<Neuron> neurons = neuralLayer.getNeurons();
			for(int i = 0; i < neurons.size(); i++){
				Neuron neuron = neurons.get(i);

				Value<V> output = valueFactory.newValue();

				List<Connection> connections = neuron.getConnections();
				for(int j = 0; j < connections.size(); j++){
					Connection connection = connections.get(j);

					Value<V> input = result.get(connection.getFrom());
					if(input == null){
						throw new InvalidFeatureException(connection);
					}

					output.add(connection.getWeight(), input.getValue());
				}

				Double bias = neuron.getBias();
				if(bias != null){
					output.add(bias);
				}

				switch(activationFunction){
					case THRESHOLD:
					case LOGISTIC:
					case TANH:
					case IDENTITY:
					case EXPONENTIAL:
					case RECIPROCAL:
					case SQUARE:
					case GAUSS:
					case SINE:
					case COSINE:
					case ELLIOTT:
					case ARCTAN:
					case RECTIFIER:
						NeuralNetworkUtil.activateNeuronOutput(output, threshold, activationFunction);
						break;
					default:
						throw new UnsupportedFeatureException(locatable, activationFunction);
				}

				result.put(neuron.getId(), output);

				outputs.add(output);
			}

			locatable = neuralLayer;

			NeuralNetwork.NormalizationMethod normalizationMethod = neuralLayer.getNormalizationMethod();
			if(normalizationMethod == null){
				locatable = neuralNetwork;

				normalizationMethod = neuralNetwork.getNormalizationMethod();
			}

			switch(normalizationMethod){
				case NONE:
				case SIMPLEMAX:
				case SOFTMAX:
					NeuralNetworkUtil.normalizeNeuralLayerOutputs(outputs, normalizationMethod);
					break;
				default:
					throw new UnsupportedFeatureException(locatable, normalizationMethod);
			}
		}

		return result;
	}

	private Map<FieldName, List<NeuralOutput>> getNeuralOutputMap(){

		if(this.neuralOutputMap == null){
			this.neuralOutputMap = parseNeuralOutputs();
		}

		return this.neuralOutputMap;
	}

	private Map<FieldName, List<NeuralOutput>> parseNeuralOutputs(){
		NeuralNetwork neuralNetwork = getModel();

		NeuralOutputs neuralOutputs = neuralNetwork.getNeuralOutputs();
		if(neuralOutputs == null){
			return Collections.<FieldName, List<NeuralOutput>>emptyMap();
		}

		ListMultimap<FieldName, NeuralOutput> result = ArrayListMultimap.create();

		for(NeuralOutput neuralOutput : neuralOutputs){
			FieldName name;

			Expression expression = getOutputExpression(neuralOutput);

			if(expression instanceof FieldRef){
				FieldRef fieldRef = (FieldRef)expression;

				name = fieldRef.getField();
			} else

			if(expression instanceof NormContinuous){
				NormContinuous normContinuous = (NormContinuous)expression;

				name = normContinuous.getField();
			} else

			if(expression instanceof NormDiscrete){
				NormDiscrete normDiscrete = (NormDiscrete)expression;

				name = normDiscrete.getField();
			} else

			{
				throw new UnsupportedFeatureException(expression);
			} // End if

			if(name == null){
				throw new InvalidFeatureException(expression);
			}

			result.put(name, neuralOutput);
		}

		return (Map)result.asMap();
	}

	private static final LoadingCache<NeuralNetwork, BiMap<String, Entity>> entityCache = CacheUtil.buildLoadingCache(new CacheLoader<NeuralNetwork, BiMap<String, Entity>>(){

		@Override
		public BiMap<String, Entity> load(NeuralNetwork neuralNetwork){
			ImmutableBiMap.Builder<String, Entity> builder = new ImmutableBiMap.Builder<>();

			AtomicInteger index = new AtomicInteger(1);

			NeuralInputs neuralInputs = neuralNetwork.getNeuralInputs();
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