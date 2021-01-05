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
package org.jpmml.evaluator.naive_bayes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.math3.util.Precision;
import org.dmg.pmml.ContinuousDistribution;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Discretize;
import org.dmg.pmml.Expression;
import org.dmg.pmml.Extension;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.GaussianDistribution;
import org.dmg.pmml.PMML;
import org.dmg.pmml.PoissonDistribution;
import org.dmg.pmml.naive_bayes.BayesInput;
import org.dmg.pmml.naive_bayes.BayesInputs;
import org.dmg.pmml.naive_bayes.BayesOutput;
import org.dmg.pmml.naive_bayes.NaiveBayesModel;
import org.dmg.pmml.naive_bayes.PMMLAttributes;
import org.dmg.pmml.naive_bayes.PMMLElements;
import org.dmg.pmml.naive_bayes.PairCounts;
import org.dmg.pmml.naive_bayes.TargetValueCount;
import org.dmg.pmml.naive_bayes.TargetValueCounts;
import org.dmg.pmml.naive_bayes.TargetValueStat;
import org.dmg.pmml.naive_bayes.TargetValueStats;
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.DiscretizationUtil;
import org.jpmml.evaluator.DistributionUtil;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.ExpressionUtil;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.FieldValues;
import org.jpmml.evaluator.Functions;
import org.jpmml.evaluator.InvalidAttributeException;
import org.jpmml.evaluator.MapHolder;
import org.jpmml.evaluator.MisplacedElementException;
import org.jpmml.evaluator.MissingAttributeException;
import org.jpmml.evaluator.MissingElementException;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.NumberUtil;
import org.jpmml.evaluator.PMMLUtil;
import org.jpmml.evaluator.ProbabilityDistribution;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TargetUtil;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueUtil;
import org.jpmml.evaluator.VerificationUtil;
import org.jpmml.model.XPathUtil;

public class NaiveBayesModelEvaluator extends ModelEvaluator<NaiveBayesModel> {

	private List<BayesInput> bayesInputs = Collections.emptyList();

	private Map<FieldName, Map<Object, Number>> fieldCountSums = Collections.emptyMap();


	private NaiveBayesModelEvaluator(){
	}

	public NaiveBayesModelEvaluator(PMML pmml){
		this(pmml, PMMLUtil.findModel(pmml, NaiveBayesModel.class));
	}

	public NaiveBayesModelEvaluator(PMML pmml, NaiveBayesModel naiveBayesModel){
		super(pmml, naiveBayesModel);

		BayesInputs bayesInputs = naiveBayesModel.getBayesInputs();
		if(bayesInputs == null){
			throw new MissingElementException(naiveBayesModel, PMMLElements.NAIVEBAYESMODEL_BAYESINPUTS);
		} // End if

		if(!bayesInputs.hasBayesInputs() && !bayesInputs.hasExtensions()){
			throw new MissingElementException(bayesInputs, PMMLElements.BAYESINPUTS_BAYESINPUTS);
		} else

		{
			this.bayesInputs = ImmutableList.copyOf(parseBayesInputs(bayesInputs));

			this.fieldCountSums = ImmutableMap.copyOf(toImmutableMapMap(calculateFieldCountSums(this.bayesInputs)));
		}

		BayesOutput bayesOutput = naiveBayesModel.getBayesOutput();
		if(bayesOutput == null){
			throw new MissingElementException(naiveBayesModel, PMMLElements.NAIVEBAYESMODEL_BAYESOUTPUT);
		}

		TargetValueCounts targetValueCounts = bayesOutput.getTargetValueCounts();
		if(targetValueCounts == null){
			throw new MissingElementException(bayesOutput, PMMLElements.BAYESOUTPUT_TARGETVALUECOUNTS);
		} // End if

		if(!targetValueCounts.hasTargetValueCounts()){
			throw new MissingElementException(targetValueCounts, PMMLElements.TARGETVALUECOUNTS_TARGETVALUECOUNTS);
		}
	}

	@Override
	public String getSummary(){
		return "Naive Bayes model";
	}

	@Override
	protected <V extends Number> Map<FieldName, ? extends Classification<?, V>> evaluateClassification(ValueFactory<V> valueFactory, EvaluationContext context){
		NaiveBayesModel naiveBayesModel = getModel();

		BayesOutput bayesOutput = naiveBayesModel.getBayesOutput();

		TargetField targetField = getTargetField();

		FieldName targetName = bayesOutput.getField();
		if(targetName == null){
			throw new MissingAttributeException(bayesOutput, PMMLAttributes.BAYESOUTPUT_FIELD);
		} // End if

		if(targetName != null && !Objects.equals(targetField.getFieldName(), targetName)){
			throw new InvalidAttributeException(bayesOutput, PMMLAttributes.BAYESOUTPUT_FIELD, targetName);
		}

		// Probability calculations use logarithmic scale for greater numerical stability
		ProbabilityMap<Object, V> probabilities = new ProbabilityMap<Object, V>(){

			@Override
			public ValueFactory<V> getValueFactory(){
				return valueFactory;
			}

			@Override
			public void multiply(Object key, Number probability){
				ValueFactory<V> valueFactory = getValueFactory();

				Value<V> value = ensureValue(key);

				Value<V> probabilityValue = valueFactory.newValue(probability)
					.ln();

				value.add(probabilityValue);
			}
		};

		{
			TargetValueCounts targetValueCounts = getTargetValueCounts(bayesOutput);

			calculatePriorProbabilities(probabilities, targetValueCounts);
		}

		Number threshold = naiveBayesModel.getThreshold();
		if(threshold == null){
			throw new MissingAttributeException(naiveBayesModel, PMMLAttributes.NAIVEBAYESMODEL_THRESHOLD);
		}

		Map<FieldName, ? extends Map<?, Number>> fieldCountSums = getFieldCountSums();

		List<BayesInput> bayesInputs = getBayesInputs();
		for(BayesInput bayesInput : bayesInputs){
			FieldName name = bayesInput.getField();
			if(name == null){
				throw new MissingAttributeException(bayesInput, PMMLAttributes.BAYESINPUT_FIELD);
			}

			FieldValue value = context.evaluate(name);

			// "Missing values are ignored"
			if(FieldValueUtil.isMissing(value)){
				continue;
			}

			TargetValueStats targetValueStats = getTargetValueStats(bayesInput);
			if(targetValueStats != null){
				calculateContinuousProbabilities(probabilities, targetValueStats, threshold, value);

				continue;
			}

			DerivedField derivedField = bayesInput.getDerivedField();
			if(derivedField != null){
				value = discretize(derivedField, value);

				if(FieldValueUtil.isMissing(value)){
					continue;
				}
			}

			Map<?, Number> countSums = fieldCountSums.get(name);

			TargetValueCounts targetValueCounts = getTargetValueCounts(bayesInput, value);
			if(targetValueCounts != null){
				calculateDiscreteProbabilities(probabilities, targetValueCounts, threshold, countSums);
			}
		}

		// Convert from logarithmic scale to normal scale
		ValueUtil.normalizeSoftMax(probabilities);

		ProbabilityDistribution<V> result = new ProbabilityDistribution<>(probabilities);

		return TargetUtil.evaluateClassification(targetField, result);
	}

	private FieldValue discretize(DerivedField derivedField, FieldValue value){
		Expression expression = ExpressionUtil.ensureExpression(derivedField);

		if(expression instanceof Discretize){
			Discretize discretize = (Discretize)expression;

			value = DiscretizationUtil.discretize(discretize, value);
			if(FieldValueUtil.isMissing(value)){
				return FieldValues.MISSING_VALUE;
			}

			return value.cast(derivedField);
		} else

		{
			throw new MisplacedElementException(expression);
		}
	}

	private void calculateContinuousProbabilities(ProbabilityMap<Object, ?> probabilities, TargetValueStats targetValueStats, Number threshold, FieldValue value){
		Number x = value.asNumber();

		for(TargetValueStat targetValueStat : targetValueStats){
			Object targetCategory = targetValueStat.getValue();
			if(targetCategory == null){
				throw new MissingAttributeException(targetValueStat, PMMLAttributes.TARGETVALUESTAT_VALUE);
			}

			ContinuousDistribution distribution = targetValueStat.getContinuousDistribution();
			if(distribution == null){
				throw new MissingElementException(MissingElementException.formatMessage(XPathUtil.formatElement(targetValueStat.getClass()) + "/<ContinuousDistribution>"), targetValueStat);
			} // End if

			Number probability;

			// "For Naive Bayes models, continuous distribution types are restricted to Gaussian and Poisson distributions"
			if((distribution instanceof GaussianDistribution) || (distribution instanceof PoissonDistribution)){
				probability = DistributionUtil.probability(distribution, x);
			} else

			{
				throw new MisplacedElementException(distribution);
			}

			// The calculated probability cannot fall below the default probability
			if(NumberUtil.compare(probability, threshold) < 0){
				probability = threshold;
			}

			probabilities.multiply(targetCategory, probability);
		}
	}

	private void calculateDiscreteProbabilities(ProbabilityMap<Object, ?> probabilities, TargetValueCounts targetValueCounts, Number threshold, Map<?, Number> countSums){

		for(TargetValueCount targetValueCount : targetValueCounts){
			Object targetCategory = targetValueCount.getValue();
			if(targetCategory == null){
				throw new MissingAttributeException(targetValueCount, PMMLAttributes.TARGETVALUECOUNT_VALUE);
			}

			Number count = targetValueCount.getCount();
			if(count == null){
				throw new MissingAttributeException(targetValueCount, PMMLAttributes.TARGETVALUECOUNT_COUNT);
			}

			Number probability;

			// The calculated probability can fall below the default probability
			// However, a count of zero represents a special case, which needs adjustment
			if(VerificationUtil.isZero(count, Precision.EPSILON)){
				probability = threshold;
			} else

			{
				Number countSum = countSums.get(targetCategory);

				probability = Functions.DIVIDE.evaluate(count, NumberUtil.asDouble(countSum));
			}

			probabilities.multiply(targetCategory, probability);
		}
	}

	private void calculatePriorProbabilities(ProbabilityMap<Object, ?> probabilities, TargetValueCounts targetValueCounts){

		for(TargetValueCount targetValueCount : targetValueCounts){
			Object targetCategory = targetValueCount.getValue();
			if(targetCategory == null){
				throw new MissingAttributeException(targetValueCount, PMMLAttributes.TARGETVALUECOUNT_VALUE);
			}

			Number count = targetValueCount.getCount();
			if(count == null){
				throw new MissingAttributeException(targetValueCount, PMMLAttributes.TARGETVALUECOUNT_COUNT);
			}

			Number probability = count;

			probabilities.multiply(targetCategory, probability);
		}
	}

	protected List<BayesInput> getBayesInputs(){
		return this.bayesInputs;
	}

	protected Map<FieldName, Map<Object, Number>> getFieldCountSums(){
		return this.fieldCountSums;
	}

	static
	private Map<FieldName, Map<Object, Number>> calculateFieldCountSums(List<BayesInput> bayesInputs){
		Map<FieldName, Map<Object, Number>> result = new LinkedHashMap<>();

		for(BayesInput bayesInput : bayesInputs){
			FieldName name = bayesInput.getField();

			Map<Object, Number> countSums = new LinkedHashMap<>();

			List<PairCounts> pairCounts = bayesInput.getPairCounts();
			for(PairCounts pairCount : pairCounts){
				TargetValueCounts targetValueCounts = pairCount.getTargetValueCounts();

				for(TargetValueCount targetValueCount : targetValueCounts){
					Object targetCategory = targetValueCount.getValue();
					if(targetCategory == null){
						throw new MissingAttributeException(targetValueCount, PMMLAttributes.TARGETVALUECOUNT_VALUE);
					}

					Number count = targetValueCount.getCount();
					if(count == null){
						throw new MissingAttributeException(targetValueCount, PMMLAttributes.TARGETVALUECOUNT_COUNT);
					}

					Number countSum = countSums.get(targetCategory);
					if(countSum == null){
						countSum = count;
					} else

					{
						countSum = Functions.ADD.evaluate(countSum, count);
					}

					countSums.put(targetCategory, countSum);
				}
			}

			result.put(name, countSums);
		}

		return result;
	}

	static
	private List<BayesInput> parseBayesInputs(BayesInputs bayesInputs){

		if(!bayesInputs.hasExtensions()){
			return bayesInputs.getBayesInputs();
		}

		List<BayesInput> result = new ArrayList<>(bayesInputs.getBayesInputs());

		// The support for continuous fields using the TargetValueStats element was officially introduced in PMML schema version 4.2.
		// However, it is possible to encounter this feature in older PMML schema version documents (most notably, produced by R's "pmml" package),
		// where the offending BayesInput element is surrounded by an Extension element:
		// <BayesInputs>
		//   <BayesInput>
		//     <PairCounts/>
		//   </BayesInput>
		//   <Extension>
		//     <BayesInput>
		//       <TargetValueStats/>
		//     </BayesInput>
		//   </Extension>
		// </BayesInputs>
		List<Extension> extensions = bayesInputs.getExtensions();
		for(Extension extension : extensions){
			List<?> objects = extension.getContent();

			for(Object object : objects){

				if(object instanceof BayesInput){
					BayesInput bayesInput = (BayesInput)object;

					result.add(bayesInput);
				}
			}
		}

		return result;
	}

	static
	private TargetValueStats getTargetValueStats(BayesInput bayesInput){
		return bayesInput.getTargetValueStats();
	}

	static
	private TargetValueCounts getTargetValueCounts(BayesInput bayesInput, FieldValue value){

		if(bayesInput instanceof MapHolder){
			MapHolder<?> mapHolder = (MapHolder<?>)bayesInput;

			return (TargetValueCounts)mapHolder.get(value.getDataType(), value.getValue());
		}

		List<PairCounts> pairCounts = bayesInput.getPairCounts();
		for(PairCounts pairCount : pairCounts){
			Object category = pairCount.getValue();
			if(category == null){
				throw new MissingAttributeException(pairCount, PMMLAttributes.PAIRCOUNTS_VALUE);
			} // End if

			if(value.equalsValue(category)){
				TargetValueCounts targetValueCounts = pairCount.getTargetValueCounts();
				if(targetValueCounts == null){
					throw new MissingElementException(pairCount, PMMLElements.PAIRCOUNTS_TARGETVALUECOUNTS);
				}

				return targetValueCounts;
			}
		}

		return null;
	}

	static
	private TargetValueCounts getTargetValueCounts(BayesOutput bayesOutput){
		return bayesOutput.getTargetValueCounts();
	}
}