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

import java.util.*;

import org.dmg.pmml.*;

import com.google.common.collect.*;

abstract
public class EvaluationContext {

	private Map<FieldName, FieldValue> fields = Maps.newLinkedHashMap();

	private List<String> warnings = Lists.newArrayList();


	abstract
	public DerivedField resolveDerivedField(FieldName name);

	abstract
	public DefineFunction resolveFunction(String name);

	/**
	 * @see #getFieldEntry(FieldName)
	 */
	public FieldValue getField(FieldName name){
		Map.Entry<FieldName, FieldValue> entry = getFieldEntry(name);
		if(entry != null){
			return entry.getValue();
		}

		return null;
	}

	public Map.Entry<FieldName, FieldValue> getFieldEntry(FieldName name){
		Map<FieldName, FieldValue> fields = getFields();

		if(fields.containsKey(name)){
			Map.Entry<FieldName, FieldValue> entry = new AbstractMap.SimpleEntry<FieldName, FieldValue>(name, fields.get(name));

			return entry;
		}

		return null;
	}

	public void declare(FieldName name, Object value){

		if(value instanceof FieldValue){
			declare(name, (FieldValue)value);

			return;
		}

		declare(name, createFieldValue(name, value));
	}

	public void declare(FieldName name, FieldValue value){
		Map<FieldName, FieldValue> fields = getFields();

		boolean declared = fields.containsKey(name);
		if(declared){
			throw new DuplicateFieldException(name);
		}

		fields.put(name, value);
	}

	public void declareAll(Map<FieldName, ?> fields){
		Collection<? extends Map.Entry<FieldName, ?>> entries = fields.entrySet();

		for(Map.Entry<FieldName, ?> entry : entries){
			declare(entry.getKey(), entry.getValue());
		}
	}

	public FieldValue createFieldValue(FieldName name, Object value){
		return FieldValueUtil.create(value);
	}

	public void addWarning(String warning){
		List<String> warnings = getWarnings();

		warnings.add(warning);
	}

	public Map<FieldName, FieldValue> getFields(){
		return this.fields;
	}

	public List<String> getWarnings(){
		return this.warnings;
	}
}