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
package org.jpmml.evaluator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.DefineFunction;
import org.dmg.pmml.FieldName;

abstract
public class EvaluationContext {

	private Map<FieldName, FieldValue> fields = new HashMap<>();

	private List<String> warnings = null;


	EvaluationContext(){
	}

	abstract
	protected FieldValue createFieldValue(FieldName name, Object value);

	protected void reset(){
		this.fields.clear();

		if(this.warnings != null){
			this.warnings.clear();
		}
	}

	public FieldValue evaluate(FieldName name){
		Map<FieldName, FieldValue> fields = getFields();

		if(fields.size() > 0){
			FieldValue value = fields.get(name);

			if((value != null) || (value == null && fields.containsKey(name))){
				return value;
			}
		}

		return resolve(name);
	}

	protected FieldValue resolve(FieldName name){
		throw new MissingFieldException(name);
	}

	public FieldValue getField(FieldName name){
		Map<FieldName, FieldValue> fields = getFields();

		return fields.get(name);
	}

	public FieldValue declare(FieldName name, Object value){

		if(value instanceof FieldValue){
			return declare(name, (FieldValue)value);
		}

		return declare(name, createFieldValue(name, value));
	}

	public FieldValue declare(FieldName name, FieldValue value){
		Map<FieldName, FieldValue> fields = getFields();

		boolean declared = fields.containsKey(name);
		if(declared){
			throw new DuplicateValueException(name);
		}

		fields.put(name, value);

		return value;
	}

	public void declareAll(Map<FieldName, ?> values){
		Collection<? extends Map.Entry<FieldName, ?>> entries = values.entrySet();

		for(Map.Entry<FieldName, ?> entry : entries){
			declare(entry.getKey(), entry.getValue());
		}
	}

	protected DefineFunction getDefineFunction(String name){
		throw new UnsupportedOperationException();
	}

	public void addWarning(String warning){

		if(this.warnings == null){
			this.warnings = new ArrayList<>();
		}

		this.warnings.add(warning);
	}

	public Map<FieldName, FieldValue> getFields(){
		return this.fields;
	}

	public List<String> getWarnings(){

		if(this.warnings == null){
			return Collections.emptyList();
		}

		return this.warnings;
	}
}