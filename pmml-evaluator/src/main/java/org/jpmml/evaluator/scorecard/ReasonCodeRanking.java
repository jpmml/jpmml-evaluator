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

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import org.dmg.pmml.MiningFunction;
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.Computable;
import org.jpmml.evaluator.HasReasonCodeRanking;
import org.jpmml.evaluator.ValueMap;

/**
 * @see MiningFunction#REGRESSION
 */
public class ReasonCodeRanking<V extends Number> implements Computable, HasReasonCodeRanking {

	private Object result = null;

	private ValueMap<String, V> reasonCodePoints = null;


	ReasonCodeRanking(Object result, ValueMap<String, V> reasonCodePoints){
		setResult(result);
		setReasonCodePoints(reasonCodePoints);
	}

	@Override
	public Object getResult(){
		return this.result;
	}

	private void setResult(Object result){
		this.result = result;
	}

	@Override
	public List<String> getReasonCodeRanking(){
		ValueMap<String, V> reasonCodePoints = getReasonCodePoints();

		return Classification.entryKeys(Classification.getWinnerList(Classification.Type.VOTE, reasonCodePoints.entrySet()));
	}

	@Override
	public String toString(){
		ToStringHelper helper = Objects.toStringHelper(this)
			.add("result", getResult())
			.add("reasonCodeRanking", getReasonCodeRanking());

		return helper.toString();
	}

	public ValueMap<String, V> getReasonCodePoints(){
		return this.reasonCodePoints;
	}

	private void setReasonCodePoints(ValueMap<String, V> reasonCodePoints){
		this.reasonCodePoints = reasonCodePoints;
	}
}