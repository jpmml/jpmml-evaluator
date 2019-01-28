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
package org.jpmml.evaluator.regression;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.dmg.pmml.regression.CategoricalPredictor;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.HasParsedValue;
import org.jpmml.evaluator.MissingAttributeException;
import org.jpmml.evaluator.PMMLAttributes;
import org.jpmml.evaluator.TypeInfo;
import org.jpmml.model.ReflectionUtil;

@XmlRootElement (
	name = "CategoricalPredictor"
)
public class RichCategoricalPredictor extends CategoricalPredictor implements HasParsedValue<CategoricalPredictor> {

	@XmlTransient
	private FieldValue parsedValue = null;


	public RichCategoricalPredictor(){
	}

	public RichCategoricalPredictor(CategoricalPredictor categoricalPredictor){
		ReflectionUtil.copyState(categoricalPredictor, this);
	}

	@Override
	public FieldValue getValue(TypeInfo typeInfo){

		if(this.parsedValue == null){
			String value = getValue();
			if(value == null){
				throw new MissingAttributeException(this, PMMLAttributes.CATEGORICALPREDICTOR_FIELD);
			}

			this.parsedValue = parse(typeInfo, value);
		}

		return this.parsedValue;
	}
}