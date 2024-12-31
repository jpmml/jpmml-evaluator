/*
 * Copyright (c) 2015 Villu Ruusmann
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
package org.jpmml.evaluator.testing;

import java.util.Map;

import com.google.common.collect.MapDifference;
import org.jpmml.model.ToStringHelper;

public class Conflict {

	private Object id = null;

	private Map<String, ?> arguments = null;

	private MapDifference<String, ?> difference = null;

	private Exception exception = null;


	public Conflict(Object id, Map<String, ?> arguments, MapDifference<String, ?> difference){
		setId(id);
		setArguments(arguments);
		setDifference(difference);
	}

	public Conflict(Object id, Map<String, ?> arguments, Exception exception){
		setId(id);
		setArguments(arguments);
		setException(exception);
	}

	@Override
	public String toString(){
		ToStringHelper helper = new ToStringHelper(this)
			.add("id", getId())
			.add("arguments", getArguments());

		MapDifference<String, ?> difference = getDifference();
		if(difference != null){
			Map<String, ?> onlyOnLeft = difference.entriesOnlyOnLeft();
			if(!onlyOnLeft.isEmpty()){
				helper.add("expected but absent", onlyOnLeft);
			}

			Map<String, ?> onlyOnRight = difference.entriesOnlyOnRight();
			if(!onlyOnRight.isEmpty()){
				helper.add("not expected but present", onlyOnRight);
			}

			Map<String, ? extends MapDifference.ValueDifference<?>> differing = difference.entriesDiffering();
			if(!differing.isEmpty()){
				helper.add("differing (expected vs. actual)", differing);
			}
		}

		Exception exception = getException();
		if(exception != null){
			helper.add("exception", exception);
		}

		return helper.toString();
	}

	public Object getId(){
		return this.id;
	}

	private void setId(Object id){
		this.id = id;
	}

	public Map<String, ?> getArguments(){
		return this.arguments;
	}

	private void setArguments(Map<String, ?> arguments){
		this.arguments = arguments;
	}

	public MapDifference<String, ?> getDifference(){
		return this.difference;
	}

	private void setDifference(MapDifference<String, ?> difference){
		this.difference = difference;
	}

	public Exception getException(){
		return this.exception;
	}

	private void setException(Exception exception){
		this.exception = exception;
	}
}