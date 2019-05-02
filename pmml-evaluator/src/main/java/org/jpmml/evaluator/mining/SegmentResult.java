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
package org.jpmml.evaluator.mining;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ForwardingMap;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.mining.Segment;
import org.jpmml.evaluator.HasEntityId;
import org.jpmml.evaluator.HasResultFields;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.OutputField;
import org.jpmml.evaluator.TargetField;

abstract
public class SegmentResult extends ForwardingMap<FieldName, Object> implements HasEntityId, HasResultFields {

	private Segment segment = null;

	private Map<FieldName, ?> results = null;


	SegmentResult(Segment segment, Map<FieldName, ?> results){
		setSegment(segment);
		setResults(results);
	}

	abstract
	protected ModelEvaluator<?> getModelEvaluator();

	@Override
	@SuppressWarnings (
		value = {"rawtypes", "unchecked"}
	)
	public Map<FieldName, Object> delegate(){
		Map<FieldName, ?> results = getResults();

		return (Map)results;
	}

	@Override
	public List<TargetField> getTargetFields(){
		ModelEvaluator<?> modelEvaluator = getModelEvaluator();

		return modelEvaluator.getTargetFields();
	}

	@Override
	public List<OutputField> getOutputFields(){
		ModelEvaluator<?> modelEvaluator = getModelEvaluator();

		return modelEvaluator.getOutputFields();
	}

	public Object getTargetValue(){
		ModelEvaluator<?> modelEvaluator = getModelEvaluator();

		FieldName targetName = modelEvaluator.getTargetName();

		return get(targetName);
	}

	public Number getWeight(){
		Segment segment = getSegment();

		return segment.getWeight();
	}

	public Segment getSegment(){
		return this.segment;
	}

	private void setSegment(Segment segment){
		this.segment = segment;
	}

	public Map<FieldName, ?> getResults(){
		return this.results;
	}

	private void setResults(Map<FieldName, ?> results){
		this.results = results;
	}
}