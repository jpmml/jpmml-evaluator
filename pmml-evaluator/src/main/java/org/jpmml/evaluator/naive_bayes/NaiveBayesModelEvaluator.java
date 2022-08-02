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
package org.jpmml.evaluator.naive_bayes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.math3.util.Precision;
import org.dmg.pmml.ContinuousDistribution;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Discretize;
import org.dmg.pmml.Expression;
import org.dmg.pmml.GaussianDistribution;
import org.dmg.pmml.PMML;
import org.dmg.pmml.PoissonDistribution;
import org.dmg.pmml.naive_bayes.BayesInput;
import org.dmg.pmml.naive_bayes.BayesInputs;
import org.dmg.pmml.naive_bayes.BayesOutput;
import org.dmg.pmml.naive_bayes.NaiveBayesModel;
import org.dmg.pmml.naive_bayes.PMMLAttributes;
import org.dmg.pmml.naive_bayes.PairCounts;
import org.dmg.pmml.naive_bayes.TargetValueCount;
import org.dmg.pmml.naive_bayes.TargetValueCounts;
import org.dmg.pmml.naive_bayes.TargetValueStat;
import org.dmg.pmml.naive_bayes.TargetValueStats;
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.DiscretizationUtil;
import org.jpmml.evaluator.DistributionUtil;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.FieldValues;
import org.jpmml.evaluator.Functions;
import org.jpmml.evaluator.MapHolder;
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
import org.jpmml.model.InvalidAttributeException;
import org.jpmml.model.MisplacedElementException;

public class NaiveBayesModelEvaluator extends ModelEvaluator<NaiveBayesModel> {

	private Map<String, Map<Object, Number>> fieldCountSums = Collections.emptyMap();


	private NaiveBayesModelEvaluator(){
	}

	public NaiveBayesModelEvaluator(PMML pmml){
		this(pmml, PMMLUtil.findModel(pmml, NaiveBayesModel.class));
	}

	public NaiveBayesModelEvaluator(PMML pmml, NaiveBayesModel naiveBayesModel){
		super(pmml, naiveBayesModel);

		List<BayesInput> bayesInputs = (naiveBayesModel.requireBayesInputs()).requireBayesInputs();

		this.fieldCountSums = ImmutableMap.copyOf(toImmutableMapMap(calculateFieldCountSums(bayesInputs)));

		BayesOutput bayesOutput = naiveBayesModel.requireBayesOutput();

		@SuppressWarnings("unused")
		List<TargetValueCount> targetValueCounts = (bayesOutput.requireTargetValueCounts()).requireTargetValueCounts();
	}

	@Override
	public String getSummary(){
		return "Naive Bayes model";
	}

	@Override
	protected <V extends Number> Map<String, ? extends Classification<?, V>> evaluateClassification(ValueFactory<V> valueFactory, EvaluationContext context){
		NaiveBayesModel naiveBayesModel = getModel();

		BayesInputs bayesInputs = naiveBayesModel.requireBayesInputs();
		BayesOutput bayesOutput = naiveBayesModel.requireBayesOutput();

		TargetField targetField = getTargetField();

		String targetFieldName = bayesOutput.getTargetField();
		if(targetFieldName != null && !Objects.equals(targetField.getFieldName(), targetFieldName)){
			throw new InvalidAttributeException(bayesOutput, PMMLAttributes.BAYESOUTPUT_TARGETFIELD, targetFieldName);
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
			TargetValueCounts targetValueCounts = bayesOutput.requireTargetValueCounts();

			calculatePriorProbabilities(probabilities, targetValueCounts);
		}

		Number threshold = naiveBayesModel.requireThreshold();

		Map<String, ? extends Map<?, Number>> fieldCountSums = getFieldCountSums();

		for(BayesInput bayesInput : bayesInputs){
			String fieldName = bayesInput.requireField();

			FieldValue value = context.evaluate(fieldName);

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

			Map<?, Number> countSums = fieldCountSums.get(fieldName);

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
		Expression expression = derivedField.requireExpression();

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
			Object targetCategory = targetValueStat.requireValue();
			ContinuousDistribution distribution = targetValueStat.requireContinuousDistribution();

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
			Object targetCategory = targetValueCount.requireValue();
			Number count = targetValueCount.requireCount();

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
			Object targetCategory = targetValueCount.requireValue();
			Number count = targetValueCount.requireCount();

			Number probability = count;

			probabilities.multiply(targetCategory, probability);
		}
	}

	protected Map<String, Map<Object, Number>> getFieldCountSums(){
		return this.fieldCountSums;
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
		for(int i = 0, max = pairCounts.size(); i < max; i++){
			PairCounts pairCount = pairCounts.get(i);

			Object category = pairCount.requireValue();

			if(value.equalsValue(category)){
				TargetValueCounts targetValueCounts = pairCount.requireTargetValueCounts();

				return targetValueCounts;
			}
		}

		return null;
	}

	static
	private Map<String, Map<Object, Number>> calculateFieldCountSums(List<BayesInput> bayesInputs){
		Map<String, Map<Object, Number>> result = new LinkedHashMap<>();

		for(BayesInput bayesInput : bayesInputs){
			Map<Object, Number> countSums = new LinkedHashMap<>();

			List<PairCounts> pairCounts = bayesInput.getPairCounts();
			for(PairCounts pairCount : pairCounts){
				TargetValueCounts targetValueCounts = pairCount.getTargetValueCounts();

				for(TargetValueCount targetValueCount : targetValueCounts){
					Object targetCategory = targetValueCount.requireValue();
					Number count = targetValueCount.requireCount();

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

			result.put(bayesInput.requireField(), countSums);
		}

		return result;
	}
}