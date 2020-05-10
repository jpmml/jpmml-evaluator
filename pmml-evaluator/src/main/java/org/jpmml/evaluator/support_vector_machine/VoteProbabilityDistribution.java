/*
 * Copyright (c) 2014 Villu Ruusmann
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
package org.jpmml.evaluator.support_vector_machine;

import java.util.Set;

import org.dmg.pmml.DataType;
import org.jpmml.evaluator.EvaluationException;
import org.jpmml.evaluator.HasProbability;
import org.jpmml.evaluator.Report;
import org.jpmml.evaluator.ReportUtil;
import org.jpmml.evaluator.UndefinedResultException;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueMap;
import org.jpmml.evaluator.ValueUtil;
import org.jpmml.evaluator.VoteDistribution;

public class VoteProbabilityDistribution<V extends Number> extends VoteDistribution<V> implements HasProbability {

	private Value<V> sum = null;


	VoteProbabilityDistribution(ValueMap<Object, V> votes){
		super(votes);
	}

	@Override
	protected void computeResult(DataType dataType){
		ValueMap<Object, V> values = getValues();

		super.computeResult(dataType);

		this.sum = ValueUtil.sum(values);
	}

	@Override
	public Set<Object> getCategories(){
		return keySet();
	}

	@Override
	public Double getProbability(Object category){
		Value<V> probability = computeProbability(category);

		return Type.PROBABILITY.getValue(probability);
	}

	@Override
	public Report getProbabilityReport(Object category){
		Value<V> probability = computeProbability(category);

		return ReportUtil.getReport(probability);
	}

	private Value<V> computeProbability(Object category){
		ValueMap<Object, V> values = getValues();

		if(this.sum == null){
			throw new EvaluationException("Vote distribution result has not been computed");
		}

		Value<V> probability = values.get(category);

		if(probability != null){
			probability = probability.copy();

			if(this.sum.isZero()){
				throw new UndefinedResultException();
			}

			probability.divide(this.sum);
		}

		return probability;
	}
}