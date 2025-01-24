/*
 * Copyright (c) 2020 Villu Ruusmann
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
package org.jpmml.evaluator.kryo;

import java.util.Objects;

public class PreHashedValue {

	private int hashCode;

	private Object value;


	private PreHashedValue(){
	}

	public PreHashedValue(int hashCode, Object value){
		this.hashCode = hashCode;
		this.value = Objects.requireNonNull(value);
	}

	@Override
	public int hashCode(){
		return this.hashCode;
	}

	@Override
	public boolean equals(Object object){

		if(object instanceof PreHashedValue){
			PreHashedValue that = (PreHashedValue)object;

			return Objects.equals(this.value, that.value);
		}

		return false;
	}
}