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

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;

public class TextTokenizer {

	private Pattern pattern = null;


	public TextTokenizer(Pattern pattern){
		setPattern(pattern);
	}

	public List<String> tokenize(String string){
		Pattern pattern = getPattern();

		if(("").equals(string)){
			return Collections.<String>emptyList();
		}

		int count = 0;

		String[] tokens = pattern.split(string, -1);
		for(int i = 0, max = tokens.length; i < max; i++){
			String token = tokens[i];

			int begin = 0;
			int end = token.length();

			// Trim leading punctuation
			while(begin < end){
				char c = token.charAt(begin);

				if(!isPunctuation(c)){
					break;
				}

				begin++;
			}

			// Trim trailing punctuation
			while(end > begin){
				char c = token.charAt(end - 1);

				if(!isPunctuation(c)){
					break;
				}

				end--;
			}

			if(begin > 0 || end < token.length()){
				token = token.substring(begin, end);
			} // End if

			if(token.length() > 0){
				tokens[count] = token;

				count++;
			}
		}

		if(count < tokens.length){
			String[] tmpTokens = new String[count];

			System.arraycopy(tokens, 0, tmpTokens, 0, count);

			tokens = tmpTokens;
		}

		return ImmutableList.copyOf(tokens);
	}

	public Pattern getPattern(){
		return this.pattern;
	}

	private void setPattern(Pattern pattern){
		this.pattern = pattern;
	}

	static
	public boolean isPunctuation(char c){
		int type = Character.getType(c);

		switch(type){
			case Character.DASH_PUNCTUATION:
			case Character.END_PUNCTUATION:
			case Character.START_PUNCTUATION:
			case Character.CONNECTOR_PUNCTUATION:
			case Character.OTHER_PUNCTUATION:
			case Character.INITIAL_QUOTE_PUNCTUATION:
			case Character.FINAL_QUOTE_PUNCTUATION:
				return true;
			default:
				return false;
		}
	}
}