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

import java.lang.reflect.Field;
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
import org.jpmml.evaluator.InvalidFeatureException;
import org.jpmml.evaluator.InvalidResultException;
import org.jpmml.evaluator.MissingValueException;
import org.jpmml.evaluator.ModelEvaluationContext;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.OutputUtil;
import org.jpmml.evaluator.SparseArrayUtil;
import org.jpmml.evaluator.TargetUtil;
import org.jpmml.evaluator.UnsupportedFeatureException;
import org.jpmml.evaluator.VoteDistribution;
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
			throw new UnsupportedFeatureException(supportVectorMachineModel);
		}

		SupportVectorMachineModel.Representation representation = supportVectorMachineModel.getRepresentation();
		switch(representation){
			case SUPPORT_VECTORS:
				break;
			default:
				throw new UnsupportedFeatureException(supportVectorMachineModel, representation);
		}

		VectorDictionary vectorDictionary = supportVectorMachineModel.getVectorDictionary();
		if(vectorDictionary == null){
			throw new InvalidFeatureException(supportVectorMachineModel);
		}

		VectorFields vectorFields = vectorDictionary.getVectorFields();
		if(vectorFields == null){
			throw new InvalidFeatureException(vectorDictionary);
		} // End if

		if(!supportVectorMachineModel.hasSupportVectorMachines()){
			throw new InvalidFeatureException(supportVectorMachineModel);
		}
	}

	@Override
	public String getSummary(){
		return "Support vector machine";
	}

	@Override
	public Map<FieldName, ?> evaluate(ModelEvaluationContext context){
		SupportVectorMachineModel supportVectorMachineModel = getModel();
		if(!supportVectorMachineModel.isScorable()){
			throw new InvalidResultException(supportVectorMachineModel);
		}

		MathContext mathContext = supportVectorMachineModel.getMathContext();
		switch(mathContext){
			case DOUBLE:
				break;
			default:
				throw new UnsupportedFeatureException(supportVectorMachineModel, mathContext);
		}

		Map<FieldName, ?> predictions;

		MiningFunction miningFunction = supportVectorMachineModel.getMiningFunction();
		switch(miningFunction){
			case REGRESSION:
				predictions = evaluateRegression(context);
				break;
			case CLASSIFICATION:
				predictions = evaluateClassification(context);
				break;
			default:
				throw new UnsupportedFeatureException(supportVectorMachineModel, miningFunction);
		}

		return OutputUtil.evaluate(predictions, context);
	}

	private Map<FieldName, ?> evaluateRegression(EvaluationContext context){
		SupportVectorMachineModel supportVectorMachineModel = getModel();

		List<SupportVectorMachine> supportVectorMachines = supportVectorMachineModel.getSupportVectorMachines();
		if(supportVectorMachines.size() != 1){
			throw new InvalidFeatureException(supportVectorMachineModel);
		}

		SupportVectorMachine supportVectorMachine = supportVectorMachines.get(0);

		double[] input = createInput(context);

		Double result = evaluateSupportVectorMachine(supportVectorMachine, input);

		return TargetUtil.evaluateRegression(getTargetField(), result);
	}

	private Map<FieldName, ? extends Classification> evaluateClassification(EvaluationContext context){
		SupportVectorMachineModel supportVectorMachineModel = getModel();

		List<SupportVectorMachine> supportVectorMachines = supportVectorMachineModel.getSupportVectorMachines();

		String alternateBinaryTargetCategory = supportVectorMachineModel.getAlternateBinaryTargetCategory();

		Classification result;

		SupportVectorMachineModel.ClassificationMethod classificationMethod = getClassificationMethod();
		switch(classificationMethod){
			case ONE_AGAINST_ALL:
				result = new Classification(Classification.Type.DISTANCE);
				break;
			case ONE_AGAINST_ONE:
				result = new VoteDistribution();
				break;
			default:
				throw new UnsupportedFeatureException(supportVectorMachineModel, classificationMethod);
		}

		double[] input = createInput(context);

		for(SupportVectorMachine supportVectorMachine : supportVectorMachines){
			String targetCategory = supportVectorMachine.getTargetCategory();
			String alternateTargetCategory = supportVectorMachine.getAlternateTargetCategory();

			Double value = evaluateSupportVectorMachine(supportVectorMachine, input);

			switch(classificationMethod){
				case ONE_AGAINST_ALL:
					{
						if(targetCategory == null || alternateTargetCategory != null){
							throw new InvalidFeatureException(supportVectorMachine);
						}

						result.put(targetCategory, value);
					}
					break;
				case ONE_AGAINST_ONE:
					if(alternateBinaryTargetCategory != null){

						if(targetCategory == null || alternateTargetCategory != null){
							throw new InvalidFeatureException(supportVectorMachine);
						}

						String label;

						long roundedValue = Math.round(value);

						// "A rounded value of 1 corresponds to the targetCategory attribute of the SupportVectorMachine element"
						if(roundedValue == 1){
							label = targetCategory;
						} else

						// "A rounded value of 0 corresponds to the alternateBinaryTargetCategory attribute of the SupportVectorMachineModel element"
						if(roundedValue == 0){
							label = alternateBinaryTargetCategory;
						} else

						// "The numeric prediction must be between 0 and 1"
						{
							throw new EvaluationException("Invalid numeric prediction " + value);
						}

						Double vote = result.get(label);
						if(vote == null){
							vote = 0d;
						}

						result.put(label, (vote + 1d));
					} else

					{
						if(targetCategory == null || alternateTargetCategory == null){
							throw new InvalidFeatureException(supportVectorMachine);
						}

						Double threshold = supportVectorMachine.getThreshold();
						if(threshold == null){
							threshold = supportVectorMachineModel.getThreshold();
						}

						String label;

						// "If the numeric prediction is smaller than the threshold, then it corresponds to the targetCategory attribute"
						if((value).compareTo(threshold) < 0){
							label = targetCategory;
						} else

						{
							label = alternateTargetCategory;
						}

						Double vote = result.get(label);
						if(vote == null){
							vote = 0d;
						}

						result.put(label, (vote + 1d));
					}
					break;
				default:
					break;
			}
		}

		return TargetUtil.evaluateClassification(getTargetField(), result);
	}

	private double evaluateSupportVectorMachine(SupportVectorMachine supportVectorMachine, double[] input){
		SupportVectorMachineModel supportVectorMachineModel = getModel();

		double result = 0d;

		Kernel kernel = supportVectorMachineModel.getKernel();

		Coefficients coefficients = supportVectorMachine.getCoefficients();
		Iterator<Coefficient> coefficientIt = coefficients.iterator();

		SupportVectors supportVectors = supportVectorMachine.getSupportVectors();
		Iterator<SupportVector> supportVectorIt = supportVectors.iterator();

		Map<String, double[]> vectorMap = getVectorMap();

		while(coefficientIt.hasNext() && supportVectorIt.hasNext()){
			Coefficient coefficient = coefficientIt.next();
			SupportVector supportVector = supportVectorIt.next();

			double[] vector = vectorMap.get(supportVector.getVectorId());
			if(vector == null){
				throw new InvalidFeatureException(supportVector);
			}

			result += (coefficient.getValue() * KernelUtil.evaluate(kernel, input, vector));
		}

		if(coefficientIt.hasNext() || supportVectorIt.hasNext()){
			throw new InvalidFeatureException(supportVectorMachine);
		}

		result += coefficients.getAbsoluteValue();

		return result;
	}

	private SupportVectorMachineModel.ClassificationMethod getClassificationMethod(){
		SupportVectorMachineModel supportVectorMachineModel = getModel();

		// Older versions of several popular PMML producer software are known to omit the classificationMethod attribute.
		// The method SupportVectorMachineModel#getRepresentation() replaces a missing value with the default value "OneAgainstAll", which may lead to incorrect behaviour.
		// The workaround is to bypass this method using Java Reflection API, and infer the correct classification method type based on evidence.
		Field field = ReflectionUtil.getField(SupportVectorMachineModel.class, "classificationMethod");

		SupportVectorMachineModel.ClassificationMethod classificationMethod = ReflectionUtil.getFieldValue(field, supportVectorMachineModel);
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

				throw new InvalidFeatureException(supportVectorMachine);
			}

			throw new InvalidFeatureException(supportVectorMachineModel);
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

			throw new InvalidFeatureException(supportVectorMachine);
		}

		throw new InvalidFeatureException(supportVectorMachineModel);
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

				double coefficient = categoricalPredictor.getCoefficient();
				if(coefficient != 1d){
					throw new InvalidFeatureException(categoricalPredictor);
				}

				FieldName name = categoricalPredictor.getName();

				FieldValue value = context.evaluate(name);
				if(value == null){
					throw new MissingValueException(name, categoricalPredictor);
				}

				boolean equals = value.equals(categoricalPredictor);

				result[i] = (equals ? 1d : 0d);
			} else

			{
				throw new UnsupportedFeatureException(object);
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
				throw new InvalidFeatureException(vectorInstance);
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
				throw new InvalidFeatureException(vectorInstance);
			} // End if

			if(content.size() != values.size()){
				throw new InvalidFeatureException(vectorInstance);
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