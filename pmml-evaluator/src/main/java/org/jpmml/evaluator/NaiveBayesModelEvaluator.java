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
package org.jpmml.evaluator;

import java.util.*;

import javax.xml.bind.*;

import org.jpmml.manager.*;
import org.jpmml.model.*;

import org.apache.commons.math3.util.*;

import org.dmg.pmml.*;

import com.google.common.cache.*;
import com.google.common.collect.*;

public class NaiveBayesModelEvaluator extends ModelEvaluator<NaiveBayesModel> {

	public NaiveBayesModelEvaluator(PMML pmml){
		this(pmml, find(pmml.getModels(), NaiveBayesModel.class));
	}

	public NaiveBayesModelEvaluator(PMML pmml, NaiveBayesModel naiveBayesModel){
		super(pmml, naiveBayesModel);
	}

	@Override
	public String getSummary(){
		return "Naive Bayes model";
	}

	@Override
	public Map<FieldName, ?> evaluate(ModelEvaluationContext context){
		NaiveBayesModel naiveBayesModel = getModel();
		if(!naiveBayesModel.isScorable()){
			throw new InvalidResultException(naiveBayesModel);
		}

		Map<FieldName, ?> predictions;

		MiningFunctionType miningFunction = naiveBayesModel.getFunctionName();
		switch(miningFunction){
			case CLASSIFICATION:
				predictions = evaluateClassification(context);
				break;
			default:
				throw new UnsupportedFeatureException(naiveBayesModel, miningFunction);
		}

		return OutputUtil.evaluate(predictions, context);
	}

	private Map<FieldName, ? extends ClassificationMap<?>> evaluateClassification(ModelEvaluationContext context){
		NaiveBayesModel naiveBayesModel = getModel();

		double threshold = naiveBayesModel.getThreshold();

		// Probability calculations use logarithmic scale for greater numerical stability
		ProbabilityClassificationMap<String> result = new ProbabilityClassificationMap<String>();

		Map<FieldName, Map<String, Double>> countsMap = getCountsMap();

		List<BayesInput> bayesInputs = getValue(NaiveBayesModelEvaluator.bayesInputCache);
		for(BayesInput bayesInput : bayesInputs){
			FieldName name = bayesInput.getFieldName();

			FieldValue value = ExpressionUtil.evaluate(name, context);

			// "Missing values are ignored"
			if(value == null){
				continue;
			}

			TargetValueStats targetValueStats = getTargetValueStats(bayesInput);
			if(targetValueStats != null){
				calculateContinuousProbabilities(value, targetValueStats, threshold, result);

				continue;
			}

			Map<String, Double> counts = countsMap.get(name);

			DerivedField derivedField = bayesInput.getDerivedField();
			if(derivedField != null){
				Expression expression = derivedField.getExpression();
				if(!(expression instanceof Discretize)){
					throw new InvalidFeatureException(derivedField);
				}

				Discretize discretize = (Discretize)expression;

				value = DiscretizationUtil.discretize(discretize, value);
				if(value == null){
					throw new EvaluationException();
				}

				value = FieldValueUtil.refine(derivedField, value);
			}

			TargetValueCounts targetValueCounts = getTargetValueCounts(bayesInput, value);
			if(targetValueCounts != null){
				calculateDiscreteProbabilities(counts, targetValueCounts, threshold, result);
			}
		}

		BayesOutput bayesOutput = naiveBayesModel.getBayesOutput();

		calculatePriorProbabilities(bayesOutput.getTargetValueCounts(), result);

		final Double max = Collections.max(result.values());

		// Convert from logarithmic scale to normal scale
		Collection<Map.Entry<String, Double>> entries = result.entrySet();
		for(Map.Entry<String, Double> entry : entries){
			entry.setValue(Math.exp(entry.getValue() - max));
		}

		result.normalizeValues();

		return TargetUtil.evaluateClassification(Collections.singletonMap(bayesOutput.getFieldName(), result), context);
	}

	private void calculateContinuousProbabilities(FieldValue value, TargetValueStats targetValueStats, double threshold, Map<String, Double> probabilities){
		double x = (value.asNumber()).doubleValue();

		for(TargetValueStat targetValueStat : targetValueStats){
			String targetValue = targetValueStat.getValue();

			ContinuousDistribution distribution = targetValueStat.getContinuousDistribution();
			if(!(distribution instanceof GaussianDistribution)){
				throw new InvalidFeatureException(targetValueStat);
			}

			GaussianDistribution gaussianDistribution = (GaussianDistribution)distribution;

			double mean = gaussianDistribution.getMean();
			double variance = gaussianDistribution.getVariance();

			double probability = Math.max(Math.exp(-Math.pow(x - mean, 2) / (2d * variance)) / Math.sqrt(2d * Math.PI * variance), threshold);

			updateSum(targetValue, Math.log(probability), probabilities);
		}
	}

	private void calculateDiscreteProbabilities(Map<String, Double> counts, TargetValueCounts targetValueCounts, double threshold, Map<String, Double> probabilities){

		for(TargetValueCount targetValueCount : targetValueCounts){
			String targetValue = targetValueCount.getValue();

			Double count = counts.get(targetValue);

			double probability = Math.max(targetValueCount.getCount() / count, threshold);

			updateSum(targetValue, Math.log(probability), probabilities);
		}
	}

	private void calculatePriorProbabilities(TargetValueCounts targetValueCounts, Map<String, Double> probabilities){

		for(TargetValueCount targetValueCount : targetValueCounts){
			String targetValue = targetValueCount.getValue();

			updateSum(targetValue, Math.log(targetValueCount.getCount()), probabilities);
		}
	}

	protected Map<FieldName, Map<String, Double>> getCountsMap(){
		return getValue(NaiveBayesModelEvaluator.countCache);
	}

	static
	private Map<FieldName, Map<String, Double>> calculateCounts(NaiveBayesModel naiveBayesModel){
		Map<FieldName, Map<String, Double>> result = Maps.newLinkedHashMap();

		List<BayesInput> bayesInputs = CacheUtil.getValue(naiveBayesModel, NaiveBayesModelEvaluator.bayesInputCache);
		for(BayesInput bayesInput : bayesInputs){
			FieldName name = bayesInput.getFieldName();

			Map<String, Double> counts = Maps.newLinkedHashMap();

			List<PairCounts> pairCounts = bayesInput.getPairCounts();
			for(PairCounts pairCount : pairCounts){
				TargetValueCounts targetValueCounts = pairCount.getTargetValueCounts();

				for(TargetValueCount targetValueCount : targetValueCounts){
					updateSum(targetValueCount.getValue(), targetValueCount.getCount(), counts);
				}
			}

			result.put(name, counts);
		}

		return result;
	}

	static
	private List<BayesInput> parseBayesInputs(NaiveBayesModel naiveBayesModel){
		List<BayesInput> result = Lists.newArrayList();

		BayesInputs bayesInputs = naiveBayesModel.getBayesInputs();

		// The TargetValueStats element is not part of the PMML standard (as of PMML 4.1).
		// Therefore, every BayesInput element that deals with TargetValueStats element has to be surrounded by an Extension element.
		// Once the TargetValueStats element is incorporated into the PMML standard then it will be no longer necessary.
		List<Extension> extensions = bayesInputs.getExtensions();
		for(Extension extension : extensions){
			BayesInput bayesInput;

			try {
				bayesInput = ExtensionUtil.getExtension(extension, BayesInput.class);
			} catch(JAXBException je){
				throw new InvalidFeatureException(extension);
			}

			if(bayesInput == null){
				continue;
			}

			result.add(bayesInput);
		}

		result.addAll(bayesInputs.getBayesInputs());

		return result;
	}

	static
	private void updateSum(String key, Double value, Map<String, Double> counts){
		Double count = counts.get(key);
		if(count == null){
			count = 0d;
		}

		counts.put(key, count + value);
	}

	static
	private TargetValueStats getTargetValueStats(BayesInput bayesInput){
		return bayesInput.getTargetValueStats();
	}

	static
	private TargetValueCounts getTargetValueCounts(BayesInput bayesInput, FieldValue value){
		List<PairCounts> pairCounts = bayesInput.getPairCounts();
		for(PairCounts pairCount : pairCounts){

			if((value).equalsString(pairCount.getValue())){
				return pairCount.getTargetValueCounts();
			}
		}

		return null;
	}

	private static final LoadingCache<NaiveBayesModel, List<BayesInput>> bayesInputCache = CacheBuilder.newBuilder()
		.weakKeys()
		.build(new CacheLoader<NaiveBayesModel, List<BayesInput>>(){

			@Override
			public List<BayesInput> load(NaiveBayesModel naiveBayesModel){
				return parseBayesInputs(naiveBayesModel);
			}
		});

	private static final LoadingCache<NaiveBayesModel, Map<FieldName, Map<String, Double>>> countCache = CacheBuilder.newBuilder()
		.weakKeys()
		.build(new CacheLoader<NaiveBayesModel, Map<FieldName, Map<String, Double>>>(){

			@Override
			public Map<FieldName, Map<String, Double>> load(NaiveBayesModel naiveBayesModel){
				return calculateCounts(naiveBayesModel);
			}
		});
}