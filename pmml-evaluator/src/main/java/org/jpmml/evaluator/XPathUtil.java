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
package org.jpmml.evaluator;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.dmg.pmml.PMMLObject;

public class XPathUtil {

	private XPathUtil(){
	}

	static
	public String formatXPath(PMMLObject object){
		Class<?> clazz = object.getClass();

		return getElementName(clazz);
	}

	static
	public String formatXPath(PMMLObject object, Field field){
		XmlElement element = field.getAnnotation(XmlElement.class);
		if(element != null){
			Class<?> elementClazz = field.getType();

			if(List.class.isAssignableFrom(elementClazz)){
				ParameterizedType listType = (ParameterizedType)field.getGenericType();

				Type[] typeArguments = listType.getActualTypeArguments();
				if(typeArguments.length != 1){
					throw new RuntimeException();
				}

				elementClazz = (Class<?>)typeArguments[0];
			}

			return formatXPath(object) + "/" + getElementName(elementClazz);
		}

		XmlAttribute attribute = field.getAnnotation(XmlAttribute.class);
		if(attribute != null){
			return formatXPath(object) + "@" + attribute.name();
		}

		throw new RuntimeException();
	}

	static
	public String formatXPath(PMMLObject object, Field field, Object value){
		XmlAttribute attribute = field.getAnnotation(XmlAttribute.class);
		if(attribute != null){
			return formatXPath(object, field) + (value != null ? ("=" + String.valueOf(value)) : "");
		}

		throw new RuntimeException();
	}

	static
	private String getElementName(Class<?> clazz){

		while(clazz != null){
			XmlRootElement rootElement = clazz.getAnnotation(XmlRootElement.class);

			if(rootElement != null){
				return rootElement.name();
			}

			clazz = clazz.getSuperclass();
		}

		throw new RuntimeException();
	}
}