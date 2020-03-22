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
package org.jpmml.evaluator.rule_set;

import java.util.Set;

import org.dmg.pmml.DataType;
import org.dmg.pmml.rule_set.PMMLAttributes;
import org.dmg.pmml.rule_set.SimpleRule;
import org.jpmml.evaluator.EntityClassification;
import org.jpmml.evaluator.HasConfidence;
import org.jpmml.evaluator.MissingAttributeException;
import org.jpmml.evaluator.Report;
import org.jpmml.evaluator.TypeUtil;
import org.jpmml.evaluator.ValueMap;

abstract
public class SimpleRuleScoreDistribution<V extends Number> extends EntityClassification<SimpleRule, Object, V> implements HasConfidence {

	SimpleRuleScoreDistribution(ValueMap<Object, V> confidences){
		super(Type.CONFIDENCE, confidences);
	}

	@Override
	protected void computeResult(DataType dataType){
		SimpleRule simpleRule = getEntity();

		if(simpleRule != null){
			Object score = simpleRule.getScore();
			if(score == null){
				throw new MissingAttributeException(simpleRule, PMMLAttributes.SIMPLERULE_SCORE);
			}

			Object result = TypeUtil.parseOrCast(dataType, score);

			super.setResult(result);

			return;
		}

		super.computeResult(dataType);
	}

	@Override
	public Set<Object> getCategories(){
		return keySet();
	}

	@Override
	public Double getConfidence(Object category){
		return getValue(category);
	}

	@Override
	public Report getConfidenceReport(Object category){
		return getValueReport(category);
	}

	@Override
	protected void setEntity(SimpleRule simpleRule){
		super.setEntity(simpleRule);
	}
}