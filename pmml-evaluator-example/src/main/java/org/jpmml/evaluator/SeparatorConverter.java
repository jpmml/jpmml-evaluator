/*
 * Copyright (c) 2017 Villu Ruusmann
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

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;

public class SeparatorConverter extends BaseConverter<String> {

	public SeparatorConverter(String optionName){
		super(optionName);
	}

	@Override
	public String convert(String value){

		try {
			return unescape(value);
		} catch(IllegalArgumentException iae){
			throw new ParameterException(getErrorString(value, "a Java-style escaped string"));
		}
	}

	static
	public String unescape(String string){
		StringBuilder sb = new StringBuilder();

		boolean escaped = false;

		for(int i = 0; i < string.length(); i++){
			char c = string.charAt(i);

			if(c == '\\'){

				if(escaped){
					sb.append('\\');

					escaped = false;
				} else

				{
					escaped = true;
				}

				continue;
			} // End if

			if(escaped){
				sb.append(translate(c));
			} else

			{
				sb.append(c);
			}

			escaped = false;
		}

		if(escaped){
			throw new IllegalArgumentException();
		} // End if

		if(sb.length() == 0){
			throw new IllegalArgumentException();
		}

		return sb.toString();
	}

	static
	private char translate(char c){

		switch(c){
			case 'b':
				return '\b';
			case 't':
				return '\t';
			case 'n':
				return '\n';
			case 'f':
				return '\f';
			case 'r':
				return '\r';
			case '\"':
			case '\'':
				return c;
			default:
				throw new IllegalArgumentException();
		}
	}
}