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

import org.dmg.pmml.PMMLObject;
import org.jpmml.model.XPathUtil;

public class UnsupportedElementException extends UnsupportedMarkupException {

	public UnsupportedElementException(PMMLObject object){
		super("Element " + formatElement(object) + " is not supported", object);
	}

	static
	private String formatElement(PMMLObject object){
		Class<? extends PMMLObject> clazz = object.getClass();

		String result = XPathUtil.formatElement(clazz);

		String name = clazz.getName();
		if(!name.startsWith("org.dmg.pmml.")){
			result += (" (Java class " + name + ")");
		}

		return result;
	}
}