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
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.EvaluationException;
import org.jpmml.evaluator.HasProbability;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueMap;
import org.jpmml.evaluator.ValueUtil;

public class VoteDistribution<V extends Number> extends Classification<V> implements HasProbability {

	private Value<V> sum = null;


	VoteDistribution(){
		super(Type.VOTE);
	}

	VoteDistribution(ValueMap<String, V> votes){
		super(Type.VOTE, votes);
	}

	@Override
	public void computeResult(DataType dataType){
		super.computeResult(dataType);

		this.sum = ValueUtil.sum(super.values);
	}

	@Override
	public Set<String> getCategoryValues(){
		return keySet();
	}

	@Override
	public Double getProbability(String category){

		if(this.sum == null){
			throw new EvaluationException();
		}

		Value<V> probability = super.values.get(category);

		if(probability != null){
			probability = probability.copy();

			probability.divide(this.sum);
		}

		return Type.PROBABILITY.getValue(probability);
	}
}