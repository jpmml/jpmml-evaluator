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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.DataType;
import org.dmg.pmml.DefineFunction;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OpType;

abstract
public class EvaluationContext {

	private Map<FieldName, FieldValue> values = new HashMap<>();

	private FieldName[] indexedNames = null;

	private FieldValue[] indexedValues = null;

	private List<String> warnings = null;


	EvaluationContext(){
	}

	abstract
	protected FieldValue prepare(FieldName name, Object value);

	protected void reset(boolean clearValues){

		if(clearValues){
			this.values.clear();

			this.indexedNames = null;
			this.indexedValues = null;
		} // End if

		if(this.warnings != null){
			this.warnings.clear();
		}
	}

	public void setIndex(List<FieldName> names){

		if(names == null){
			this.indexedNames = null;
			this.indexedValues = null;

			return;
		}

		this.indexedNames = names.toArray(new FieldName[names.size()]);
		this.indexedValues = new FieldValue[names.size()];

		Arrays.fill(this.indexedValues, EvaluationContext.UNDECLARED_VALUE);
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

		FieldValue value = values.getOrDefault(name, EvaluationContext.UNDECLARED_VALUE);
		if(value != EvaluationContext.UNDECLARED_VALUE){
			return value;
		}

		throw new MissingValueException(name);
	}

	/**
	 * <p>
	 * Looks up a field value by name.
	 * If the field value has not been declared, then makes full effort to resolve and declare it.
	 * </p>
	 */
	public FieldValue evaluate(FieldName name){
		Map<FieldName, FieldValue> values = getValues();

		FieldValue value = values.getOrDefault(name, EvaluationContext.UNDECLARED_VALUE);
		if(value != EvaluationContext.UNDECLARED_VALUE){
			return value;
		}

		return resolve(name);
	}

 	/**
 	 * <p>
	 * Looks up a field value by name index.
	 * </p>
	 */
	public FieldValue evaluate(int index){

		if(this.indexedNames == null){
			throw new IllegalStateException();
		}

		FieldValue value = this.indexedValues[index];
		if(value == EvaluationContext.UNDECLARED_VALUE){
			FieldName name = this.indexedNames[index];

			value = evaluate(name);

			this.indexedValues[index] = value;
		}

		return value;
	}

	public List<FieldValue> evaluateAll(List<FieldName> names){
		List<FieldValue> values = new ArrayList<>(names.size());

		for(FieldName name : names){
			FieldValue value = evaluate(name);

			values.add(value);
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

		// XXX: Fails to detect a situation where the name was already associated with a missing value (null)
		FieldValue prevValue = values.putIfAbsent(name, value);
		if(prevValue != null){
			throw new DuplicateValueException(name);
		}

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

	private static final FieldValue UNDECLARED_VALUE = new ScalarValue(DataType.DOUBLE, Double.NaN){

		@Override
		public OpType getOpType(){
			return OpType.CONTINUOUS;
		}
	};
}