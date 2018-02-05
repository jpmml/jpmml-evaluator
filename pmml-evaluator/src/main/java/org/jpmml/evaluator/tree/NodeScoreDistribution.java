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

import org.dmg.pmml.DataType;
import org.dmg.pmml.tree.Node;
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.EntityUtil;
import org.jpmml.evaluator.HasConfidence;
import org.jpmml.evaluator.HasEntityId;
import org.jpmml.evaluator.HasEntityRegistry;
import org.jpmml.evaluator.HasProbability;
import org.jpmml.evaluator.Report;
import org.jpmml.evaluator.ReportUtil;
import org.jpmml.evaluator.ToStringHelper;
import org.jpmml.evaluator.TypeUtil;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueMap;

abstract
public class NodeScoreDistribution<V extends Number> extends Classification<V> implements HasEntityId, HasEntityRegistry<Node>, HasProbability, HasConfidence {

	private Node node = null;

	private ValueMap<String, V> confidences = null;


	NodeScoreDistribution(ValueMap<String, V> probabilities,  Node node){
		super(Type.PROBABILITY, probabilities);

		setNode(node);
	}

	@Override
	protected void computeResult(DataType dataType){
		Node node = getNode();

		Object result = TypeUtil.parseOrCast(dataType, node.getScore());

		setResult(result);
	}

	@Override
	protected ToStringHelper toStringHelper(){
		ValueMap<String, V> confidences = getConfidences();

		ToStringHelper helper = super.toStringHelper()
			.add("entityId", getEntityId())
			.add(Type.CONFIDENCE.entryKey(), confidences != null ? confidences.entrySet() : Collections.emptySet());

		return helper;
	}

	@Override
	public String getEntityId(){
		Node node = getNode();

		return EntityUtil.getId(node, this);
	}

	@Override
	public Set<String> getCategoryValues(){
		return keySet();
	}

	@Override
	public Double getProbability(String category){
		return getValue(category);
	}

	@Override
	public Report getProbabilityReport(String category){
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

	public Node getNode(){
		return this.node;
	}

	private void setNode(Node node){

		if(node == null){
			throw new IllegalArgumentException();
		}

		this.node = node;
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