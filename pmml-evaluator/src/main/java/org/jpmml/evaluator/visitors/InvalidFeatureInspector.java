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

import java.lang.reflect.Field;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.VisitorAction;
import org.jpmml.manager.InvalidFeatureException;
import org.jpmml.manager.PMMLObjectUtil;

public class InvalidFeatureInspector extends FeatureInspector<InvalidFeatureException> {

	@Override
	public VisitorAction visit(PMMLObject object){
		Class<?> clazz = object.getClass();

		Field[] fields = clazz.getDeclaredFields();
		for(Field field : fields){
			Object value = PMMLObjectUtil.getFieldValue(object, field);

			if(value instanceof List){
				List<?> collection = (List<?>)value;

				// The getter method may have initialized the field with an empty ArrayList instance
				if(collection.size() == 0){
					value = null;
				}
			} // End if

			// The field is set
			if(value != null){
				continue;
			}

			XmlElement element = field.getAnnotation(XmlElement.class);
			if(element != null && element.required()){
				report(new InvalidFeatureException(object, field));
			}

			XmlAttribute attribute = field.getAnnotation(XmlAttribute.class);
			if(attribute != null && attribute.required()){
				report(new InvalidFeatureException(object, field));
			}
		}

		return super.visit(object);
	}
}