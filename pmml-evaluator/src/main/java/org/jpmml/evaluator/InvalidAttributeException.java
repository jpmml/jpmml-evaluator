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

import java.lang.reflect.Field;

import org.dmg.pmml.PMMLObject;
import org.jpmml.model.EnumUtil;
import org.jpmml.model.XPathUtil;

public class InvalidAttributeException extends InvalidMarkupException {

	public InvalidAttributeException(String message){
		super(message);
	}

	public InvalidAttributeException(String message, PMMLObject context){
		super(message, context);
	}

	public InvalidAttributeException(PMMLObject object, Enum<?> value){
		this(object, EnumUtil.getEnumField(object, value), EnumUtil.getEnumValue(value));
	}

	public InvalidAttributeException(PMMLObject object, Field field, Object value){
		super(formatMessage(XPathUtil.formatAttribute(field, value)), object);
	}

	static
	public String formatMessage(String xPath){
		return "Attribute with value " + xPath + " is not valid";
	}
}