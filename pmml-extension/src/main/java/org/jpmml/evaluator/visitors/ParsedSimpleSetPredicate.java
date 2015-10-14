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
package org.jpmml.evaluator.visitors;

import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.dmg.pmml.Array;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Extension;
import org.dmg.pmml.OpType;
import org.dmg.pmml.SimpleSetPredicate;
import org.jpmml.evaluator.ArrayUtil;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.HasValueSet;

class ParsedSimpleSetPredicate extends SimpleSetPredicate implements HasValueSet {

	private Set<FieldValue> parsedValueSet = null;


	ParsedSimpleSetPredicate(SimpleSetPredicate simpleSetPredicate){
		setField(simpleSetPredicate.getField());
		setBooleanOperator(simpleSetPredicate.getBooleanOperator());
		setArray(simpleSetPredicate.getArray());

		if(simpleSetPredicate.hasExtensions()){
			List<Extension> extensions = getExtensions();

			extensions.addAll(simpleSetPredicate.getExtensions());
		}
	}

	@Override
	public Set<FieldValue> getValueSet(final DataType dataType, final OpType opType){

		if(this.parsedValueSet == null){
			Array array = getArray();

			List<String> content = ArrayUtil.getContent(array);

			Function<String, FieldValue> function = new Function<String, FieldValue>(){

				@Override
				public FieldValue apply(String value){
					return FieldValueUtil.create(dataType, opType, value);
				}
			};

			this.parsedValueSet = ImmutableSet.copyOf(Iterables.transform(content, function));
		}

		return this.parsedValueSet;
	}
}