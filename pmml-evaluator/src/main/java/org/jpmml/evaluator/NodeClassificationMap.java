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

import java.util.Map;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.collect.Maps;
import org.dmg.pmml.Node;

@Beta
public class NodeClassificationMap extends EntityClassificationMap<Node> implements HasConfidence, HasProbability {

	private Map<String, Double> confidences = Maps.newLinkedHashMap();


	protected NodeClassificationMap(){
		super(Type.PROBABILITY);
	}

	protected NodeClassificationMap(Node node){
		super(Type.PROBABILITY, node);
	}

	@Override
	public String getResult(){
		Node node = getEntity();

		String score = node.getScore();
		if(score != null){
			return score;
		}

		return super.getResult();
	}

	@Override
	public Double getConfidence(String value){
		return this.confidences.get(value);
	}

	void putConfidence(String value, Double confidence){
		this.confidences.put(value, confidence);
	}

	@Override
	public Double getProbability(String value){
		return getFeature(value);
	}

	@Override
	protected ToStringHelper toStringHelper(){
		ToStringHelper helper = super.toStringHelper()
			.add(Type.CONFIDENCE.entryKey(), this.confidences.entrySet());

		return helper;
	}
}