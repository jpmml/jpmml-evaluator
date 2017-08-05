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
package org.jpmml.evaluator;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.dmg.pmml.DataType;

public class VoteDistribution extends Classification implements HasProbability {

	private Double sum = null;


	public VoteDistribution(){
		super(Type.VOTE);
	}

	public VoteDistribution(Map<String, Double> votes){
		super(Type.VOTE, votes);
	}

	@Override
	public void computeResult(DataType dataType){
		super.computeResult(dataType);

		double sum = 0;

		Collection<Double> values = values();
		for(Double value : values){
			sum += value;
		}

		this.sum = sum;
	}

	@Override
	public Set<String> getCategoryValues(){
		return keySet();
	}

	@Override
	public Double getProbability(String value){

		if(this.sum == null){
			throw new EvaluationException();
		}

		return get(value) / this.sum;
	}
}