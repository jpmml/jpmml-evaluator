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

import java.util.List;

import org.dmg.pmml.DataType;
import org.dmg.pmml.mining.Segmentation;
import org.jpmml.evaluator.EvaluationException;
import org.jpmml.evaluator.HasProbability;
import org.jpmml.evaluator.ProbabilityAggregator;
import org.jpmml.evaluator.ValueAggregator;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueMap;
import org.jpmml.evaluator.VoteAggregator;

public class MiningModelUtil {

	private MiningModelUtil(){
	}

	static
	public <V extends Number> Value<V> aggregateValues(final ValueFactory<V> valueFactory, List<SegmentResult> segmentResults, Segmentation.MultipleModelMethod multipleModelMethod){
		ValueAggregator<V> aggregator;

		switch(multipleModelMethod){
			case AVERAGE:
			case SUM:
				aggregator = new ValueAggregator<>(valueFactory.newVector(0));
				break;
			case MEDIAN:
				aggregator = new ValueAggregator<>(valueFactory.newVector(segmentResults.size()));
				break;
			case WEIGHTED_AVERAGE:
			case WEIGHTED_SUM:
				aggregator = new ValueAggregator<>(valueFactory.newVector(0), valueFactory.newVector(0), valueFactory.newVector(0));
				break;
			case WEIGHTED_MEDIAN:
				aggregator = new ValueAggregator<>(valueFactory.newVector(segmentResults.size()), valueFactory.newVector(segmentResults.size()));
				break;
			default:
				throw new EvaluationException();
		}

		for(SegmentResult segmentResult : segmentResults){
			Double value = (Double)segmentResult.getTargetValue(DataType.DOUBLE);

			switch(multipleModelMethod){
				case AVERAGE:
				case SUM:
				case MEDIAN:
					aggregator.add(value);
					break;
				case WEIGHTED_AVERAGE:
				case WEIGHTED_SUM:
				case WEIGHTED_MEDIAN:
					double weight = segmentResult.getWeight();

					aggregator.add(value, weight);
					break;
				default:
					throw new EvaluationException();
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
				throw new EvaluationException();
		}
	}

	static
	public <V extends Number> ValueMap<String, V> aggregateVotes(final ValueFactory<V> valueFactory, List<SegmentResult> segmentResults, Segmentation.MultipleModelMethod multipleModelMethod){
		VoteAggregator<String, V> aggregator = new VoteAggregator<String, V>(){

			@Override
			public ValueFactory<V> getValueFactory(){
				return valueFactory;
			}
		};

		for(SegmentResult segmentResult : segmentResults){
			String key = (String)segmentResult.getTargetValue(DataType.STRING);

			switch(multipleModelMethod){
				case MAJORITY_VOTE:
					aggregator.add(key);
					break;
				case WEIGHTED_MAJORITY_VOTE:
					double weight = segmentResult.getWeight();

					aggregator.add(key, weight);
					break;
				default:
					throw new EvaluationException();
			}
		}

		return aggregator.sumMap();
	}

	static
	public <V extends Number> ValueMap<String, V> aggregateProbabilities(final ValueFactory<V> valueFactory, List<SegmentResult> segmentResults, List<String> categories, Segmentation.MultipleModelMethod multipleModelMethod){
		ProbabilityAggregator<V> aggregator;

		switch(multipleModelMethod){
			case AVERAGE:
				aggregator = new ProbabilityAggregator<V>(0){

					@Override
					public ValueFactory<V> getValueFactory(){
						return valueFactory;
					}
				};
				break;
			case WEIGHTED_AVERAGE:
				aggregator = new ProbabilityAggregator<V>(0, valueFactory.newVector(0)){

					@Override
					public ValueFactory<V> getValueFactory(){
						return valueFactory;
					}
				};
				break;
			case MEDIAN:
			case MAX:
				aggregator = new ProbabilityAggregator<V>(segmentResults.size()){

					@Override
					public ValueFactory<V> getValueFactory(){
						return valueFactory;
					}
				};
				break;
			default:
				throw new EvaluationException();
		}

		for(SegmentResult segmentResult : segmentResults){
			HasProbability hasProbability = segmentResult.getTargetValue(HasProbability.class);

			switch(multipleModelMethod){
				case AVERAGE:
				case MEDIAN:
				case MAX:
					aggregator.add(hasProbability);
					break;
				case WEIGHTED_AVERAGE:
					double weight = segmentResult.getWeight();

					aggregator.add(hasProbability, weight);
					break;
				default:
					throw new EvaluationException();
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
				throw new EvaluationException();
		}
	}
}