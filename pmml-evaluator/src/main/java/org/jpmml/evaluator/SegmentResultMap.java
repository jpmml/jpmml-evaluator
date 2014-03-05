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
package org.jpmml.evaluator;

import java.util.*;

import org.dmg.pmml.*;

class SegmentResultMap extends LinkedHashMap<FieldName, Object> implements Computable, HasEntityId {

	private Segment segment = null;

	private FieldName targetField = null;


	public SegmentResultMap(Segment segment, FieldName targetField){
		setSegment(segment);
		setTargetField(targetField);
	}

	@Override
	public Object getResult(){
		Object targetValue = get(getTargetField());

		return EvaluatorUtil.decode(targetValue);
	}

	@Override
	public String getEntityId(){
		Segment segment = getSegment();

		return segment.getId();
	}

	public double getWeight(){
		Segment segment = getSegment();

		return segment.getWeight();
	}

	public Segment getSegment(){
		return this.segment;
	}

	private void setSegment(Segment segment){
		this.segment = segment;
	}

	public FieldName getTargetField(){
		return this.targetField;
	}

	private void setTargetField(FieldName targetField){
		this.targetField = targetField;
	}
}