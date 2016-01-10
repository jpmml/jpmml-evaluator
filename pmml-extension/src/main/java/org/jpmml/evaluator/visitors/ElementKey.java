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
package org.jpmml.evaluator.visitors;

import java.util.Arrays;

class ElementKey {

	private Object[] content = null;


	ElementKey(Object[] content){
		setContent(content);
	}

	@Override
	public int hashCode(){
		Object[] content = getContent();

		return Arrays.hashCode(content);
	}

	@Override
	public boolean equals(Object object){

		if(object instanceof ElementKey){
			ElementKey that = (ElementKey)object;

			return Arrays.equals(this.getContent(), that.getContent());
		}

		return false;
	}

	public Object[] getContent(){
		return this.content;
	}

	private void setContent(Object[] content){
		this.content = content;
	}

	public static final ElementKey EMPTY = new ElementKey(new Object[0]);
}