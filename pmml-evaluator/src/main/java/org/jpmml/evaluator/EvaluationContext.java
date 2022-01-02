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
import java.util.List;
import java.util.Map;

import org.dmg.pmml.DataType;
import org.dmg.pmml.DefineFunction;
import org.dmg.pmml.OpType;

abstract
public class EvaluationContext {

	private FieldValueMap values = null;

	private List<String> warnings = null;


	EvaluationContext(){
		this.values = FieldValueMap.create();
	}

	EvaluationContext(int numberOfVisibleFields){
		this.values = FieldValueMap.create(numberOfVisibleFields);
	}

	abstract
	protected FieldValue prepare(String name, Object value);

	protected void reset(boolean clearValues){

		if(clearValues){
			this.values.clear();
		} // End if

		if(this.warnings != null){
			this.warnings.clear();
		}
	}

	/**
	 * <p>
	 * Looks up a field value by name.
	 * If the field value has not been declared, then fails fast with an exception.
	 * </p>
	 *
	 * @throws MissingFieldValueException If the field value has not been declared.
	 */
	public FieldValue lookup(String name){
		FieldValueMap values = getValues();

		FieldValue value = values.getOrDefault(name, EvaluationContext.UNDECLARED_VALUE);
		if(value != EvaluationContext.UNDECLARED_VALUE){
			return value;
		}

		throw new MissingFieldValueException(name);
	}

	/**
	 * <p>
	 * Looks up a field value by name.
	 * If the field value has not been declared, then makes full effort to resolve and declare it.
	 * </p>
	 */
	public FieldValue evaluate(String name){
		FieldValueMap values = getValues();

		FieldValue value = values.getOrDefault(name, EvaluationContext.UNDECLARED_VALUE);
		if(value != EvaluationContext.UNDECLARED_VALUE){
			return value;
		}

		return resolve(name);
	}

	public List<FieldValue> evaluateAll(List<String> names){
		List<FieldValue> values = new ArrayList<>(names.size());

		for(String name : names){
			FieldValue value = evaluate(name);

			values.add(value);
		}

		return values;
	}

	protected FieldValue resolve(String name){
		throw new MissingFieldException(name);
	}

	public FieldValue declare(String name, Object value){

		if(value instanceof FieldValue){
			return declare(name, (FieldValue)value);
		}

		value = prepare(name, value);

		return declare(name, (FieldValue)value);
	}

	public FieldValue declare(String name, FieldValue value){
		FieldValueMap values = getValues();

		// XXX: Fails to detect a situation where the name was already associated with a missing value (null)
		FieldValue prevValue = values.putIfAbsent(name, value);
		if(prevValue != null){
			throw new DuplicateFieldValueException(name);
		}

		return value;
	}

	public void declareAll(Map<String, ?> values){
		Collection<? extends Map.Entry<String, ?>> entries = values.entrySet();

		for(Map.Entry<String, ?> entry : entries){
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

	public FieldValueMap getValues(){
		return this.values;
	}

	public List<String> getWarnings(){

		if(this.warnings == null){
			return Collections.emptyList();
		}

		return this.warnings;
	}

	private static final FieldValue UNDECLARED_VALUE = new ScalarValue(DataType.DOUBLE, Double.NaN){

		{
			setValid(false);
		}

		@Override
		public OpType getOpType(){
			return OpType.CONTINUOUS;
		}
	};

	public static final ThreadLocal<SymbolTable<String>> DERIVEDFIELD_GUARD_PROVIDER = new ThreadLocal<SymbolTable<String>>(){

		@Override
		public SymbolTable<String> initialValue(){
			return null;
		}
	};

	public static final ThreadLocal<SymbolTable<String>> FUNCTION_GUARD_PROVIDER = new ThreadLocal<SymbolTable<String>>(){

		@Override
		public SymbolTable<String> initialValue(){
			return null;
		}
	};
}