/*
 * Copyright (c) 2022 Villu Ruusmann
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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TokenizedString {

	private int hashCode = 0;

	private String[] tokens = null;


	public TokenizedString(String... tokens){
		setTokens(tokens);
	}

	public TokenizedString(List<String> tokens){
		this(tokens.toArray(new String[tokens.size()]));
	}

	public int size(){
		String[] tokens = getTokens();

		return tokens.length;
	}

	public String get(int i){
		String[] tokens = getTokens();

		return tokens[i];
	}

	public TokenizedString slice(int fromIndex, int toIndex){
		String[] tokens = getTokens();

		if((fromIndex == 0) && (toIndex == tokens.length)){
			return this;
		}

		String[] newTokens = new String[toIndex - fromIndex];
		System.arraycopy(tokens, fromIndex, newTokens, 0, toIndex - fromIndex);

		return new TokenizedString(newTokens);
	}

	@Override
	public int hashCode(){

		if(this.hashCode == 0){
			this.hashCode = Arrays.hashCode(this.getTokens());
		}

		return this.hashCode;
	}

	@Override
	public boolean equals(Object object){

		if(object instanceof TokenizedString){
			TokenizedString that = (TokenizedString)object;

			return Arrays.equals(this.getTokens(), that.getTokens());
		}

		return false;
	}

	public String[] getTokens(){
		return this.tokens;
	}

	private void setTokens(String[] tokens){
		this.tokens = Objects.requireNonNull(tokens);
	}

	public static final TokenizedString EMPTY = new TokenizedString(new String[0]){

		@Override
		public int hashCode(){
			return 0;
		}
	};
}