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
import java.util.Map;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import org.dmg.pmml.MiningFunction;
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.Computable;
import org.jpmml.evaluator.HasReasonCodeRanking;

/**
 * @see MiningFunction#REGRESSION
 */
public class ReasonCodeRanking implements Computable, HasReasonCodeRanking {

	private Object result = null;

	private Map<String, Double> reasonCodes = null;


	ReasonCodeRanking(Object result, Map<String, Double> reasonCodes){
		setResult(result);
		setReasonCodes(reasonCodes);
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
		Map<String, Double> reasonCodes = getReasonCodes();

		return Classification.entryKeys(Classification.getWinnerList(Classification.Type.VOTE, reasonCodes.entrySet()));
	}

	@Override
	public String toString(){
		ToStringHelper helper = Objects.toStringHelper(this)
			.add("result", getResult())
			.add("reasonCodeRanking", getReasonCodeRanking());

		return helper.toString();
	}

	public Map<String, Double> getReasonCodes(){
		return this.reasonCodes;
	}

	private void setReasonCodes(Map<String, Double> reasonCodes){
		this.reasonCodes = reasonCodes;
	}
}