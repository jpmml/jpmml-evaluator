/*
 * Copyright (c) 2024 Villu Ruusmann
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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.BiMap;
import com.google.common.collect.ForwardingMap;
import org.dmg.pmml.mining.Segment;
import org.jpmml.evaluator.HasEntityRegistry;
import org.jpmml.evaluator.TypeUtil;

abstract
public class SegmentationResult extends ForwardingMap<String, Object> implements HasEntityRegistry<Segment>, HasSegmentResults {

	private Map<String, ?> results = null;


	SegmentationResult(Map<String, ?> results){
		setResults(results);
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Map<String, Object> delegate(){
		Map<String, ?> results = getResults();

		return (Map)results;
	}

	public Map<String, ?> getResults(Iterable<String> segmentIds){
		Iterator<String> it = segmentIds.iterator();

		String segmentId = it.next();

		SegmentResult segmentResult = getSegmentResult(segmentId);
		if(segmentResult == null){
			return null;
		}

		while(it.hasNext()){
			SegmentationResult segmentationResult = TypeUtil.cast(SegmentationResult.class, segmentResult.getResults());

			segmentId = it.next();

			segmentResult = segmentationResult.getSegmentResult(segmentId);
			if(segmentResult == null){
				return null;
			}
		}

		return segmentResult.getResults();
	}

	SegmentResult getSegmentResult(String id){
		BiMap<String, Segment> entityRegistry = getEntityRegistry();

		if(!entityRegistry.containsKey(id)){
			throw new IllegalArgumentException(id);
		}

		Collection<? extends SegmentResult> segmentResults = getSegmentResults();
		if(segmentResults != null && !segmentResults.isEmpty()){

			for(SegmentResult segmentResult : segmentResults){

				if(Objects.equals(segmentResult.getEntityId(), id)){
					return segmentResult;
				}
			}
		}

		return null;
	}

	public Map<String, ?> getResults(){
		return this.results;
	}

	void setResults(Map<String, ?> results){
		this.results = Objects.requireNonNull(results);
	}
}