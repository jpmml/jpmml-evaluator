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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
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
import org.dmg.pmml.MathContext;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.PMML;
import org.dmg.pmml.PoissonDistribution;
import org.dmg.pmml.naive_bayes.BayesInput;
import org.dmg.pmml.naive_bayes.BayesInputs;
import org.dmg.pmml.naive_bayes.BayesOutput;
import org.dmg.pmml.naive_bayes.NaiveBayesModel;
import org.dmg.pmml.naive_bayes.PairCounts;
import org.dmg.pmml.naive_bayes.TargetValueCount;
import org.dmg.pmml.naive_bayes.TargetValueCounts;
import org.dmg.pmml.naive_bayes.TargetValueStat;
import org.dmg.pmml.naive_bayes.TargetValueStats;
import org.jpmml.evaluator.CacheUtil;
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.DiscretizationUtil;
import org.jpmml.evaluator.DistributionUtil;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.ExpressionUtil;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.FieldValues;
import org.jpmml.evaluator.HasParsedValueMapping;
import org.jpmml.evaluator.InvalidAttributeException;
import org.jpmml.evaluator.MisplacedElementException;
import org.jpmml.evaluator.MissingAttributeException;
import org.jpmml.evaluator.MissingElementException;
import org.jpmml.evaluator.ModelEvaluationContext;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.OutputUtil;
import org.jpmml.evaluator.PMMLAttributes;
import org.jpmml.evaluator.PMMLElements;
import org.jpmml.evaluator.ProbabilityDistribution;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TargetUtil;
import org.jpmml.evaluator.UnsupportedAttributeException;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueUtil;
import org.jpmml.evaluator.VerificationUtil;
import org.jpmml.evaluator.XPathUtil;

public class NaiveBayesModelEvaluator extends ModelEvaluator<NaiveBayesModel> {

	transient
	private List<BayesInput> bayesInputs = null;

	transient
	private Map<FieldName, Map<String, Double>> fieldCountSums = null;


	public NaiveBayesModelEvaluator(PMML pmml){
		this(pmml, selectModel(pmml, NaiveBayesModel.class));
	}

	public NaiveBayesModelEvaluator(PMML pmml, NaiveBayesModel naiveBayesModel){
		super(pmml, naiveBayesModel);

		BayesInputs bayesInputs = naiveBayesModel.getBayesInputs();
		if(bayesInputs == null){
			throw new MissingElementException(naiveBayesModel, PMMLElements.NAIVEBAYESMODEL_BAYESINPUTS);
		} // End if

		if(!bayesInputs.hasBayesInputs() && !bayesInputs.hasExtensions()){
			throw new MissingElementException(bayesInputs, PMMLElements.BAYESINPUTS_BAYESINPUTS);
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
	public Map<FieldName, ?> evaluate(ModelEvaluationContext context){
		NaiveBayesModel naiveBayesModel = ensureScorableModel();

		ValueFactory<Double> valueFactory;

		MathContext mathContext = naiveBayesModel.getMathContext();
		switch(mathContext){
			case DOUBLE:
				valueFactory = (ValueFactory)ensureValueFactory();
				break;
			default:
				throw new UnsupportedAttributeException(naiveBayesModel, mathContext);
		}

		Map<FieldName, ? extends Classification<Double>> predictions;

		MiningFunction miningFunction = naiveBayesModel.getMiningFunction();
		switch(miningFunction){
			case CLASSIFICATION:
				predictions = evaluateClassification(valueFactory, context);
				break;
			case ASSOCIATION_RULES:
			case SEQUENCES:
			case REGRESSION:
			case CLUSTERING:
			case TIME_SERIES:
			case MIXED:
				throw new InvalidAttributeException(naiveBayesModel, miningFunction);
			default:
				throw new UnsupportedAttributeException(naiveBayesModel, miningFunction);
		}

		return OutputUtil.evaluate(predictions, context);
	}

	private Map<FieldName, ? extends Classification<Double>> evaluateClassification(final ValueFactory<Double> valueFactory, EvaluationContext context){
		NaiveBayesModel naiveBayesModel = getModel();

		TargetField targetField = getTargetField();

		double threshold = naiveBayesModel.getThreshold();

		Map<FieldName, Map<String, Double>> fieldCountSums = getFieldCountSums();

		// Probability calculations use logarithmic scale for greater numerical stability
		ProbabilityMap<String, Double> probabilities = new ProbabilityMap<String, Double>(){

			@Override
			public ValueFactory<Double> getValueFactory(){
				return valueFactory;
			}

			@Override
			public void multiply(String key, double probability){
				Value<Double> value = ensureValue(key);

				value.add(Math.log(probability));
			}
		};

		List<BayesInput> bayesInputs = getBayesInputs();
		for(BayesInput bayesInput : bayesInputs){
			FieldName name = bayesInput.getField();
			if(name == null){
				throw new MissingAttributeException(bayesInput, PMMLAttributes.BAYESINPUT_FIELD);
			}

			FieldValue value = context.evaluate(name);

			// "Missing values are ignored"
			if(Objects.equals(FieldValues.MISSING_VALUE, value)){
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

				if(Objects.equals(FieldValues.MISSING_VALUE, value)){
					continue;
				}
			}

			Map<String, Double> countSums = fieldCountSums.get(name);

			TargetValueCounts targetValueCounts = getTargetValueCounts(bayesInput, value);
			if(targetValueCounts != null){
				calculateDiscreteProbabilities(probabilities, targetValueCounts, threshold, countSums);
			}
		}

		BayesOutput bayesOutput = naiveBayesModel.getBayesOutput();

		FieldName targetFieldName = bayesOutput.getField();
		if(targetFieldName == null){
			throw new MissingAttributeException(bayesOutput, PMMLAttributes.BAYESOUTPUT_FIELD);
		} // End if

		if(targetFieldName != null && !Objects.equals(targetField.getName(), targetFieldName)){
			throw new InvalidAttributeException(bayesOutput, PMMLAttributes.BAYESOUTPUT_FIELD, targetFieldName);
		}

		calculatePriorProbabilities(probabilities, bayesOutput.getTargetValueCounts());

		// Convert from logarithmic scale to normal scale
		ValueUtil.normalizeSoftMax(probabilities);

		ProbabilityDistribution<Double> result = new ProbabilityDistribution<>(probabilities);

		return TargetUtil.evaluateClassification(targetField, result);
	}

	private FieldValue discretize(DerivedField derivedField, FieldValue value){
		Expression expression = ExpressionUtil.ensureExpression(derivedField);

		if(expression instanceof Discretize){
			Discretize discretize = (Discretize)expression;

			value = DiscretizationUtil.discretize(discretize, value);
			if(Objects.equals(FieldValues.MISSING_VALUE, value)){
				return FieldValues.MISSING_VALUE;
			}

			return FieldValueUtil.refine(derivedField, value);
		} else

		{
			throw new MisplacedElementException(expression);
		}
	}

	private void calculateContinuousProbabilities(ProbabilityMap<String, Double> probabilities, TargetValueStats targetValueStats, double threshold, FieldValue value){
		Number x = value.asNumber();

		for(TargetValueStat targetValueStat : targetValueStats){
			String targetCategory = targetValueStat.getValue();
			if(targetCategory == null){
				throw new MissingAttributeException(targetValueStat, PMMLAttributes.TARGETVALUESTAT_VALUE);
			}

			ContinuousDistribution distribution = targetValueStat.getContinuousDistribution();
			if(distribution == null){
				throw new MissingElementException(MissingElementException.formatMessage(XPathUtil.formatElement(targetValueStat.getClass()) + "/<ContinuousDistribution>"), targetValueStat);
			} // End if

			double probability;

			// "For Naive Bayes models, continuous distribution types are restricted to Gaussian and Poisson distributions"
			if((distribution instanceof GaussianDistribution) || (distribution instanceof PoissonDistribution)){

				if(DistributionUtil.isNoOp(distribution)){
					continue;
				}

				probability = DistributionUtil.probability(distribution, x);
			} else

			{
				throw new MisplacedElementException(distribution);
			}

			// The calculated probability cannot fall below the default probability
			probability = Math.max(probability, threshold);

			probabilities.multiply(targetCategory, probability);
		}
	}

	private void calculateDiscreteProbabilities(ProbabilityMap<String, Double> probabilities, TargetValueCounts targetValueCounts, double threshold, Map<String, Double> countSums){

		for(TargetValueCount targetValueCount : targetValueCounts){
			String targetCategory = targetValueCount.getValue();
			if(targetCategory == null){
				throw new MissingAttributeException(targetValueCount, PMMLAttributes.TARGETVALUECOUNT_VALUE);
			}

			double count = targetValueCount.getCount();

			double probability;

			// The calculated probability can fall below the default probability
			// However, a count of zero represents a special case, which needs adjustment
			if(VerificationUtil.isZero(count, Precision.EPSILON)){
				probability = threshold;
			} else

			{
				Double countSum = countSums.get(targetCategory);

				probability = count / countSum;
			}

			probabilities.multiply(targetCategory, probability);
		}
	}

	private void calculatePriorProbabilities(ProbabilityMap<String, Double> probabilities, TargetValueCounts targetValueCounts){

		for(TargetValueCount targetValueCount : targetValueCounts){
			String targetCategory = targetValueCount.getValue();
			if(targetCategory == null){
				throw new MissingAttributeException(targetValueCount, PMMLAttributes.TARGETVALUECOUNT_VALUE);
			}

			double probability = targetValueCount.getCount();

			probabilities.multiply(targetCategory, probability);
		}
	}

	protected List<BayesInput> getBayesInputs(){

		if(this.bayesInputs == null){
			this.bayesInputs = getValue(NaiveBayesModelEvaluator.bayesInputCache);
		}

		return this.bayesInputs;
	}

	protected Map<FieldName, Map<String, Double>> getFieldCountSums(){

		if(this.fieldCountSums == null){
			this.fieldCountSums = getValue(NaiveBayesModelEvaluator.fieldCountSumCache);
		}

		return this.fieldCountSums;
	}

	static
	private Map<FieldName, Map<String, Double>> calculateFieldCountSums(NaiveBayesModel naiveBayesModel){
		Map<FieldName, Map<String, Double>> result = new LinkedHashMap<>();

		List<BayesInput> bayesInputs = CacheUtil.getValue(naiveBayesModel, NaiveBayesModelEvaluator.bayesInputCache);
		for(BayesInput bayesInput : bayesInputs){
			FieldName name = bayesInput.getField();

			Map<String, Double> counts = new LinkedHashMap<>();

			List<PairCounts> pairCounts = bayesInput.getPairCounts();
			for(PairCounts pairCount : pairCounts){
				TargetValueCounts targetValueCounts = pairCount.getTargetValueCounts();

				for(TargetValueCount targetValueCount : targetValueCounts){
					Double count = counts.get(targetValueCount.getValue());

					if(count == null){
						count = 0d;
					}

					counts.put(targetValueCount.getValue(), count + targetValueCount.getCount());
				}
			}

			result.put(name, counts);
		}

		return result;
	}

	static
	private List<BayesInput> parseBayesInputs(NaiveBayesModel naiveBayesModel){
		BayesInputs bayesInputs = naiveBayesModel.getBayesInputs();

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

		if(bayesInput instanceof HasParsedValueMapping){
			HasParsedValueMapping<?> hasParsedValueMapping = (HasParsedValueMapping<?>)bayesInput;

			return (TargetValueCounts)value.getMapping(hasParsedValueMapping);
		}

		List<PairCounts> pairCounts = bayesInput.getPairCounts();
		for(PairCounts pairCount : pairCounts){
			String category = pairCount.getValue();
			if(category == null){
				throw new MissingAttributeException(pairCount, PMMLAttributes.PAIRCOUNTS_VALUE);
			} // End if

			if(value.equalsString(category)){
				TargetValueCounts targetValueCounts = pairCount.getTargetValueCounts();
				if(targetValueCounts == null){
					throw new MissingElementException(pairCount, PMMLElements.PAIRCOUNTS_TARGETVALUECOUNTS);
				}

				return targetValueCounts;
			}
		}

		return null;
	}

	private static final LoadingCache<NaiveBayesModel, List<BayesInput>> bayesInputCache = CacheUtil.buildLoadingCache(new CacheLoader<NaiveBayesModel, List<BayesInput>>(){

		@Override
		public List<BayesInput> load(NaiveBayesModel naiveBayesModel){
			return ImmutableList.copyOf(parseBayesInputs(naiveBayesModel));
		}
	});

	private static final LoadingCache<NaiveBayesModel, Map<FieldName, Map<String, Double>>> fieldCountSumCache = CacheUtil.buildLoadingCache(new CacheLoader<NaiveBayesModel, Map<FieldName, Map<String, Double>>>(){

		@Override
		public Map<FieldName, Map<String, Double>> load(NaiveBayesModel naiveBayesModel){
			return ImmutableMap.copyOf(calculateFieldCountSums(naiveBayesModel));
		}
	});
}