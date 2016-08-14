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
package org.jpmml.evaluator;

import java.util.Map;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.collect.MapDifference;
import org.dmg.pmml.FieldName;

public class Conflict {

	private Integer id = null;

	private Map<FieldName, ?> arguments = null;

	private MapDifference<FieldName, ?> difference = null;


	public Conflict(Integer id, Map<FieldName, ?> arguments, MapDifference<FieldName, ?> difference){
		setId(id);
		setArguments(arguments);
		setDifference(difference);
	}

	@Override
	public String toString(){
		ToStringHelper helper = Objects.toStringHelper(this)
			.add("id", getId())
			.add("arguments", getArguments())
			.add("difference", getDifference());

		return helper.toString();
	}

	public Integer getId(){
		return this.id;
	}

	private void setId(Integer id){
		this.id = id;
	}

	public Map<FieldName, ?> getArguments(){
		return this.arguments;
	}

	private void setArguments(Map<FieldName, ?> arguments){
		this.arguments = arguments;
	}

	public MapDifference<FieldName, ?> getDifference(){
		return this.difference;
	}

	private void setDifference(MapDifference<FieldName, ?> difference){
		this.difference = difference;
	}
}