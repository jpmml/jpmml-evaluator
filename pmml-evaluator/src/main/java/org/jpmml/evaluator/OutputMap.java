/*
 * Copyright (c) 2019 Villu Ruusmann
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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

class OutputMap extends LinkedHashMap<String, Object> {

	private Map<String, ?> predictions = null;

	private Set<String> privateFields = null;


	OutputMap(Map<String, ?> predictions){
		super(predictions);

		setPredictions(predictions);
	}

	public Object putPublic(String name, Object value){
		return put(name, value);
	}

	public Object putPrivate(String name, Object value){

		if(this.privateFields == null){
			this.privateFields = new HashSet<>();
		}

		this.privateFields.add(name);

		return put(name, value);
	}

	public void clearPrivate(){

		if(this.privateFields != null && !this.privateFields.isEmpty()){
			Set<String> fields = keySet();

			fields.removeAll(this.privateFields);
		}

		this.privateFields = null;
	}

	public Map<String, ?> getPredictions(){
		return this.predictions;
	}

	private void setPredictions(Map<String, ?> predictions){
		this.predictions = predictions;
	}
}