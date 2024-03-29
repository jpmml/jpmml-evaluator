/*
 * Copyright (c) 2016 Villu Ruusmann
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
package org.jpmml.evaluator.example;

import java.lang.reflect.Field;
import java.util.Comparator;

import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.Parameterized;

class ParameterOrderComparator implements Comparator<ParameterDescription> {

	@Override
	public int compare(ParameterDescription left, ParameterDescription right){
		int leftOrder = getParameterOrder(left);
		int rightOrder = getParameterOrder(right);

		if(leftOrder > -1 || rightOrder > -1){
			return Integer.compare(leftOrder, rightOrder);
		}

		return (left.getLongestName()).compareToIgnoreCase(right.getLongestName());
	}

	@SuppressWarnings("deprecation")
	private int getParameterOrder(ParameterDescription parameterDescription){
		Field parameterField;

		try {
			Parameterized parameterized = parameterDescription.getParameterized();

			Field fieldField = Parameterized.class.getDeclaredField("field");
			if(!fieldField.isAccessible()){
				fieldField.setAccessible(true);
			}

			parameterField = (Field)fieldField.get(parameterized);
		} catch(ReflectiveOperationException roe){
			throw new RuntimeException(roe);
		}

		if(parameterField != null){
			ParameterOrder parameterOrder = parameterField.getAnnotation(ParameterOrder.class);

			if(parameterOrder != null){
				return parameterOrder.value();
			}
		}

		return -1;
	}
}