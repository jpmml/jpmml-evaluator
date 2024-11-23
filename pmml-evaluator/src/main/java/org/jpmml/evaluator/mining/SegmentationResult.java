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

import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ForwardingMap;

abstract
public class SegmentationResult extends ForwardingMap<String, Object> implements HasSegmentResults {

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

	public Map<String, ?> getResults(){
		return this.results;
	}

	void setResults(Map<String, ?> results){
		this.results = Objects.requireNonNull(results);
	}
}