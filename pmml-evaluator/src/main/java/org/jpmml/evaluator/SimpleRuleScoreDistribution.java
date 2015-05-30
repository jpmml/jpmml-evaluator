/*
 * Copyright (c) 2013 Villu Ruusmann
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

import java.util.Set;

import com.google.common.collect.BiMap;
import org.dmg.pmml.DataType;
import org.dmg.pmml.SimpleRule;

public class SimpleRuleScoreDistribution extends EntityClassification<SimpleRule> implements HasConfidence {

	protected SimpleRuleScoreDistribution(BiMap<String, SimpleRule> entityRegistry){
		super(Type.CONFIDENCE, entityRegistry);
	}

	@Override
	void computeResult(DataType dataType){
		SimpleRule simpleRule = getEntity();

		if(simpleRule != null){
			String score = simpleRule.getScore();
			if(score == null){
				throw new InvalidFeatureException(simpleRule);
			}

			Object result = TypeUtil.parseOrCast(dataType, score);

			super.setResult(result);

			return;
		}

		super.computeResult(dataType);
	}

	@Override
	public Set<String> getCategoryValues(){
		return keySet();
	}

	@Override
	public Double getConfidence(String value){
		return get(value);
	}
}