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

import com.google.common.collect.Lists;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.Visitable;
import org.dmg.pmml.VisitorAction;
import org.jpmml.manager.InvalidFeatureException;
import org.jpmml.manager.PMMLObjectUtil;
import org.jpmml.model.visitors.AbstractSimpleVisitor;

public class InvalidFeatureInspector extends AbstractSimpleVisitor {

	private List<InvalidFeatureException> exceptions = Lists.newArrayList();


	@Override
	public void applyTo(Visitable visitable){
		super.applyTo(visitable);

		List<InvalidFeatureException> exceptions = getExceptions();
		if(exceptions.size() > 0){
			throw exceptions.get(0);
		}
	}

	@Override
	public VisitorAction visit(PMMLObject object){
		Class<?> clazz = object.getClass();

		Field[] fields = clazz.getDeclaredFields();
		for(Field field : fields){
			Object value = PMMLObjectUtil.getFieldValue(object, field);

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

		return VisitorAction.CONTINUE;
	}

	private void report(InvalidFeatureException exception){
		this.exceptions.add(exception);
	}

	public List<InvalidFeatureException> getExceptions(){
		return this.exceptions;
	}
}