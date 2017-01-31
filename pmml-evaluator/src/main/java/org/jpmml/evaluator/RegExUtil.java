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

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.dmg.pmml.PMMLObject;

public class RegExUtil {

	private RegExUtil(){
	}

	static
	public Pattern compile(String regex, PMMLObject context){
		return compile(regex, 0, context);
	}

	static
	public Pattern compile(String regex, int flags, PMMLObject context){

		try {
			return Pattern.compile(regex, flags);
		} catch(PatternSyntaxException pse){
			Throwable throwable = (context != null ? new InvalidFeatureException(context) : new InvalidFeatureException())
				.initCause(pse);

			throw (PMMLException)throwable;
		}
	}
}