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
package org.jpmml.evaluator.mining;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.mining.Segmentation;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.HasProbability;
import org.jpmml.evaluator.ProbabilityAggregator;
import org.jpmml.evaluator.TypeCheckException;
import org.jpmml.evaluator.TypeUtil;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueAggregator;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueMap;
import org.jpmml.evaluator.VoteAggregator;

public class MiningModelUtil {

	private MiningModelUtil(){
	}

	static
	public SegmentResult asSegmentResult(Segmentation.MultipleModelMethod multipleModelMethod, Map<FieldName, ?> predictions){

		switch(multipleModelMethod){
			case SELECT_FIRST:
			case SELECT_ALL:
			case MODEL_CHAIN:
				{
					if(predictions instanceof SegmentResult){
						SegmentResult segmentResult = (SegmentResult)predictions;

						return segmentResult;
					}
				}
				break;
			default:
				break;
		}

		return null;
	}

	static
	public <V extends Number> Value<V> aggregateValues(ValueFactory<V> valueFactory, Segmentation.MultipleModelMethod multipleModelMethod, Segmentation.MissingPredictionTreatment missingPredictionTreatment, Number missingThreshold, List<SegmentResult> segmentResults){
		ValueAggregator<V> aggregator;

		switch(multipleModelMethod){
			case AVERAGE:
			case SUM:
				aggregator = new ValueAggregator.UnivariateStatistic<>(valueFactory);
				break;
			case MEDIAN:
				aggregator = new ValueAggregator.Median<>(valueFactory, segmentResults.size());
				break;
			case WEIGHTED_AVERAGE:
			case WEIGHTED_SUM:
				aggregator = new ValueAggregator.WeightedUnivariateStatistic<>(valueFactory);
				break;
			case WEIGHTED_MEDIAN:
				aggregator = new ValueAggregator.WeightedMedian<>(valueFactory, segmentResults.size());
				break;
			default:
				throw new IllegalArgumentException();
		}

		Fraction<V> missingFraction = null;

		segmentResults:
		for(SegmentResult segmentResult : segmentResults){
			Object targetValue = EvaluatorUtil.decode(segmentResult.getTargetValue());

			if(targetValue == null){

				switch(missingPredictionTreatment){
					case RETURN_MISSING:
						return null;
					case SKIP_SEGMENT:
						if(missingFraction == null){
							missingFraction = new Fraction<>(valueFactory, segmentResults);
						} // End if

						if(missingFraction.update(segmentResult, missingThreshold)){
							return null;
						}

						continue segmentResults;
					case CONTINUE:
						return null;
					default:
						throw new IllegalArgumentException();
				}
			}

			Number value;

			try {
				if(targetValue instanceof Number){
					value = (Number)targetValue;
				} else

				{
					value = (Number)TypeUtil.cast(DataType.DOUBLE, targetValue);
				}
			} catch(TypeCheckException tce){
				throw tce.ensureContext(segmentResult.getSegment());
			}

			switch(multipleModelMethod){
				case AVERAGE:
				case SUM:
				case MEDIAN:
					aggregator.add(value);
					break;
				case WEIGHTED_AVERAGE:
				case WEIGHTED_SUM:
				case WEIGHTED_MEDIAN:
					Number weight = segmentResult.getWeight();

					aggregator.add(value, weight);
					break;
				default:
					throw new IllegalArgumentException();
			}
		}

		switch(multipleModelMethod){
			case AVERAGE:
				return aggregator.average();
			case WEIGHTED_AVERAGE:
				return aggregator.weightedAverage();
			case SUM:
				return aggregator.sum();
			case WEIGHTED_SUM:
				return aggregator.weightedSum();
			case MEDIAN:
				return aggregator.median();
			case WEIGHTED_MEDIAN:
				return aggregator.weightedMedian();
			default:
				throw new IllegalArgumentException();
		}
	}

	static
	public <V extends Number> ValueMap<Object, V> aggregateVotes(ValueFactory<V> valueFactory, Segmentation.MultipleModelMethod multipleModelMethod, Segmentation.MissingPredictionTreatment missingPredictionTreatment, Number missingThreshold, List<SegmentResult> segmentResults){
		VoteAggregator<Object, V> aggregator = new VoteAggregator<>(valueFactory);

		Fraction<V> missingFraction = null;

		segmentResults:
		for(SegmentResult segmentResult : segmentResults){
			Object targetValue = EvaluatorUtil.decode(segmentResult.getTargetValue());

			if(targetValue == null){

				switch(missingPredictionTreatment){
					case RETURN_MISSING:
						return null;
					case SKIP_SEGMENT:
					case CONTINUE:
						if(missingFraction == null){
							missingFraction = new Fraction<>(valueFactory, segmentResults);
						} // End if

						if(missingFraction.update(segmentResult, missingThreshold)){
							return null;
						}
						break;
					default:
						throw new IllegalArgumentException();
				} // End switch

				switch(missingPredictionTreatment){
					case SKIP_SEGMENT:
						continue segmentResults;
					case CONTINUE:
						break;
					default:
						throw new IllegalArgumentException();
				}
			}

			switch(multipleModelMethod){
				case MAJORITY_VOTE:
					aggregator.add(targetValue);
					break;
				case WEIGHTED_MAJORITY_VOTE:
					Number weight = segmentResult.getWeight();

					aggregator.add(targetValue, weight);
					break;
				default:
					throw new IllegalArgumentException();
			}
		}

		ValueMap<Object, V> result = aggregator.sumMap();

		switch(missingPredictionTreatment){
			case CONTINUE:
				// Remove the "missing" pseudo-category
				Value<V> missingVoteSum = result.remove(null);

				if(missingVoteSum != null){
					Collection<Value<V>> voteSums = result.values();

					// "The missing result is returned if it gets the most (possibly weighted) votes"
					if(!voteSums.isEmpty() && (missingVoteSum).compareTo(Collections.max(voteSums)) > 0){
						return null;
					}
				}
				break;
			default:
				break;
		}

		return result;
	}

	static
	public <V extends Number> ValueMap<Object, V> aggregateProbabilities(ValueFactory<V> valueFactory, Segmentation.MultipleModelMethod multipleModelMethod, Segmentation.MissingPredictionTreatment missingPredictionTreatment, Number missingThreshold, List<?> categories, List<SegmentResult> segmentResults){
		ProbabilityAggregator<V> aggregator;

		switch(multipleModelMethod){
			case AVERAGE:
				aggregator = new ProbabilityAggregator.Average<>(valueFactory);
				break;
			case WEIGHTED_AVERAGE:
				aggregator = new ProbabilityAggregator.WeightedAverage<>(valueFactory);
				break;
			case MEDIAN:
				aggregator = new ProbabilityAggregator.Median<>(valueFactory, segmentResults.size());
				break;
			case MAX:
				aggregator = new ProbabilityAggregator.Max<>(valueFactory, segmentResults.size());
				break;
			default:
				throw new IllegalArgumentException();
		}

		Fraction<V> missingFraction = null;

		segmentResults:
		for(SegmentResult segmentResult : segmentResults){
			Object targetValue = segmentResult.getTargetValue();

			if(targetValue == null){

				switch(missingPredictionTreatment){
					case RETURN_MISSING:
						return null;
					case SKIP_SEGMENT:
						if(missingFraction == null){
							missingFraction = new Fraction<>(valueFactory, segmentResults);
						} // End if

						if(missingFraction.update(segmentResult, missingThreshold)){
							return null;
						}

						continue segmentResults;
					case CONTINUE:
						return null;
					default:
						throw new IllegalArgumentException();
				}
			}

			HasProbability hasProbability;

			try {
				hasProbability = TypeUtil.cast(HasProbability.class, targetValue);
			} catch(TypeCheckException tce){
				throw tce.ensureContext(segmentResult.getSegment());
			}

			switch(multipleModelMethod){
				case AVERAGE:
				case MEDIAN:
				case MAX:
					aggregator.add(hasProbability);
					break;
				case WEIGHTED_AVERAGE:
					Number weight = segmentResult.getWeight();

					aggregator.add(hasProbability, weight);
					break;
				default:
					throw new IllegalArgumentException();
			}
		}

		switch(multipleModelMethod){
			case AVERAGE:
				return aggregator.averageMap();
			case WEIGHTED_AVERAGE:
				return aggregator.weightedAverageMap();
			case MEDIAN:
				return aggregator.medianMap(categories);
			case MAX:
				return aggregator.maxMap(categories);
			default:
				throw new IllegalArgumentException();
		}
	}

	static
	private class Fraction<V extends Number> {

		private Value<V> weightSum = null;

		private Value<V> missingWeightSum = null;


		private Fraction(ValueFactory<V> valueFactory, List<SegmentResult> segmentResults){
			this.weightSum = valueFactory.newValue();
			this.missingWeightSum = valueFactory.newValue();

			for(int i = 0, max = segmentResults.size(); i < max; i++){
				SegmentResult segmentResult = segmentResults.get(i);

				this.weightSum.add(segmentResult.getWeight());
			}
		}

		public boolean update(SegmentResult segmentResult, Number missingThreshold){
			this.missingWeightSum.add(segmentResult.getWeight());

			return (this.missingWeightSum.doubleValue() / this.weightSum.doubleValue()) > missingThreshold.doubleValue();
		}
	}
}
