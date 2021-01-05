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

import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.xml.bind.annotation.XmlTransient;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.dmg.pmml.ComplexArray;
import org.dmg.pmml.DataType;
import org.jpmml.model.annotations.Property;

public class RichComplexArray extends ComplexArray implements SetHolder {

	@XmlTransient
	private DataType dataType = null;


	private RichComplexArray(){
	}

	public RichComplexArray(DataType dataType){
		setDataType(dataType);
	}

	@Override
	public Set<?> getSet(){
		return (Set<?>)getValue();
	}

	@Override
	public RichComplexArray setValue(List<?> values){
		throw new UnsupportedOperationException();
	}

	@Override
	public RichComplexArray setValue(Set<?> values){
		DataType dataType = getDataType();

		Function<Object, Object> function = new Function<Object, Object>(){

			@Override
			public Object apply(Object value){
				return TypeUtil.parseOrCast(dataType, value);
			}
		};

		values = Sets.newHashSet(Iterables.transform(values, function));

		return (RichComplexArray)super.setValue(values);
	}

	@Override
	public RichComplexArray setValue(@Property("value") Object value){
		throw new UnsupportedOperationException();
	}

	@Override
	public DataType getDataType(){
		return this.dataType;
	}

	private void setDataType(DataType dataType){
		this.dataType = Objects.requireNonNull(dataType);
	}
}