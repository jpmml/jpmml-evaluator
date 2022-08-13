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
import java.util.Objects;

import com.google.common.collect.ForwardingMap;
import org.dmg.pmml.mining.Segment;
import org.jpmml.evaluator.HasEntityId;
import org.jpmml.evaluator.HasResultFields;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.OutputField;
import org.jpmml.evaluator.TargetField;

abstract
public class SegmentResult extends ForwardingMap<String, Object> implements HasEntityId, HasResultFields {

	private Segment segment = null;

	private Map<String, ?> results = null;


	SegmentResult(Segment segment, Map<String, ?> results){
		setSegment(segment);
		setResults(results);
	}

	abstract
	protected ModelEvaluator<?> getModelEvaluator();

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Map<String, Object> delegate(){
		Map<String, ?> results = getResults();

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

		String targetName = modelEvaluator.getTargetName();

		return get(targetName);
	}

	boolean hasMissingTargetValues(){
		List<TargetField> targetFields = getTargetFields();

		for(TargetField targetField : targetFields){
			String name = targetField.getFieldName();

			if(get(name) == null){
				return true;
			}
		}

		return false;
	}

	public Number getWeight(){
		Segment segment = getSegment();

		return segment.getWeight();
	}

	public Segment getSegment(){
		return this.segment;
	}

	private void setSegment(Segment segment){
		this.segment = Objects.requireNonNull(segment);
	}

	public Map<String, ?> getResults(){
		return this.results;
	}

	private void setResults(Map<String, ?> results){
		this.results = Objects.requireNonNull(results);
	}
}