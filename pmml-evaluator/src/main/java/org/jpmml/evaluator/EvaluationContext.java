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

	private Map<FieldName, FieldValue> values = new HashMap<>();

	private List<FieldName> cachedNames = null;

	private List<FieldValue> cachedValues = null;

	private List<String> warnings = null;


	EvaluationContext(){
	}

	abstract
	protected FieldValue prepare(FieldName name, Object value);

	protected void reset(boolean clearValues){

		if(clearValues){

			if(!this.values.isEmpty()){
				this.values.clear();
			}

			this.cachedNames = null;
			this.cachedValues = null;
		} // End if

		if(this.warnings != null && !this.warnings.isEmpty()){
			this.warnings.clear();
		}
	}

	/**
	 * <p>
	 * Looks up a field value by name.
	 * If the field value has not been declared, then fails fast with an exception.
	 * </p>
	 *
	 * @throws MissingValueException If the field value has not been declared.
	 */
	public FieldValue lookup(FieldName name){
		Map<FieldName, FieldValue> values = getValues();

		FieldValue value = values.get(name);
		if(value != null || (value == null && values.containsKey(name))){
			return value;
		}

		throw new MissingValueException(name);
	}

	/**
	 * <p>
	 * Looks up a field value by field value cache position.
	 * </p>
	 *
	 * @throws IllegalStateException If the field value cache is not available.
	 * @throws IndexOutOfBoundsException If the field value cache position is out of range.
	 */
	public FieldValue lookup(int index){
		List<FieldValue> cachedValues = this.cachedValues;

		if(cachedValues == null){
			throw new IllegalStateException();
		}

		return cachedValues.get(index);
	}

	/**
	 * <p>
	 * Looks up a field value by name.
	 * If the field value has not been declared, then makes full effort to resolve and declare it.
	 * </p>
	 */
	public FieldValue evaluate(FieldName name){
		Map<FieldName, FieldValue> values = getValues();

		FieldValue value = values.get(name);
		if(value != null || (value == null && values.containsKey(name))){
			return value;
		}

		return resolve(name);
	}

	public List<FieldValue> evaluateAll(List<FieldName> names){
		return evaluateAll(names, true);
	}

	public List<FieldValue> evaluateAll(List<FieldName> names, boolean cache){
		this.cachedNames = null;
		this.cachedValues = null;

		List<FieldValue> values = new ArrayList<>(names.size());

		for(FieldName name : names){
			FieldValue value = evaluate(name);

			values.add(value);
		}

		if(cache){
			this.cachedNames = names;
			this.cachedValues = values;
		}

		return values;
	}

	protected FieldValue resolve(FieldName name){
		throw new MissingFieldException(name);
	}

	public FieldValue declare(FieldName name, Object value){

		if(value instanceof FieldValue){
			return declare(name, (FieldValue)value);
		}

		value = prepare(name, value);

		return declare(name, (FieldValue)value);
	}

	public FieldValue declare(FieldName name, FieldValue value){
		Map<FieldName, FieldValue> values = getValues();

		boolean declared = values.containsKey(name);
		if(declared){
			throw new DuplicateValueException(name);
		}

		values.put(name, value);

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

	public Map<FieldName, FieldValue> getValues(){
		return this.values;
	}

	public List<String> getWarnings(){

		if(this.warnings == null){
			return Collections.emptyList();
		}

		return this.warnings;
	}
}