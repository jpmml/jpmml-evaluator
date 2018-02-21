/*
 * Copyright (c) 2015 Villu Ruusmann
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
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.google.common.collect.ImmutableSet;
import org.dmg.pmml.Array;
import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.dmg.pmml.SimpleSetPredicate;
import org.jpmml.model.ReflectionUtil;

@XmlRootElement (
	name = "SimpleSetPredicate"
)
public class RichSimpleSetPredicate extends SimpleSetPredicate implements HasParsedValueSet<SimpleSetPredicate> {

	@XmlTransient
	private Set<FieldValue> parsedValueSet = null;


	public RichSimpleSetPredicate(){
	}

	public RichSimpleSetPredicate(SimpleSetPredicate simpleSetPredicate){
		ReflectionUtil.copyState(simpleSetPredicate, this);
	}

	@Override
	public Set<FieldValue> getValueSet(DataType dataType, OpType opType){

		if(this.parsedValueSet == null){
			this.parsedValueSet = ImmutableSet.copyOf(parseArray(dataType, opType));
		}

		return this.parsedValueSet;
	}

	private List<FieldValue> parseArray(DataType dataType, OpType opType){
		Array array = getArray();
		if(array == null){
			throw new MissingElementException(this, PMMLElements.SIMPLESETPREDICATE_ARRAY);
		}

		List<?> content = ArrayUtil.getContent(array);

		return parseAll(dataType, opType, content);
	}
}