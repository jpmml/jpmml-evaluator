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

import org.dmg.pmml.FieldName;

public class FieldNameTable extends HashSet<FieldName> implements SymbolTable<FieldName> {

	private int capacity = Integer.MAX_VALUE;


	public FieldNameTable(){
	}

	public FieldNameTable(int capacity){
		setCapacity(capacity);
	}

	@Override
	public void lock(FieldName name){
		int capacity = getCapacity();

		int size = size();
		if(size >= capacity){
			throw new EvaluationException("Field reference chain is too long");
		}

		boolean unique = add(name);
		if(!unique){
			throw new EvaluationException("Field " + EvaluationException.formatKey(name) + " references itself");
		}
	}

	@Override
	public void release(FieldName name){
		remove(name);
	}

	public int getCapacity(){
		return this.capacity;
	}

	private void setCapacity(int capacity){

		if(capacity < 0){
			throw new IllegalArgumentException();
		}

		this.capacity = capacity;
	}
}