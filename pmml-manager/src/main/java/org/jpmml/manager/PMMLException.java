/*
 * Copyright (c) 2013 Villu Ruusmann
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

import com.sun.xml.bind.Locatable;
import org.dmg.pmml.PMMLObject;
import org.xml.sax.Locator;

abstract
public class PMMLException extends RuntimeException {

	private PMMLObject context = null;


	public PMMLException(){
		super();
	}

	public PMMLException(String message){
		super(message);
	}

	public PMMLException(PMMLObject context){
		super();

		setContext(context);
	}

	public PMMLException(String message, PMMLObject context){
		super(message);

		setContext(context);
	}

	public PMMLObject getContext(){
		return this.context;
	}

	private void setContext(PMMLObject context){
		this.context = context;
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getName());

		Locatable locatable = getContext();
		if(locatable != null){
			int lineNumber = -1;

			Locator locator = locatable.sourceLocation();
			if(locator != null){
				lineNumber = locator.getLineNumber();
			}

			if(lineNumber != -1){
				sb.append(" ").append("(at or around line ").append(lineNumber).append(")");
			}
		}

		String message = getLocalizedMessage();
		if(message != null){
			sb.append(":");

			sb.append(" ").append(message);
		}

		return sb.toString();
	}

	static
	public String formatElement(Class<?> clazz){
		XmlRootElement xmlRootElement = clazz.getAnnotation(XmlRootElement.class);
		if(xmlRootElement == null){
			throw new RuntimeException();
		}

		return xmlRootElement.name();
	}

	static
	public String formatAttribute(Class<?> clazz, String name){
		Field field;

		try {
			field = clazz.getDeclaredField(name);
		} catch(NoSuchFieldException nsfe){
			throw new RuntimeException();
		}

		XmlAttribute xmlAttribute = field.getAnnotation(XmlAttribute.class);
		if(xmlAttribute == null){
			throw new RuntimeException();
		}

		return xmlAttribute.name();
	}

	static
	public String formatValue(Enum<?> value){
		Class<?> clazz = value.getClass();

		Field field;

		try {
			field = clazz.getField(value.name());
		} catch(NoSuchFieldException nfse){
			throw new RuntimeException();
		}

		XmlEnumValue xmlEnumValue = field.getAnnotation(XmlEnumValue.class);
		if(xmlEnumValue == null){
			throw new RuntimeException();
		}

		return xmlEnumValue.value();
	}
}