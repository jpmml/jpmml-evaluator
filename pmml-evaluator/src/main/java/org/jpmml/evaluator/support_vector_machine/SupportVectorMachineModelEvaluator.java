/*
 * Copyright (c) 2013 KNIME.com AG, Zurich, Switzerland
 * Copyright (c) 2014 Villu Ruusmann
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import org.dmg.pmml.Array;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.MathContext;
import org.dmg.pmml.PMML;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.RealSparseArray;
import org.dmg.pmml.regression.CategoricalPredictor;
import org.dmg.pmml.support_vector_machine.Coefficient;
import org.dmg.pmml.support_vector_machine.Coefficients;
import org.dmg.pmml.support_vector_machine.Kernel;
import org.dmg.pmml.support_vector_machine.PMMLAttributes;
import org.dmg.pmml.support_vector_machine.SupportVector;
import org.dmg.pmml.support_vector_machine.SupportVectorMachine;
import org.dmg.pmml.support_vector_machine.SupportVectorMachineModel;
import org.dmg.pmml.support_vector_machine.SupportVectors;
import org.dmg.pmml.support_vector_machine.VectorDictionary;
import org.dmg.pmml.support_vector_machine.VectorFields;
import org.dmg.pmml.support_vector_machine.VectorInstance;
import org.jpmml.evaluator.ArrayUtil;
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.EvaluationException;
import org.jpmml.evaluator.ExpressionUtil;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.MissingFieldValueException;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.Numbers;
import org.jpmml.evaluator.PMMLUtil;
import org.jpmml.evaluator.SparseArrayUtil;
import org.jpmml.evaluator.TargetUtil;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueMap;
import org.jpmml.model.InvalidAttributeException;
import org.jpmml.model.InvalidElementException;
import org.jpmml.model.InvalidElementListException;
import org.jpmml.model.MisplacedElementException;
import org.jpmml.model.UnsupportedAttributeException;

public class SupportVectorMachineModelEvaluator extends ModelEvaluator<SupportVectorMachineModel> {

	private Map<String, Object> vectorMap = Collections.emptyMap();


	private SupportVectorMachineModelEvaluator(){
	}

	public SupportVectorMachineModelEvaluator(PMML pmml){
		this(pmml, PMMLUtil.findModel(pmml, SupportVectorMachineModel.class));
	}

	public SupportVectorMachineModelEvaluator(PMML pmml, SupportVectorMachineModel supportVectorMachineModel){
		super(pmml, supportVectorMachineModel);

		boolean maxWins = supportVectorMachineModel.isMaxWins();
		if(maxWins){
			throw new UnsupportedAttributeException(supportVectorMachineModel, PMMLAttributes.SUPPORTVECTORMACHINEMODEL_MAXWINS, maxWins);
		}

		SupportVectorMachineModel.Representation representation = supportVectorMachineModel.getRepresentation();
		switch(representation){
			case SUPPORT_VECTORS:
				break;
			default:
				throw new UnsupportedAttributeException(supportVectorMachineModel, representation);
		}

		@SuppressWarnings("unused")
		List<SupportVectorMachine> supportVectorMachines = supportVectorMachineModel.requireSupportVectorMachines();

		this.vectorMap = ImmutableMap.copyOf(parseVectorDictionary(supportVectorMachineModel));
	}

	@Override
	public String getSummary(){
		return "Support vector machine";
	}

	@Override
	protected <V extends Number> Map<String, ?> evaluateRegression(ValueFactory<V> valueFactory, EvaluationContext context){
		SupportVectorMachineModel supportVectorMachineModel = getModel();

		List<SupportVectorMachine> supportVectorMachines = supportVectorMachineModel.requireSupportVectorMachines();
		if(supportVectorMachines.size() != 1){
			throw new InvalidElementListException(supportVectorMachines);
		}

		SupportVectorMachine supportVectorMachine = supportVectorMachines.get(0);

		Object input = createInput(context);

		Value<V> result = evaluateSupportVectorMachine(valueFactory, supportVectorMachine, input);

		return TargetUtil.evaluateRegression(getTargetField(), result);
	}

	@Override
	protected <V extends Number> Map<String, ? extends Classification<?, V>> evaluateClassification(ValueFactory<V> valueFactory, EvaluationContext context){
		SupportVectorMachineModel supportVectorMachineModel = getModel();

		Object alternateBinaryTargetCategory = supportVectorMachineModel.getAlternateBinaryTargetCategory();

		List<SupportVectorMachine> supportVectorMachines = supportVectorMachineModel.requireSupportVectorMachines();

		ValueMap<Object, V> values;

		SupportVectorMachineModel.ClassificationMethod classificationMethod = supportVectorMachineModel.getClassificationMethod();
		switch(classificationMethod){
			case ONE_AGAINST_ALL:
				values = new ValueMap<>(2 * supportVectorMachines.size());
				break;
			case ONE_AGAINST_ONE:
				values = new VoteMap<Object, V>(2 * supportVectorMachines.size()){

					@Override
					public ValueFactory<V> getValueFactory(){
						return valueFactory;
					}
				};
				break;
			default:
				throw new UnsupportedAttributeException(supportVectorMachineModel, classificationMethod);
		}

		Object input = createInput(context);

		for(SupportVectorMachine supportVectorMachine : supportVectorMachines){
			Value<V> value = evaluateSupportVectorMachine(valueFactory, supportVectorMachine, input);

			switch(classificationMethod){
				case ONE_AGAINST_ALL:
					{
						Object targetCategory = supportVectorMachine.requireTargetCategory();

						values.put(targetCategory, value);
					}
					break;
				case ONE_AGAINST_ONE:
					{
						Object targetCategory;

						if(alternateBinaryTargetCategory != null){
							value.round();

							// "A rounded value of 0 corresponds to the alternateBinaryTargetCategory attribute of the SupportVectorMachineModel element"
							if(value.isZero()){
								targetCategory = alternateBinaryTargetCategory;
							} else

							// "A rounded value of 1 corresponds to the targetCategory attribute of the SupportVectorMachine element"
							if(value.isOne()){
								targetCategory = supportVectorMachine.requireTargetCategory();
							} else

							// "The numeric prediction must be between 0 and 1"
							{
								throw new EvaluationException("Expected " + EvaluationException.formatValue(Numbers.DOUBLE_ZERO) + " or " + EvaluationException.formatValue(Numbers.DOUBLE_ONE) + ", got " + EvaluationException.formatValue(value.getValue()));
							}
						} else

						{
							Number threshold = supportVectorMachine.getThreshold();
							if(threshold == null){
								threshold = supportVectorMachineModel.getThreshold();
							} // End if

							// "If the numeric prediction is smaller than the threshold, then it corresponds to the targetCategory attribute"
							if(value.compareTo(threshold) < 0){
								targetCategory = supportVectorMachine.requireTargetCategory();
							} else

							{
								targetCategory = supportVectorMachine.requireAlternateTargetCategory();
							}
						}

						VoteMap<Object, V> votes = (VoteMap<Object, V>)values;

						votes.increment(targetCategory);
					}
					break;
				default:
					break;
			}
		}

		Classification<?, V> result;

		switch(classificationMethod){
			case ONE_AGAINST_ALL:
				result = new DistanceDistribution<>(values);
				break;
			case ONE_AGAINST_ONE:
				result = new VoteProbabilityDistribution<>(values);
				break;
			default:
				throw new UnsupportedAttributeException(supportVectorMachineModel, classificationMethod);
		}

		return TargetUtil.evaluateClassification(getTargetField(), result);
	}

	private <V extends Number> Value<V> evaluateSupportVectorMachine(ValueFactory<V> valueFactory, SupportVectorMachine supportVectorMachine, Object input){
		SupportVectorMachineModel supportVectorMachineModel = getModel();

		Value<V> result = valueFactory.newValue();

		Kernel kernel = supportVectorMachineModel.requireKernel();

		Coefficients coefficients = supportVectorMachine.getCoefficients();
		Iterator<Coefficient> coefficientIt = coefficients.iterator();

		SupportVectors supportVectors = supportVectorMachine.getSupportVectors();
		Iterator<SupportVector> supportVectorIt = supportVectors.iterator();

		Map<String, Object> vectorMap = getVectorMap();

		while(coefficientIt.hasNext() && supportVectorIt.hasNext()){
			Coefficient coefficient = coefficientIt.next();
			SupportVector supportVector = supportVectorIt.next();

			String vectorId = supportVector.requireVectorId();

			Object vector = vectorMap.get(vectorId);
			if(vector == null){
				throw new InvalidAttributeException(supportVector, PMMLAttributes.SUPPORTVECTOR_VECTORID, vectorId);
			}

			Value<V> value = KernelUtil.evaluate(kernel, valueFactory, input, vector);

			result.add(coefficient.getValue(), value.getValue());
		}

		if(coefficientIt.hasNext() || supportVectorIt.hasNext()){
			throw new InvalidElementException(supportVectorMachine);
		}

		result.add(coefficients.getAbsoluteValue());

		return result;
	}

	private Object createInput(EvaluationContext context){
		SupportVectorMachineModel supportVectorMachineModel = getModel();

		VectorDictionary vectorDictionary = supportVectorMachineModel.requireVectorDictionary();

		VectorFields vectorFields = vectorDictionary.requireVectorFields();

		List<PMMLObject> content = vectorFields.getContent();

		List<Number> result = new ArrayList<>(content.size());

		for(int i = 0, max = content.size(); i < max; i++){
			PMMLObject object = content.get(i);

			if(object instanceof FieldRef){
				FieldRef fieldRef = (FieldRef)object;

				FieldValue value = ExpressionUtil.evaluate(fieldRef, context);
				if(FieldValueUtil.isMissing(value)){
					throw new MissingFieldValueException(fieldRef);
				}

				result.add(value.asNumber());
			} else

			if(object instanceof CategoricalPredictor){
				CategoricalPredictor categoricalPredictor = (CategoricalPredictor)object;

				FieldValue value = context.evaluate(categoricalPredictor.requireField());
				if(FieldValueUtil.isMissing(value)){
					throw new MissingFieldValueException(categoricalPredictor);
				}

				Number coefficient = categoricalPredictor.getCoefficient();
				if(coefficient != null && coefficient.doubleValue() != 1d){
					throw new InvalidAttributeException(categoricalPredictor, org.dmg.pmml.regression.PMMLAttributes.CATEGORICALPREDICTOR_COEFFICIENT, coefficient);
				}

				boolean equals = value.equals(categoricalPredictor);

				result.add(equals ? Numbers.DOUBLE_ONE : Numbers.DOUBLE_ZERO);
			} else

			{
				throw new MisplacedElementException(object);
			}
		}

		return toArray(supportVectorMachineModel, result);
	}

	private Map<String, Object> getVectorMap(){
		return this.vectorMap;
	}

	static
	private Map<String, Object> parseVectorDictionary(SupportVectorMachineModel supportVectorMachineModel){
		VectorDictionary vectorDictionary = supportVectorMachineModel.requireVectorDictionary();

		VectorFields vectorFields = vectorDictionary.requireVectorFields();

		List<PMMLObject> content = vectorFields.getContent();

		Map<String, Object> result = new LinkedHashMap<>();

		List<VectorInstance> vectorInstances = vectorDictionary.getVectorInstances();
		for(VectorInstance vectorInstance : vectorInstances){
			String id = vectorInstance.requireId();

			Array array = vectorInstance.getArray();
			RealSparseArray sparseArray = vectorInstance.getRealSparseArray();

			List<? extends Number> values;

			if(array != null && sparseArray == null){
				values = ArrayUtil.asNumberList(array);
			} else

			if(array == null && sparseArray != null){
				values = SparseArrayUtil.asNumberList(sparseArray);
			} else

			{
				throw new InvalidElementException(vectorInstance);
			} // End if

			if(content.size() != values.size()){
				throw new InvalidElementException(vectorInstance);
			}

			Object vector = toArray(supportVectorMachineModel, values);

			result.put(id, vector);
		}

		return result;
	}

	static
	private Object toArray(SupportVectorMachineModel supportVectorMachineModel, List<? extends Number> values){
		MathContext mathContext = supportVectorMachineModel.getMathContext();

		switch(mathContext){
			case FLOAT:
				return Floats.toArray(values);
			case DOUBLE:
				return Doubles.toArray(values);
			default:
				throw new UnsupportedAttributeException(supportVectorMachineModel, mathContext);
		}
	}
}
