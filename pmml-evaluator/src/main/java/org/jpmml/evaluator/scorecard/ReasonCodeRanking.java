/*
 * Copyright (c) 2015 Villu Ruusmann
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

import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.Classification.Type;
import org.jpmml.evaluator.HasReasonCodeRanking;
import org.jpmml.evaluator.Regression;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueMap;
import org.jpmml.model.ToStringHelper;

public class ReasonCodeRanking<V extends Number> extends Regression<V> implements HasReasonCodeRanking {

	private ValueMap<String, V> reasonCodePoints = null;


	ReasonCodeRanking(Value<V> value, ValueMap<String, V> reasonCodePoints){
		super(value);

		setReasonCodePoints(reasonCodePoints);
	}

	@Override
	protected ToStringHelper toStringHelper(){
		ValueMap<String, V> reasonCodePoints = getReasonCodePoints();

		ToStringHelper helper = super.toStringHelper()
			.add(Type.VOTE.entryKey(), reasonCodePoints.entrySet())
			.add("reasonCodeRanking", getReasonCodeRanking());

		return helper;
	}

	@Override
	public List<String> getReasonCodeRanking(){
		ValueMap<String, V> reasonCodePoints = getReasonCodePoints();

		return Classification.entryKeys(Classification.getWinnerList(Classification.Type.VOTE, reasonCodePoints.entrySet()));
	}

	public ValueMap<String, V> getReasonCodePoints(){
		return this.reasonCodePoints;
	}

	private void setReasonCodePoints(ValueMap<String, V> reasonCodePoints){

		if(reasonCodePoints == null){
			throw new IllegalArgumentException();
		}

		this.reasonCodePoints = reasonCodePoints;
	}
}