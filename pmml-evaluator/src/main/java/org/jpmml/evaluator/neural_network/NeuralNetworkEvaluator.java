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
import org.dmg.pmml.FieldName;
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
import org.dmg.pmml.neural_network.PMMLElements;
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.EntityUtil;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.ExpressionUtil;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.HasEntityRegistry;
import org.jpmml.evaluator.InvalidAttributeException;
import org.jpmml.evaluator.InvalidElementException;
import org.jpmml.evaluator.InvalidElementListException;
import org.jpmml.evaluator.MisplacedElementException;
import org.jpmml.evaluator.MissingAttributeException;
import org.jpmml.evaluator.MissingElementException;
import org.jpmml.evaluator.MissingFieldException;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.NormalizationUtil;
import org.jpmml.evaluator.Numbers;
import org.jpmml.evaluator.PMMLUtil;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TargetUtil;
import org.jpmml.evaluator.UnsupportedAttributeException;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueMap;
import org.jpmml.model.XPathUtil;
import org.jpmml.model.visitors.AbstractVisitor;

public class NeuralNetworkEvaluator extends ModelEvaluator<NeuralNetwork> implements HasEntityRegistry<NeuralEntity> {

	private BiMap<String, NeuralEntity> entityRegistry = ImmutableBiMap.of();

	private Map<FieldName, List<NeuralOutput>> neuralOutputMap = null;


	private NeuralNetworkEvaluator(){
	}

	public NeuralNetworkEvaluator(PMML pmml){
		this(pmml, PMMLUtil.findModel(pmml, NeuralNetwork.class));
	}

	public NeuralNetworkEvaluator(PMML pmml, NeuralNetwork neuralNetwork){
		super(pmml, neuralNetwork);

		NeuralInputs neuralInputs = neuralNetwork.getNeuralInputs();
		if(neuralInputs == null){
			throw new MissingElementException(neuralNetwork, PMMLElements.NEURALNETWORK_NEURALINPUTS);
		} // End if

		if(!neuralInputs.hasNeuralInputs()){
			throw new MissingElementException(neuralInputs, PMMLElements.NEURALINPUTS_NEURALINPUTS);
		} // End if

		if(!neuralNetwork.hasNeuralLayers()){
			throw new MissingElementException(neuralNetwork, PMMLElements.NEURALNETWORK_NEURALLAYERS);
		} // End if

		{
			List<NeuralEntity> neuralEntities = collectNeuralEntities(neuralNetwork);

			this.entityRegistry = ImmutableBiMap.copyOf(EntityUtil.buildBiMap(neuralEntities));
		}

		NeuralOutputs neuralOutputs = neuralNetwork.getNeuralOutputs();
		if(neuralOutputs == null){
			throw new MissingElementException(neuralNetwork, PMMLElements.NEURALNETWORK_NEURALOUTPUTS);
		} // End if

		if(!neuralOutputs.hasNeuralOutputs()){
			throw new MissingElementException(neuralOutputs, PMMLElements.NEURALOUTPUTS_NEURALOUTPUTS);
		}
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
	protected <V extends Number> Map<FieldName, ?> evaluateRegression(ValueFactory<V> valueFactory, EvaluationContext context){
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
			FieldName name = targetField.getFieldName();

			List<NeuralOutput> neuralOutputs = neuralOutputMap.get(name);
			if(neuralOutputs == null){
				throw new InvalidElementException(neuralNetwork);
			} else

			if(neuralOutputs.size() != 1){
				throw new InvalidElementListException(neuralOutputs);
			}

			NeuralOutput neuralOutput = neuralOutputs.get(0);

			String id = neuralOutput.getOutputNeuron();
			if(id == null){
				throw new MissingAttributeException(neuralOutput, PMMLAttributes.NEURALOUTPUT_OUTPUTNEURON);
			}

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
	protected <V extends Number> Map<FieldName, ? extends Classification<?, V>> evaluateClassification(ValueFactory<V> valueFactory, EvaluationContext context){
		NeuralNetwork neuralNetwork = getModel();

		List<TargetField> targetFields = getTargetFields();

		ValueMap<String, V> values = evaluateRaw(valueFactory, context);
		if(values == null){

			if(targetFields.size() == 1){
				TargetField targetField = targetFields.get(0);

				return TargetUtil.evaluateClassificationDefault(valueFactory, targetField);
			}

			Map<FieldName, Classification<?, V>> results = new LinkedHashMap<>();

			for(TargetField targetField : targetFields){
				results.putAll(TargetUtil.evaluateClassificationDefault(valueFactory, targetField));
			}

			return results;
		}

		Map<FieldName, List<NeuralOutput>> neuralOutputMap = getNeuralOutputMap();

		BiMap<String, NeuralEntity> entityRegistry = getEntityRegistry();

		Map<FieldName, Classification<?, V>> results = null;

		for(TargetField targetField : targetFields){
			FieldName name = targetField.getFieldName();

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
				String id = neuralOutput.getOutputNeuron();
				if(id == null){
					throw new MissingAttributeException(neuralOutput, PMMLAttributes.NEURALOUTPUT_OUTPUTNEURON);
				}

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

					Object targetCategory = normDiscrete.getValue();
					if(targetCategory == null){
						throw new MissingAttributeException(normDiscrete, org.dmg.pmml.PMMLAttributes.NORMDISCRETE_VALUE);
					}

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
		DerivedField derivedField = neuralOutput.getDerivedField();
		if(derivedField == null){
			throw new MissingElementException(neuralOutput, PMMLElements.NEURALOUTPUT_DERIVEDFIELD);
		}

		Expression expression = ExpressionUtil.ensureExpression(derivedField);

		if(expression instanceof FieldRef){
			FieldRef fieldRef = (FieldRef)expression;

			FieldName name = fieldRef.getField();
			if(name == null){
				throw new MissingAttributeException(fieldRef, org.dmg.pmml.PMMLAttributes.FIELDREF_FIELD);
			}

			Field<?> field = resolveField(name);
			if(field == null){
				throw new MissingFieldException(name, fieldRef);
			} // End if

			if(field instanceof DataField){
				return expression;
			} else

			if(field instanceof DerivedField){
				DerivedField targetDerivedField = (DerivedField)field;

				Expression targetExpression = ExpressionUtil.ensureExpression(targetDerivedField);

				return targetExpression;
			} else

			{
				throw new InvalidAttributeException(fieldRef, org.dmg.pmml.PMMLAttributes.FIELDREF_FIELD, name);
			}
		}

		return expression;
	}

	private <V extends Number> ValueMap<String, V> evaluateRaw(ValueFactory<V> valueFactory, EvaluationContext context){
		NeuralNetwork neuralNetwork = getModel();

		BiMap<String, NeuralEntity> entityRegistry = getEntityRegistry();

		ValueMap<String, V> result = new ValueMap<>(2 * entityRegistry.size());

		NeuralInputs neuralInputs = neuralNetwork.getNeuralInputs();
		for(NeuralInput neuralInput : neuralInputs){
			DerivedField derivedField = neuralInput.getDerivedField();
			if(derivedField == null){
				throw new MissingElementException(neuralInput, PMMLElements.NEURALINPUT_DERIVEDFIELD);
			}

			FieldValue value = ExpressionUtil.evaluateTypedExpressionContainer(derivedField, context);
			if(FieldValueUtil.isMissing(value)){
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
				throw new MissingAttributeException(neuralNetwork, PMMLAttributes.NEURALNETWORK_ACTIVATIONFUNCTION);
			}

			Number threshold = neuralLayer.getThreshold();
			if(threshold == null){
				threshold = neuralNetwork.getThreshold();
			}

			Number leakage = neuralLayer.getLeakage();
			if(leakage == null){
				leakage = neuralNetwork.getLeakage();
			}

			Number altitude = neuralLayer.getAltitude();
			if(altitude == null){
				altitude = neuralNetwork.getAltitude();
			}

			Number width = neuralLayer.getWidth();
			if(width == null){
				width = neuralNetwork.getWidth();
			}

			switch(activationFunction){
				case THRESHOLD:
					if(threshold == null){
						throw new MissingAttributeException(neuralNetwork, PMMLAttributes.NEURALNETWORK_THRESHOLD);
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
				case RADIAL_BASIS:
					break;
				default:
					throw new UnsupportedAttributeException(locatable, activationFunction);
			}

			List<Neuron> neurons = neuralLayer.getNeurons();
			for(int i = 0; i < neurons.size(); i++){
				Neuron neuron = neurons.get(i);

				Value<V> output = valueFactory.newValue();

				List<Connection> connections = neuron.getConnections();
				for(int j = 0; j < connections.size(); j++){
					Connection connection = connections.get(j);

					String id = connection.getFrom();
					if(id == null){
						throw new MissingAttributeException(connection, PMMLAttributes.CONNECTION_FROM);
					}

					Number weight = connection.getWeight();
					if(weight == null){
						throw new MissingAttributeException(connection, PMMLAttributes.CONNECTION_WEIGHT);
					}

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
					NeuralNetworkUtil.normalizeNeuralLayerOutputs(normalizationMethod, outputs);
					break;
				default:
					throw new UnsupportedAttributeException(locatable, normalizationMethod);
			}
		}

		return result;
	}

	private Map<FieldName, List<NeuralOutput>> getNeuralOutputMap(){

		if(this.neuralOutputMap == null){
			this.neuralOutputMap = ImmutableMap.copyOf(toImmutableListMap(parseNeuralOutputs()));
		}

		return this.neuralOutputMap;
	}

	private Map<FieldName, List<NeuralOutput>> parseNeuralOutputs(){
		NeuralNetwork neuralNetwork = getModel();

		ListMultimap<FieldName, NeuralOutput> result = ArrayListMultimap.create();

		NeuralOutputs neuralOutputs = neuralNetwork.getNeuralOutputs();
		for(NeuralOutput neuralOutput : neuralOutputs){
			FieldName name;

			Expression expression = getOutputExpression(neuralOutput);

			if(expression instanceof HasFieldReference){
				HasFieldReference<?> hasFieldReference = (HasFieldReference<?>)expression;

				name = hasFieldReference.getField();
				if(name == null){
					throw new MissingAttributeException(MissingAttributeException.formatMessage(XPathUtil.formatElement((Class)hasFieldReference.getClass()) + "@field"), expression);
				}
			} else

			{
				throw new MisplacedElementException(expression);
			}

			result.put(name, neuralOutput);
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