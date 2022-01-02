/*
 * Copyright (c) 2021 Villu Ruusmann
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
package org.jpmml.evaluator.scorecard;

import java.util.List;
import java.util.Objects;

import org.jpmml.evaluator.Regression;
import org.jpmml.evaluator.Value;

abstract
public class ScorecardScore<V extends Number> extends Regression<V> implements HasPartialScores {

	private List<PartialScore> partialScores = null;


	protected ScorecardScore(Value<V> value, List<PartialScore> partialScores){
		super(value);

		setPartialScores(partialScores);
	}

	@Override
	public List<PartialScore> getPartialScores(){
		return this.partialScores;
	}

	private void setPartialScores(List<PartialScore> partialScores){
		this.partialScores = Objects.requireNonNull(partialScores);
	}
}