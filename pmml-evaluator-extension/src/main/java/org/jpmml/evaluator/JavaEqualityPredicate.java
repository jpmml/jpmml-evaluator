/*
 * Copyright (c) 2018 Villu Ruusmann
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

import java.util.Objects;

import org.jpmml.evaluator.functions.EqualityFunction;

/**
 * @see EqualityFunction
 */
abstract
public class JavaEqualityPredicate extends JavaSimplePredicate {

	private Object value = null;


	JavaEqualityPredicate(int index, Object value){
		super(index);

		setValue(value);
	}

	abstract
	public Boolean evaluate(boolean equals);

	@Override
	public Boolean evaluate(EvaluationContext context){
		FieldValue value = context.evaluate(getIndex());

		if(FieldValueUtil.isMissing(value)){
			return null;
		}

		return evaluate(value.equalsValue(getValue()));
	}

	public Object getValue(){
		return this.value;
	}

	private void setValue(Object value){
		this.value = Objects.requireNonNull(value);
	}

	static
	public class Equal extends JavaEqualityPredicate {

		public Equal(int index, Object value){
			super(index, value);
		}

		@Override
		public Boolean evaluate(boolean equals){
			return Boolean.valueOf(equals);
		}
	}

	static
	public class NotEqual extends JavaEqualityPredicate {

		public NotEqual(int index, Object value){
			super(index, value);
		}

		@Override
		public Boolean evaluate(boolean equals){
			return Boolean.valueOf(!equals);
		}
	}
}