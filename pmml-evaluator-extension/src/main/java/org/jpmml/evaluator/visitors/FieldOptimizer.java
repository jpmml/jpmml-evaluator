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
import java.util.ListIterator;

import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.VisitorAction;
import org.jpmml.evaluator.RichDataField;
import org.jpmml.model.visitors.AbstractVisitor;

public class FieldOptimizer extends AbstractVisitor {

	@Override
	public VisitorAction visit(DataDictionary dataDictionary){

		if(dataDictionary.hasDataFields()){
			List<DataField> dataFields = dataDictionary.getDataFields();

			for(ListIterator<DataField> it = dataFields.listIterator(); it.hasNext(); ){
				it.set(new RichDataField(it.next()));
			}
		}

		return super.visit(dataDictionary);
	}
}