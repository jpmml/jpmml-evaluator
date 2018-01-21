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

import javax.xml.bind.annotation.XmlEnumValue;

import org.dmg.pmml.PMMLObject;

public class EnumUtil {

	private EnumUtil(){
	}

	static
	public Field getEnumField(PMMLObject object, Enum<?> value){
		Class<?> clazz = object.getClass();

		Field[] fields = clazz.getDeclaredFields();
		for(Field field : fields){

			if((field.getType()).equals(value.getClass())){
				return field;
			}
		}

		throw new IllegalArgumentException();
	}

	static
	public String getEnumValue(Enum<?> value){
		Class<?> clazz = value.getClass();

		Field field;

		try {
			field = clazz.getField(value.name());
		} catch(NoSuchFieldException nsfe){
			throw new RuntimeException(nsfe);
		}

		XmlEnumValue enumValue = field.getAnnotation(XmlEnumValue.class);
		if(enumValue != null){
			return enumValue.value();
		}

		throw new IllegalArgumentException();
	}
}