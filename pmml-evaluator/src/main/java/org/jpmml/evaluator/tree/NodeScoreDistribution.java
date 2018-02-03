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
package org.jpmml.evaluator.tree;

import java.util.Collections;
import java.util.Set;

import com.google.common.base.Objects.ToStringHelper;
import org.dmg.pmml.DataType;
import org.dmg.pmml.tree.Node;
import org.jpmml.evaluator.EntityClassification;
import org.jpmml.evaluator.HasConfidence;
import org.jpmml.evaluator.HasProbability;
import org.jpmml.evaluator.Numbers;
import org.jpmml.evaluator.Report;
import org.jpmml.evaluator.ReportUtil;
import org.jpmml.evaluator.TypeUtil;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueMap;

abstract
public class NodeScoreDistribution<V extends Number> extends EntityClassification<Node, V> implements HasProbability, HasConfidence {

	private ValueMap<String, V> confidences = null;


	NodeScoreDistribution(ValueMap<String, V> probabilities,  Node node){
		super(Type.PROBABILITY, probabilities);

		setEntity(node);
	}

	@Override
	protected void computeResult(DataType dataType){
		Node node = getEntity();

		if(node.hasScore()){
			Object result = TypeUtil.parseOrCast(dataType, node.getScore());

			super.setResult(result);

			return;
		}

		super.computeResult(dataType);
	}

	@Override
	protected ToStringHelper toStringHelper(){
		ValueMap<String, V> confidences = getConfidences();

		ToStringHelper helper = super.toStringHelper()
			.add(Type.CONFIDENCE.entryKey(), confidences != null ? confidences.entrySet() : Collections.emptySet());

		return helper;
	}

	public boolean isEmpty(){
		ValueMap<String, V> values = getValues();

		return values.isEmpty();
	}

	@Override
	public Set<String> getCategoryValues(){

		if(isEmpty()){
			Node node = getEntity();

			return Collections.singleton(node.getScore());
		}

		return keySet();
	}

	@Override
	public Double getProbability(String category){

		if(isEmpty()){
			Node node = getEntity();

			if(category != null && (category).equals(node.getScore())){
				return Numbers.DOUBLE_ONE;
			}

			return Numbers.DOUBLE_ZERO;
		}

		return getValue(category);
	}

	@Override
	public Report getProbabilityReport(String category){

		if(isEmpty()){
			return null;
		}

		return getValueReport(category);
	}

	@Override
	public Double getConfidence(String category){
		ValueMap<String, V> confidences = getConfidences();

		Value<V> confidence = (confidences != null ? confidences.get(category) : null);

		return Type.CONFIDENCE.getValue(confidence);
	}

	@Override
	public Report getConfidenceReport(String category){
		ValueMap<String, V> confidences = getConfidences();

		Value<V> confidence = (confidences != null ? confidences.get(category) : null);

		return ReportUtil.getReport(confidence);
	}

	void putConfidence(String category, Value<V> confidence){
		ValueMap<String, V> confidences = getConfidences();

		if(confidences == null){
			confidences = new ValueMap<>();

			setConfidences(confidences);
		}

		confidences.put(category, confidence);
	}

	private ValueMap<String, V> getConfidences(){
		return this.confidences;
	}

	private void setConfidences(ValueMap<String, V> confidences){

		if(confidences == null){
			throw new IllegalArgumentException();
		}

		this.confidences = confidences;
	}
}