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

import java.util.Map;

import com.google.common.collect.ForwardingMap;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.mining.Segment;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.HasEntityId;
import org.jpmml.evaluator.PMMLException;
import org.jpmml.evaluator.TypeCheckException;
import org.jpmml.evaluator.TypeUtil;

public class SegmentResult extends ForwardingMap<FieldName, Object> implements HasEntityId {

	private Segment segment = null;

	private String segmentId = null;

	private Map<FieldName, ?> result = null;

	private FieldName targetFieldName = null;


	public SegmentResult(Segment segment, String segmentId, Map<FieldName, ?> result, FieldName targetFieldName){
		setSegment(segment);
		setSegmentId(segmentId);
		setResult(result);
		setTargetFieldName(targetFieldName);
	}

	@Override
	public String getEntityId(){
		return getSegmentId();
	}

	@Override
	@SuppressWarnings (
		value = {"rawtypes", "unchecked"}
	)
	public Map<FieldName, Object> delegate(){
		Map<FieldName, ?> result = getResult();

		return (Map)result;
	}

	public double getWeight(){
		Segment segment = getSegment();

		return segment.getWeight();
	}

	public Object getTargetValue(){
		FieldName targetFieldName = getTargetFieldName();

		return get(targetFieldName);
	}

	public Object getTargetValue(DataType dataType){
		Object targetValue = EvaluatorUtil.decode(getTargetValue());

		try {
			return TypeUtil.cast(dataType, targetValue);
		} catch(TypeCheckException tce){
			throw ensureContext(tce);
		}
	}

	public <V> V getTargetValue(Class<V> clazz){
		Object targetValue = getTargetValue();

		try {
			return TypeUtil.cast(clazz, targetValue);
		} catch(TypeCheckException tce){
			throw ensureContext(tce);
		}
	}

	public Segment getSegment(){
		return this.segment;
	}

	private void setSegment(Segment segment){
		this.segment = segment;
	}

	public String getSegmentId(){
		return this.segmentId;
	}

	private void setSegmentId(String segmentId){
		this.segmentId = segmentId;
	}

	public Map<FieldName, ?> getResult(){
		return this.result;
	}

	private void setResult(Map<FieldName, ?> result){
		this.result = result;
	}

	public FieldName getTargetFieldName(){
		return this.targetFieldName;
	}

	private void setTargetFieldName(FieldName targetFieldName){
		this.targetFieldName = targetFieldName;
	}

	private <E extends PMMLException> E ensureContext(E exception){
		Segment segment = getSegment();

		exception.ensureContext(segment);

		return exception;
	}
}