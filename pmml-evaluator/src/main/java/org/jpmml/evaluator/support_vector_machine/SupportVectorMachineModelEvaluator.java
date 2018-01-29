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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Doubles;
import org.dmg.pmml.Array;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.MathContext;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.PMML;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.RealSparseArray;
import org.dmg.pmml.regression.CategoricalPredictor;
import org.dmg.pmml.support_vector_machine.Coefficient;
import org.dmg.pmml.support_vector_machine.Coefficients;
import org.dmg.pmml.support_vector_machine.Kernel;
import org.dmg.pmml.support_vector_machine.SupportVector;
import org.dmg.pmml.support_vector_machine.SupportVectorMachine;
import org.dmg.pmml.support_vector_machine.SupportVectorMachineModel;
import org.dmg.pmml.support_vector_machine.SupportVectors;
import org.dmg.pmml.support_vector_machine.VectorDictionary;
import org.dmg.pmml.support_vector_machine.VectorFields;
import org.dmg.pmml.support_vector_machine.VectorInstance;
import org.jpmml.evaluator.ArrayUtil;
import org.jpmml.evaluator.CacheUtil;
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.EvaluationException;
import org.jpmml.evaluator.ExpressionUtil;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.InvalidAttributeException;
import org.jpmml.evaluator.InvalidElementException;
import org.jpmml.evaluator.InvalidElementListException;
import org.jpmml.evaluator.MisplacedAttributeException;
import org.jpmml.evaluator.MisplacedElementException;
import org.jpmml.evaluator.MissingAttributeException;
import org.jpmml.evaluator.MissingElementException;
import org.jpmml.evaluator.MissingValueException;
import org.jpmml.evaluator.ModelEvaluationContext;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.OutputUtil;
import org.jpmml.evaluator.PMMLAttributes;
import org.jpmml.evaluator.PMMLElements;
import org.jpmml.evaluator.PMMLException;
import org.jpmml.evaluator.SparseArrayUtil;
import org.jpmml.evaluator.TargetUtil;
import org.jpmml.evaluator.UnsupportedAttributeException;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueMap;
import org.jpmml.evaluator.XPathUtil;
import org.jpmml.model.ReflectionUtil;

public class SupportVectorMachineModelEvaluator extends ModelEvaluator<SupportVectorMachineModel> {

	transient
	private Map<String, double[]> vectorMap = null;


	public SupportVectorMachineModelEvaluator(PMML pmml){
		this(pmml, selectModel(pmml, SupportVectorMachineModel.class));
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

		VectorDictionary vectorDictionary = supportVectorMachineModel.getVectorDictionary();
		if(vectorDictionary == null){
			throw new MissingElementException(supportVectorMachineModel, PMMLElements.SUPPORTVECTORMACHINEMODEL_VECTORDICTIONARY);
		}

		VectorFields vectorFields = vectorDictionary.getVectorFields();
		if(vectorFields == null){
			throw new MissingElementException(vectorDictionary, PMMLElements.VECTORDICTIONARY_VECTORFIELDS);
		} // End if

		if(!supportVectorMachineModel.hasSupportVectorMachines()){
			throw new MissingElementException(supportVectorMachineModel, PMMLElements.SUPPORTVECTORMACHINEMODEL_SUPPORTVECTORMACHINES);
		}
	}

	@Override
	public String getSummary(){
		return "Support vector machine";
	}

	@Override
	public Map<FieldName, ?> evaluate(ModelEvaluationContext context){
		SupportVectorMachineModel supportVectorMachineModel = ensureScorableModel();

		ValueFactory<Double> valueFactory;

		MathContext mathContext = supportVectorMachineModel.getMathContext();
		switch(mathContext){
			case DOUBLE:
				valueFactory = (ValueFactory)getValueFactory();
				break;
			default:
				throw new UnsupportedAttributeException(supportVectorMachineModel, mathContext);
		}

		Map<FieldName, ?> predictions;

		MiningFunction miningFunction = supportVectorMachineModel.getMiningFunction();
		switch(miningFunction){
			case REGRESSION:
				predictions = evaluateRegression(valueFactory, context);
				break;
			case CLASSIFICATION:
				predictions = evaluateClassification(valueFactory, context);
				break;
			case ASSOCIATION_RULES:
			case SEQUENCES:
			case CLUSTERING:
			case TIME_SERIES:
			case MIXED:
				throw new InvalidAttributeException(supportVectorMachineModel, miningFunction);
			default:
				throw new UnsupportedAttributeException(supportVectorMachineModel, miningFunction);
		}

		return OutputUtil.evaluate(predictions, context);
	}

	private Map<FieldName, ?> evaluateRegression(ValueFactory<Double> valueFactory, EvaluationContext context){
		SupportVectorMachineModel supportVectorMachineModel = getModel();

		List<SupportVectorMachine> supportVectorMachines = supportVectorMachineModel.getSupportVectorMachines();
		if(supportVectorMachines.size() != 1){
			throw new InvalidElementListException(supportVectorMachines);
		}

		SupportVectorMachine supportVectorMachine = supportVectorMachines.get(0);

		double[] input = createInput(context);

		Value<Double> result = evaluateSupportVectorMachine(valueFactory, supportVectorMachine, input);

		return TargetUtil.evaluateRegression(getTargetField(), result);
	}

	private Map<FieldName, ? extends Classification<Double>> evaluateClassification(final ValueFactory<Double> valueFactory, EvaluationContext context){
		SupportVectorMachineModel supportVectorMachineModel = getModel();

		List<SupportVectorMachine> supportVectorMachines = supportVectorMachineModel.getSupportVectorMachines();

		String alternateBinaryTargetCategory = supportVectorMachineModel.getAlternateBinaryTargetCategory();

		ValueMap<String, Double> values;

		SupportVectorMachineModel.ClassificationMethod classificationMethod = getClassificationMethod();
		switch(classificationMethod){
			case ONE_AGAINST_ALL:
				values = new ValueMap<>(2 * supportVectorMachines.size());
				break;
			case ONE_AGAINST_ONE:
				values = new VoteMap<String, Double>(2 * supportVectorMachines.size()){

					@Override
					public ValueFactory<Double> getValueFactory(){
						return valueFactory;
					}
				};
				break;
			default:
				throw new UnsupportedAttributeException(supportVectorMachineModel, classificationMethod);
		}

		double[] input = createInput(context);

		for(SupportVectorMachine supportVectorMachine : supportVectorMachines){
			String targetCategory = supportVectorMachine.getTargetCategory();
			if(targetCategory == null){
				throw new MissingAttributeException(supportVectorMachine, PMMLAttributes.SUPPORTVECTORMACHINE_TARGETCATEGORY);
			}

			String alternateTargetCategory = supportVectorMachine.getAlternateTargetCategory();

			Value<Double> value = evaluateSupportVectorMachine(valueFactory, supportVectorMachine, input);

			switch(classificationMethod){
				case ONE_AGAINST_ALL:
					{
						if(alternateTargetCategory != null){
							throw new MisplacedAttributeException(supportVectorMachine, PMMLAttributes.SUPPORTVECTORMACHINE_ALTERNATETARGETCATEGORY, alternateTargetCategory);
						}

						values.put(targetCategory, value);
					}
					break;
				case ONE_AGAINST_ONE:
					{
						String label;

						if(alternateBinaryTargetCategory != null){

							if(alternateTargetCategory != null){
								throw new MisplacedAttributeException(supportVectorMachine, PMMLAttributes.SUPPORTVECTORMACHINE_ALTERNATETARGETCATEGORY, alternateTargetCategory);
							}

							value.round();

							// "A rounded value of 1 corresponds to the targetCategory attribute of the SupportVectorMachine element"
							if(value.equals(1d)){
								label = targetCategory;
							} else

							// "A rounded value of 0 corresponds to the alternateBinaryTargetCategory attribute of the SupportVectorMachineModel element"
							if(value.equals(0d)){
								label = alternateBinaryTargetCategory;
							} else

							// "The numeric prediction must be between 0 and 1"
							{
								throw new EvaluationException("Expected " + PMMLException.formatValue(0d) + " or " + PMMLException.formatValue(1d) + ", got " + PMMLException.formatValue(value.getValue()));
							}
						} else

						{
							if(alternateTargetCategory == null){
								throw new MissingAttributeException(supportVectorMachine, PMMLAttributes.SUPPORTVECTORMACHINE_ALTERNATETARGETCATEGORY);
							}

							Double threshold = supportVectorMachine.getThreshold();
							if(threshold == null){
								threshold = supportVectorMachineModel.getThreshold();
							} // End if

							// "If the numeric prediction is smaller than the threshold, then it corresponds to the targetCategory attribute"
							if(value.compareTo(threshold) < 0){
								label = targetCategory;
							} else

							{
								label = alternateTargetCategory;
							}
						}

						VoteMap<String, Double> votes = (VoteMap<String, Double>)values;

						votes.increment(label);
					}
					break;
				default:
					break;
			}
		}

		Classification<Double> result;

		switch(classificationMethod){
			case ONE_AGAINST_ALL:
				result = new DistanceDistribution<>(values);
				break;
			case ONE_AGAINST_ONE:
				result = new VoteDistribution<>(values);
				break;
			default:
				throw new UnsupportedAttributeException(supportVectorMachineModel, classificationMethod);
		}

		return TargetUtil.evaluateClassification(getTargetField(), result);
	}

	private Value<Double> evaluateSupportVectorMachine(ValueFactory<Double> valueFactory, SupportVectorMachine supportVectorMachine, double[] input){
		SupportVectorMachineModel supportVectorMachineModel = getModel();

		Value<Double> result = valueFactory.newValue();

		Kernel kernel = supportVectorMachineModel.getKernel();
		if(kernel == null){
			throw new MissingElementException(MissingElementException.formatMessage(XPathUtil.formatElement(supportVectorMachineModel.getClass()) + "/<Kernel>"), supportVectorMachine);
		}

		Coefficients coefficients = supportVectorMachine.getCoefficients();
		Iterator<Coefficient> coefficientIt = coefficients.iterator();

		SupportVectors supportVectors = supportVectorMachine.getSupportVectors();
		Iterator<SupportVector> supportVectorIt = supportVectors.iterator();

		Map<String, double[]> vectorMap = getVectorMap();

		while(coefficientIt.hasNext() && supportVectorIt.hasNext()){
			Coefficient coefficient = coefficientIt.next();
			SupportVector supportVector = supportVectorIt.next();

			String vectorId = supportVector.getVectorId();
			if(vectorId == null){
				throw new MissingAttributeException(supportVector, PMMLAttributes.SUPPORTVECTOR_VECTORID);
			}

			double[] vector = vectorMap.get(vectorId);
			if(vector == null){
				throw new InvalidAttributeException(supportVector, PMMLAttributes.SUPPORTVECTOR_VECTORID, vectorId);
			}

			result.add(coefficient.getValue(), KernelUtil.evaluate(kernel, input, vector));
		}

		if(coefficientIt.hasNext() || supportVectorIt.hasNext()){
			throw new InvalidElementException(supportVectorMachine);
		}

		double absoluteValue = coefficients.getAbsoluteValue();
		if(absoluteValue != 0d){
			result.add(absoluteValue);
		}

		return result;
	}

	private SupportVectorMachineModel.ClassificationMethod getClassificationMethod(){
		SupportVectorMachineModel supportVectorMachineModel = getModel();

		// Older versions of several popular PMML producer software are known to omit the classificationMethod attribute.
		// The method SupportVectorMachineModel#getRepresentation() replaces a missing value with the default value "OneAgainstAll", which may lead to incorrect behaviour.
		// The workaround is to bypass this method using Java Reflection API, and infer the correct classification method type based on evidence.
		SupportVectorMachineModel.ClassificationMethod classificationMethod = ReflectionUtil.getFieldValue(PMMLAttributes.SUPPORTVECTORMACHINEMODEL_CLASSIFICATIONMETHOD, supportVectorMachineModel);
		if(classificationMethod != null){
			return classificationMethod;
		}

		List<SupportVectorMachine> supportVectorMachines = supportVectorMachineModel.getSupportVectorMachines();

		String alternateBinaryTargetCategory = supportVectorMachineModel.getAlternateBinaryTargetCategory();
		if(alternateBinaryTargetCategory != null){

			if(supportVectorMachines.size() == 1){
				SupportVectorMachine supportVectorMachine = supportVectorMachines.get(0);

				String targetCategory = supportVectorMachine.getTargetCategory();
				if(targetCategory != null){
					return SupportVectorMachineModel.ClassificationMethod.ONE_AGAINST_ONE;
				}

				throw new InvalidElementException(supportVectorMachine);
			}

			throw new InvalidElementException(supportVectorMachineModel);
		}

		for(SupportVectorMachine supportVectorMachine : supportVectorMachines){
			String targetCategory = supportVectorMachine.getTargetCategory();
			String alternateTargetCategory = supportVectorMachine.getAlternateTargetCategory();

			if(targetCategory != null){

				if(alternateTargetCategory != null){
					return SupportVectorMachineModel.ClassificationMethod.ONE_AGAINST_ONE;
				}

				return SupportVectorMachineModel.ClassificationMethod.ONE_AGAINST_ALL;
			}

			throw new InvalidElementException(supportVectorMachine);
		}

		throw new InvalidElementException(supportVectorMachineModel);
	}

	private double[] createInput(EvaluationContext context){
		SupportVectorMachineModel supportVectorMachineModel = getModel();

		VectorDictionary vectorDictionary = supportVectorMachineModel.getVectorDictionary();

		VectorFields vectorFields = vectorDictionary.getVectorFields();

		List<PMMLObject> content = vectorFields.getContent();

		double[] result = new double[content.size()];

		for(int i = 0; i < content.size(); i++){
			PMMLObject object = content.get(i);

			if(object instanceof FieldRef){
				FieldRef fieldRef = (FieldRef)content.get(i);

				FieldName name = fieldRef.getField();

				FieldValue value = ExpressionUtil.evaluate(fieldRef, context);
				if(value == null){
					throw new MissingValueException(name, vectorFields);
				}

				result[i] = (value.asNumber()).doubleValue();
			} else

			if(object instanceof CategoricalPredictor){
				CategoricalPredictor categoricalPredictor = (CategoricalPredictor)object;

				FieldName name = categoricalPredictor.getName();
				if(name == null){
					throw new MissingAttributeException(categoricalPredictor, PMMLAttributes.CATEGORICALPREDICTOR_NAME);
				}

				FieldValue value = context.evaluate(name);
				if(value == null){
					throw new MissingValueException(name, categoricalPredictor);
				}

				double coefficient = categoricalPredictor.getCoefficient();
				if(coefficient != 1d){
					throw new InvalidAttributeException(categoricalPredictor, PMMLAttributes.CATEGORICALPREDICTOR_COEFFICIENT, coefficient);
				}

				boolean equals = value.equals(categoricalPredictor);

				result[i] = (equals ? 1d : 0d);
			} else

			{
				throw new MisplacedElementException(object);
			}
		}

		return result;
	}

	private Map<String, double[]> getVectorMap(){

		if(this.vectorMap == null){
			this.vectorMap = getValue(SupportVectorMachineModelEvaluator.vectorCache);
		}

		return this.vectorMap;
	}

	static
	private Map<String, double[]> parseVectorDictionary(SupportVectorMachineModel supportVectorMachineModel){
		VectorDictionary vectorDictionary = supportVectorMachineModel.getVectorDictionary();

		VectorFields vectorFields = vectorDictionary.getVectorFields();

		List<PMMLObject> content = vectorFields.getContent();

		Map<String, double[]> result = new LinkedHashMap<>();

		List<VectorInstance> vectorInstances = vectorDictionary.getVectorInstances();
		for(VectorInstance vectorInstance : vectorInstances){
			String id = vectorInstance.getId();
			if(id == null){
				throw new MissingAttributeException(vectorInstance, PMMLAttributes.VECTORINSTANCE_ID);
			}

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

			double[] vector = Doubles.toArray(values);

			result.put(id, vector);
		}

		return result;
	}

	private static final LoadingCache<SupportVectorMachineModel, Map<String, double[]>> vectorCache = CacheUtil.buildLoadingCache(new CacheLoader<SupportVectorMachineModel, Map<String, double[]>>(){

		@Override
		public Map<String, double[]> load(SupportVectorMachineModel supportVectorMachineModel){
			return ImmutableMap.copyOf(parseVectorDictionary(supportVectorMachineModel));
		}
	});
}
