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
package org.jpmml.evaluator;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMMLObject;
import org.xml.sax.Locator;

abstract
public class PMMLException extends RuntimeException {

	private PMMLObject context = null;


	public PMMLException(String message){
		super(message);
	}

	public PMMLException(String message, PMMLObject context){
		super(message);

		setContext(context);
	}

	@Override
	synchronized
	public PMMLException initCause(Throwable throwable){
		return (PMMLException)super.initCause(throwable);
	}

	public PMMLException ensureContext(PMMLObject parentContext){
		PMMLObject context = getContext();

		if(context == null){
			setContext(parentContext);
		}

		return this;
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();

		Class<? extends PMMLException> clazz = getClass();
		sb.append(clazz.getName());

		PMMLObject context = getContext();
		if(context != null){
			int lineNumber = -1;

			Locator locator = context.getLocator();
			if(locator != null){
				lineNumber = locator.getLineNumber();
			} // End if

			if(lineNumber != -1){
				sb.append(" ").append("(at or around line ").append(lineNumber).append(" of the PMML document)");
			}
		}

		String message = getLocalizedMessage();
		if(message != null){
			sb.append(":");

			sb.append(" ").append(message);
		}

		return sb.toString();
	}

	public PMMLObject getContext(){
		return this.context;
	}

	private void setContext(PMMLObject context){
		this.context = context;
	}

	static
	public String formatKey(Object object){

		if(object instanceof FieldName){
			FieldName name = (FieldName)object;

			object = name.getValue();
		} // End if

		return format(object);
	}

	static
	public String formatValue(Object object){

		if(object instanceof FieldValue){
			FieldValue fieldValue = (FieldValue)object;

			object = fieldValue.getValue();
		}

		return format(object);
	}

	static
	public String format(Object object){

		if(object instanceof String){
			String string = (String)object;

			return "\"" + string + "\"";
		}

		return (object != null ? String.valueOf(object) : null);
	}
}