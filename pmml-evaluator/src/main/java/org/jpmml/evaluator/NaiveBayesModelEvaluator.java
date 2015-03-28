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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.math3.util.Precision;
import org.dmg.pmml.BayesInput;
import org.dmg.pmml.BayesInputs;
import org.dmg.pmml.BayesOutput;
import org.dmg.pmml.ContinuousDistribution;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Discretize;
import org.dmg.pmml.Expression;
import org.dmg.pmml.Extension;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.GaussianDistribution;
import org.dmg.pmml.MiningFunctionType;
import org.dmg.pmml.NaiveBayesModel;
import org.dmg.pmml.PMML;
import org.dmg.pmml.PairCounts;
import org.dmg.pmml.TargetValueCount;
import org.dmg.pmml.TargetValueCounts;
import org.dmg.pmml.TargetValueStat;
import org.dmg.pmml.TargetValueStats;
import org.jpmml.manager.InvalidFeatureException;
import org.jpmml.manager.UnsupportedFeatureException;
import org.jpmml.model.ExtensionUtil;

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

		Map<FieldName, ? extends ClassificationMap<?>> predictions;

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

		Map<FieldName, Map<String, Double>> fieldCountSums = getFieldCountSums();

		List<BayesInput> bayesInputs = getBayesInputs();
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

			DerivedField derivedField = bayesInput.getDerivedField();
			if(derivedField != null){
				Expression expression = derivedField.getExpression();
				if(expression == null){
					throw new InvalidFeatureException(derivedField);
				} // End if

				if(!(expression instanceof Discretize)){
					throw new UnsupportedFeatureException(expression);
				}

				Discretize discretize = (Discretize)expression;

				value = DiscretizationUtil.discretize(discretize, value);
				if(value == null){
					throw new EvaluationException();
				}

				value = FieldValueUtil.refine(derivedField, value);
			}

			Map<String, Double> countSums = fieldCountSums.get(name);

			TargetValueCounts targetValueCounts = getTargetValueCounts(bayesInput, value);
			if(targetValueCounts != null){
				calculateDiscreteProbabilities(countSums, targetValueCounts, threshold, result);
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

		FieldName targetField = bayesOutput.getFieldName();
		if(targetField == null){
			throw new InvalidFeatureException(bayesOutput);
		}

		return TargetUtil.evaluateClassification(Collections.singletonMap(targetField, result), context);
	}

	private void calculateContinuousProbabilities(FieldValue value, TargetValueStats targetValueStats, double threshold, Map<String, Double> probabilities){
		double x = (value.asNumber()).doubleValue();

		for(TargetValueStat targetValueStat : targetValueStats){
			String targetValue = targetValueStat.getValue();

			ContinuousDistribution distribution = targetValueStat.getContinuousDistribution();
			if(distribution == null){
				throw new InvalidFeatureException(targetValueStat);
			} // End if

			if(!(distribution instanceof GaussianDistribution)){
				throw new UnsupportedFeatureException(distribution);
			}

			GaussianDistribution gaussianDistribution = (GaussianDistribution)distribution;

			double mean = gaussianDistribution.getMean();
			double variance = gaussianDistribution.getVariance();

			double probability = NormalDistributionUtil.probability(mean, Math.sqrt(variance), x);

			// The calculated probability cannot fall below the default probability
			probability = Math.max(probability, threshold);

			updateSum(targetValue, Math.log(probability), probabilities);
		}
	}

	private void calculateDiscreteProbabilities(Map<String, Double> countSums, TargetValueCounts targetValueCounts, double threshold, Map<String, Double> probabilities){

		for(TargetValueCount targetValueCount : targetValueCounts){
			String targetValue = targetValueCount.getValue();

			Double countSum = countSums.get(targetValue);

			double probability = targetValueCount.getCount() / countSum;

			// The calculated probability can fall below the default probability
			// However, a count of zero represents a special case, which needs adjustment
			if(VerificationUtil.isZero(targetValueCount.getCount(), Precision.EPSILON)){
				probability = threshold;
			}

			updateSum(targetValue, Math.log(probability), probabilities);
		}
	}

	private void calculatePriorProbabilities(TargetValueCounts targetValueCounts, Map<String, Double> probabilities){

		for(TargetValueCount targetValueCount : targetValueCounts){
			String targetValue = targetValueCount.getValue();

			updateSum(targetValue, Math.log(targetValueCount.getCount()), probabilities);
		}
	}

	protected List<BayesInput> getBayesInputs(){
		return getValue(NaiveBayesModelEvaluator.bayesInputCache);
	}

	protected Map<FieldName, Map<String, Double>> getFieldCountSums(){
		return getValue(NaiveBayesModelEvaluator.fieldCountSumCache);
	}

	static
	private Map<FieldName, Map<String, Double>> calculateFieldCountSums(NaiveBayesModel naiveBayesModel){
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

			if(value.equalsString(pairCount.getValue())){
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
				return ImmutableList.copyOf(parseBayesInputs(naiveBayesModel));
			}
		});

	private static final LoadingCache<NaiveBayesModel, Map<FieldName, Map<String, Double>>> fieldCountSumCache = CacheBuilder.newBuilder()
		.weakKeys()
		.build(new CacheLoader<NaiveBayesModel, Map<FieldName, Map<String, Double>>>(){

			@Override
			public Map<FieldName, Map<String, Double>> load(NaiveBayesModel naiveBayesModel){
				return ImmutableMap.copyOf(calculateFieldCountSums(naiveBayesModel));
			}
		});
}