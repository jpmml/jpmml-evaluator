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

import java.util.ArrayDeque;
import java.util.Objects;

public class FunctionNameStack extends ArrayDeque<String> implements SymbolTable<String> {

	private int capacity = 16;


	public FunctionNameStack(){
	}

	public FunctionNameStack(int capacity){
		setCapacity(capacity);
	}

	public FunctionNameStack(FunctionNameStack parent){
		super(parent);

		setCapacity(parent.getCapacity());
	}

	@Override
	public FunctionNameStack fork(){
		return new FunctionNameStack(this);
	}

	@Override
	public void lock(String name){
		int capacity = getCapacity();

		int size = size();
		if(size >= capacity){
			throw new EvaluationException("Function call stack is too high");
		}

		push(name);
	}

	@Override
	public void release(String name){
		String tail = pop();

		if(!Objects.equals(name, tail)){
			throw new IllegalStateException();
		}
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