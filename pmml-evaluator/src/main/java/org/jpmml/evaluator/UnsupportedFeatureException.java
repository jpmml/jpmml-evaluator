/*
 * Copyright (c) 2012 University of Tartu
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jpmml.evaluator;

import java.lang.reflect.Field;

import javax.xml.bind.annotation.XmlEnumValue;

import org.dmg.pmml.PMMLObject;

/**
 * <p>
 * Thrown to indicate that the class model object is not supported.
 * </p>
 *
 * @see InvalidFeatureException
 */
public class UnsupportedFeatureException extends PMMLException {

	public UnsupportedFeatureException(){
		super();
	}

	public UnsupportedFeatureException(String message){
		super(message);
	}

	public UnsupportedFeatureException(PMMLObject object){
		this(XPathUtil.formatXPath(object), object);
	}

	public UnsupportedFeatureException(PMMLObject object, Field field){
		this(XPathUtil.formatXPath(object, field), object);
	}

	public UnsupportedFeatureException(PMMLObject object, Field field, Object value){
		this(XPathUtil.formatXPath(object, field, value), object);
	}

	public UnsupportedFeatureException(PMMLObject object, Enum<?> value){
		this(object, getEnumField(object, value), getEnumValue(value));
	}

	public UnsupportedFeatureException(String message, PMMLObject context){
		super(message, context);
	}

	static
	private Field getEnumField(PMMLObject element, Enum<?> value){
		Class<?> clazz = element.getClass();

		Field[] fields = clazz.getDeclaredFields();
		for(Field field : fields){

			if((field.getType()).equals(value.getClass())){
				return field;
			}
		}

		throw new RuntimeException();
	}

	static
	private String getEnumValue(Enum<?> value){
		Class<?> clazz = value.getClass();

		try {
			Field field = clazz.getField(value.name());

			XmlEnumValue xmlEnumValue = field.getAnnotation(XmlEnumValue.class);
			if(xmlEnumValue != null){
				return xmlEnumValue.value();
			}
		} catch(NoSuchFieldException nsfe){
			throw new RuntimeException(nsfe);
		}

		throw new RuntimeException();
	}
}