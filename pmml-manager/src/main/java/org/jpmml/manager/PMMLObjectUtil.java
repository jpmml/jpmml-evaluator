/*
 * Copyright (c) 2014 Villu Ruusmann
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
package org.jpmml.manager;

import java.lang.reflect.Field;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;

import org.dmg.pmml.PMMLObject;

public class PMMLObjectUtil {

	private PMMLObjectUtil(){
	}

	static
	public String getRootElementName(PMMLObject element){
		Class<?> clazz = element.getClass();

		XmlRootElement xmlRootElement = clazz.getAnnotation(XmlRootElement.class);
		if(xmlRootElement == null){
			throw new RuntimeException();
		}

		return xmlRootElement.name();
	}

	static
	public String getAttributeName(PMMLObject element, String name){
		Class<?> clazz = element.getClass();

		Field field;

		try {
			field = clazz.getDeclaredField(name);
		} catch(NoSuchFieldException nsfe){
			throw new RuntimeException(nsfe);
		}

		XmlAttribute xmlAttribute = field.getAnnotation(XmlAttribute.class);
		if(xmlAttribute == null){
			throw new RuntimeException();
		}

		return xmlAttribute.name();
	}

	@SuppressWarnings (
		value = {"unchecked"}
	)
	static
	public <E> E getAttributeValue(PMMLObject element, String name){
		Class<?> clazz = element.getClass();

		Field field;

		try {
			field = clazz.getDeclaredField(name);
		} catch(NoSuchFieldException nsfe){
			throw new RuntimeException(nsfe);
		}

		try {
			if(!field.isAccessible()){
				field.setAccessible(true);
			}

			return (E)field.get(element);
		} catch(IllegalAccessException iae){
			throw new RuntimeException(iae);
		}
	}

	static
	public String getValue(Enum<?> value){
		Class<?> clazz = value.getClass();

		Field field;

		try {
			field = clazz.getField(value.name());
		} catch(NoSuchFieldException nsfe){
			throw new RuntimeException(nsfe);
		}

		XmlEnumValue xmlEnumValue = field.getAnnotation(XmlEnumValue.class);
		if(xmlEnumValue == null){
			throw new RuntimeException();
		}

		return xmlEnumValue.value();
	}
}