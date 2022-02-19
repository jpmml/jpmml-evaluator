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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Expression;
import org.dmg.pmml.Field;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.HasFieldReference;
import org.dmg.pmml.NormContinuous;
import org.dmg.pmml.NormDiscrete;
import org.dmg.pmml.PMML;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.Visitor;
import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.neural_network.Connection;
import org.dmg.pmml.neural_network.NeuralEntity;
import org.dmg.pmml.neural_network.NeuralInput;
import org.dmg.pmml.neural_network.NeuralInputs;
import org.dmg.pmml.neural_network.NeuralLayer;
import org.dmg.pmml.neural_network.NeuralNetwork;
import org.dmg.pmml.neural_network.NeuralOutput;
import org.dmg.pmml.neural_network.NeuralOutputs;
import org.dmg.pmml.neural_network.Neuron;
import org.dmg.pmml.neural_network.PMMLAttributes;
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.EntityUtil;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.ExpressionUtil;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.HasEntityRegistry;
import org.jpmml.evaluator.MissingFieldException;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.NormalizationUtil;
import org.jpmml.evaluator.Numbers;
import org.jpmml.evaluator.PMMLUtil;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TargetUtil;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueMap;
import org.jpmml.model.InvalidAttributeException;
import org.jpmml.model.InvalidElementException;
import org.jpmml.model.InvalidElementListException;
import org.jpmml.model.MisplacedElementException;
import org.jpmml.model.MissingAttributeException;
import org.jpmml.model.UnsupportedAttributeException;
import org.jpmml.model.visitors.AbstractVisitor;

public class NeuralNetworkEvaluator extends ModelEvaluator<NeuralNetwork> implements HasEntityRegistry<NeuralEntity> {

	private BiMap<String, NeuralEntity> entityRegistry = ImmutableBiMap.of();

	private Map<String, List<NeuralOutput>> neuralOutputMap = null;


	private NeuralNetworkEvaluator(){
	}

	public NeuralNetworkEvaluator(PMML pmml){
		this(pmml, PMMLUtil.findModel(pmml, NeuralNetwork.class));
	}

	public NeuralNetworkEvaluator(PMML pmml, NeuralNetwork neuralNetwork){
		super(pmml, neuralNetwork);

		@SuppressWarnings("unused")
		List<NeuralInput> neuralInputs = (neuralNetwork.requireNeuralInputs()).requireNeuralInputs();

		@SuppressWarnings("unused")
		List<NeuralLayer> neuralLayers = neuralNetwork.requireNeuralLayers();

		List<NeuralEntity> neuralEntities = collectNeuralEntities(neuralNetwork);

		this.entityRegistry = ImmutableBiMap.copyOf(EntityUtil.buildBiMap(neuralEntities));

		@SuppressWarnings("unused")
		List<NeuralOutput> neuralOutputs = (neuralNetwork.requireNeuralOutputs()).requireNeuralOutputs();
	}

	@Override
	public String getSummary(){
		return "Neural network";
	}

	@Override
	public BiMap<String, NeuralEntity> getEntityRegistry(){
		return this.entityRegistry;
	}

	@Override
	protected <V extends Number> Map<String, ?> evaluateRegression(ValueFactory<V> valueFactory, EvaluationContext context){
		NeuralNetwork neuralNetwork = getModel();

		List<TargetField> targetFields = getTargetFields();

		ValueMap<String, V> values = evaluateRaw(valueFactory, context);
		if(values == null){

			if(targetFields.size() == 1){
				TargetField targetField = targetFields.get(0);

				return TargetUtil.evaluateRegressionDefault(valueFactory, targetField);
			}

			Map<String, Object> results = new LinkedHashMap<>();

			for(TargetField targetField : targetFields){
				results.putAll(TargetUtil.evaluateRegressionDefault(valueFactory, targetField));
			}

			return results;
		}

		Map<String, List<NeuralOutput>> neuralOutputMap = getNeuralOutputMap();

		Map<String, Object> results = null;

		for(TargetField targetField : targetFields){
			String name = targetField.getFieldName();

			List<NeuralOutput> neuralOutputs = neuralOutputMap.get(name);
			if(neuralOutputs == null){
				throw new InvalidElementException(neuralNetwork);
			} else

			if(neuralOutputs.size() != 1){
				throw new InvalidElementListException(neuralOutputs);
			}

			NeuralOutput neuralOutput = neuralOutputs.get(0);

			String id = neuralOutput.requireOutputNeuron();

			Value<V> value = values.get(id);
			if(value == null){
				throw new InvalidAttributeException(neuralOutput, PMMLAttributes.NEURALOUTPUT_OUTPUTNEURON, id);
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
				throw new MisplacedElementException(expression);
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

	@Override
	protected <V extends Number> Map<String, ? extends Classification<?, V>> evaluateClassification(ValueFactory<V> valueFactory, EvaluationContext context){
		NeuralNetwork neuralNetwork = getModel();

		List<TargetField> targetFields = getTargetFields();

		ValueMap<String, V> values = evaluateRaw(valueFactory, context);
		if(values == null){

			if(targetFields.size() == 1){
				TargetField targetField = targetFields.get(0);

				return TargetUtil.evaluateClassificationDefault(valueFactory, targetField);
			}

			Map<String, Classification<?, V>> results = new LinkedHashMap<>();

			for(TargetField targetField : targetFields){
				results.putAll(TargetUtil.evaluateClassificationDefault(valueFactory, targetField));
			}

			return results;
		}

		Map<String, List<NeuralOutput>> neuralOutputMap = getNeuralOutputMap();

		BiMap<String, NeuralEntity> entityRegistry = getEntityRegistry();

		Map<String, Classification<?, V>> results = null;

		for(TargetField targetField : targetFields){
			String name = targetField.getFieldName();

			List<NeuralOutput> neuralOutputs = neuralOutputMap.get(name);
			if(neuralOutputs == null){
				throw new InvalidElementException(neuralNetwork);
			}

			NeuronProbabilityDistribution<V> result = new NeuronProbabilityDistribution<V>(new ValueMap<Object, V>(2 * neuralOutputs.size())){

				@Override
				public BiMap<String, NeuralEntity> getEntityRegistry(){
					return entityRegistry;
				}
			};

			for(NeuralOutput neuralOutput : neuralOutputs){
				String id = neuralOutput.requireOutputNeuron();

				NeuralEntity entity = entityRegistry.get(id);
				if(entity == null){
					throw new InvalidAttributeException(neuralOutput, PMMLAttributes.NEURALOUTPUT_OUTPUTNEURON, id);
				}

				Value<V> value = values.get(id);
				if(value == null){
					throw new InvalidAttributeException(neuralOutput, PMMLAttributes.NEURALOUTPUT_OUTPUTNEURON, id);
				}

				Expression expression = getOutputExpression(neuralOutput);

				if(expression instanceof NormDiscrete){
					NormDiscrete normDiscrete = (NormDiscrete)expression;

					Object targetCategory = normDiscrete.requireValue();

					result.put(entity, targetCategory, value);
				} else

				{
					throw new MisplacedElementException(expression);
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
		DerivedField derivedField = neuralOutput.requireDerivedField();

		Expression expression = derivedField.requireExpression();

		if(expression instanceof FieldRef){
			FieldRef fieldRef = (FieldRef)expression;

			String fieldName = fieldRef.requireField();

			Field<?> field = resolveField(fieldName);
			if(field == null){
				throw new MissingFieldException(fieldRef);
			} // End if

			if(field instanceof DataField){
				return expression;
			} else

			if(field instanceof DerivedField){
				DerivedField targetDerivedField = (DerivedField)field;

				return targetDerivedField.requireExpression();
			} else

			{
				throw new InvalidAttributeException(fieldRef, org.dmg.pmml.PMMLAttributes.FIELDREF_FIELD, fieldName);
			}
		}

		return expression;
	}

	private <V extends Number> ValueMap<String, V> evaluateRaw(ValueFactory<V> valueFactory, EvaluationContext context){
		NeuralNetwork neuralNetwork = getModel();

		BiMap<String, NeuralEntity> entityRegistry = getEntityRegistry();

		ValueMap<String, V> result = new ValueMap<>(2 * entityRegistry.size());

		NeuralInputs neuralInputs = neuralNetwork.requireNeuralInputs();
		for(NeuralInput neuralInput : neuralInputs){
			DerivedField derivedField = neuralInput.requireDerivedField();

			FieldValue value = ExpressionUtil.evaluateTypedExpressionContainer(derivedField, context);
			if(FieldValueUtil.isMissing(value)){
				return null;
			}

			Value<V> output = valueFactory.newValue(value.asNumber());

			result.put(neuralInput.requireId(), output);
		}

		List<Value<V>> outputs = new ArrayList<>();

		List<NeuralLayer> neuralLayers = neuralNetwork.requireNeuralLayers();
		for(NeuralLayer neuralLayer : neuralLayers){
			outputs.clear();

			PMMLObject locatable = neuralLayer;

			NeuralNetwork.ActivationFunction activationFunction = neuralLayer.getActivationFunction();
			if(activationFunction == null){
				locatable = neuralNetwork;

				activationFunction = neuralNetwork.requireActivationFunction();
			}

			Number threshold = null;
			Number leakage = null;

			Number altitude = null;
			Number width = null;

			switch(activationFunction){
				// Group 1 activation functions
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
					threshold = neuralLayer.getThreshold();
					if(threshold == null){
						threshold = neuralNetwork.getThreshold();
					}

					leakage = neuralLayer.getLeakage();
					if(leakage == null){
						leakage = neuralNetwork.getLeakage();
					}
					break;
				// Group 2 activation functions
				case RADIAL_BASIS:
					altitude = neuralLayer.getAltitude();
					if(altitude == null){
						altitude = neuralNetwork.getAltitude();
					}

					width = neuralLayer.getWidth();
					if(width == null){
						width = neuralNetwork.getWidth();
					}
					break;
				default:
					throw new UnsupportedAttributeException(locatable, activationFunction);
			}

			List<Neuron> neurons = neuralLayer.getNeurons();
			for(int i = 0, max = neurons.size(); i < max; i++){
				Neuron neuron = neurons.get(i);

				Value<V> output = valueFactory.newValue();

				List<Connection> connections = neuron.requireConnections();
				for(int j = 0; j < connections.size(); j++){
					Connection connection = connections.get(j);

					String id = connection.requireFrom();
					Number weight = connection.requireWeight();

					Value<V> input = result.get(id);
					if(input == null){
						throw new InvalidAttributeException(connection, PMMLAttributes.CONNECTION_FROM, id);
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
							output.add(weight, input.getValue());
							break;
						case RADIAL_BASIS:
							input = input.copy();

							output.add((input.subtract(weight)).square());
							break;
						default:
							throw new UnsupportedAttributeException(locatable, activationFunction);
					}
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
						{
							Number neuronBias = neuron.getBias();
							if(neuronBias != null){
								output.add(neuronBias);
							}

							NeuralNetworkUtil.activateNeuronOutput(activationFunction, threshold, leakage, output);
						}
						break;
					case RADIAL_BASIS:
						{
							Number neuronWidth = neuron.getWidth();
							if(neuronWidth == null){

								if(width == null){
									throw new MissingAttributeException(neuralNetwork, PMMLAttributes.NEURALNETWORK_WIDTH);
								}

								neuronWidth = width;
							}

							Value<V> denominator = valueFactory.newValue(neuronWidth)
								.square()
								.multiply(Numbers.DOUBLE_MINUS_TWO);

							output.divide(denominator);

							if(altitude.doubleValue() != 1d){
								Value<V> value = valueFactory.newValue(altitude)
									.ln()
									.multiply(connections.size());

								output.add(value);
							}

							output.exp();
						}
						break;
					default:
						throw new UnsupportedAttributeException(locatable, activationFunction);
				}

				result.put(neuron.requireId(), output);

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
					NeuralNetworkUtil.normalizeNeuralLayerOutputs(normalizationMethod, outputs);
					break;
				default:
					throw new UnsupportedAttributeException(locatable, normalizationMethod);
			}
		}

		return result;
	}

	private Map<String, List<NeuralOutput>> getNeuralOutputMap(){

		if(this.neuralOutputMap == null){
			this.neuralOutputMap = ImmutableMap.copyOf(toImmutableListMap(parseNeuralOutputs()));
		}

		return this.neuralOutputMap;
	}

	private Map<String, List<NeuralOutput>> parseNeuralOutputs(){
		NeuralNetwork neuralNetwork = getModel();

		ListMultimap<String, NeuralOutput> result = ArrayListMultimap.create();

		NeuralOutputs neuralOutputs = neuralNetwork.requireNeuralOutputs();
		for(NeuralOutput neuralOutput : neuralOutputs){
			Expression expression = getOutputExpression(neuralOutput);

			if(expression instanceof HasFieldReference){
				HasFieldReference<?> hasFieldReference = (HasFieldReference<?>)expression;

				result.put(hasFieldReference.requireField(), neuralOutput);
			} else

			{
				throw new MisplacedElementException(expression);
			}
		}

		return new LinkedHashMap<>(Multimaps.asMap(result));
	}

	static
	private List<NeuralEntity> collectNeuralEntities(NeuralNetwork neuralNetwork){
		List<NeuralEntity> result = new ArrayList<>();

		Visitor visitor = new AbstractVisitor(){

			@Override
			public VisitorAction visit(NeuralEntity neuralEntity){
				result.add(neuralEntity);

				return super.visit(neuralEntity);
			}
		};
		visitor.applyTo(neuralNetwork);

		return result;
	}
}