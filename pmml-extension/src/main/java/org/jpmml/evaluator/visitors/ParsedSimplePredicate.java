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

import org.dmg.pmml.DataType;
import org.dmg.pmml.Extension;
import org.dmg.pmml.OpType;
import org.dmg.pmml.SimplePredicate;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.HasValue;

class ParsedSimplePredicate extends SimplePredicate implements HasValue {

	private FieldValue parsedValue = null;


	ParsedSimplePredicate(SimplePredicate simplePredicate){
		setField(simplePredicate.getField());
		setOperator(simplePredicate.getOperator());
		setValue(simplePredicate.getValue());

		if(simplePredicate.hasExtensions()){
			List<Extension> extensions = getExtensions();

			extensions.addAll(simplePredicate.getExtensions());
		}
	}

	@Override
	public FieldValue getValue(DataType dataType, OpType opType){

		if(this.parsedValue == null){
			this.parsedValue = FieldValueUtil.create(dataType, opType, getValue());
		}

		return this.parsedValue;
	}
}