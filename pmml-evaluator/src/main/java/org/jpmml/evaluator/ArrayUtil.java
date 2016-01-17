/*
 * Copyright (c) 2012 University of Tartu
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jpmml.evaluator;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.Lists;
import org.dmg.pmml.Array;

public class ArrayUtil {

	private ArrayUtil(){
	}

	static
	public int getSize(Array array){
		Integer n = array.getN();
		if(n != null){
			return n.intValue();
		}

		List<String> content = getContent(array);

		return content.size();
	}

	static
	public List<String> getContent(Array array){
		return CacheUtil.getValue(array, ArrayUtil.contentCache);
	}

	static
	public List<? extends Number> asNumberList(Array array){
		List<String> content = getContent(array);

		Array.Type type = array.getType();
		switch(type){
			case INT:
				return Lists.transform(content, INT_PARSER);
			case REAL:
				return Lists.transform(content, REAL_PARSER);
			case STRING:
				throw new InvalidFeatureException(array);
			default:
				throw new UnsupportedFeatureException(array, type);
		}
	}

	static
	List<String> parse(Array array){
		List<String> result;

		Array.Type type = array.getType();
		switch(type){
			case INT:
			case REAL:
				result = tokenize(array.getValue(), false);
				break;
			case STRING:
				result = tokenize(array.getValue(), true);
				break;
			default:
				throw new UnsupportedFeatureException(array, type);
		}

		Integer n = array.getN();
		if(n != null && n.intValue() != result.size()){
			throw new InvalidFeatureException(array);
		}

		return result;
	}

	static
	private List<String> tokenize(String string, boolean enableQuotes){
		List<String> result = new ArrayList<>();

		StringBuilder sb = new StringBuilder();

		boolean quoted = false;

		tokens:
		for(int i = 0; i < string.length(); i++){
			char c = string.charAt(i);

			if(quoted){

				if(c == '\\' && i < (string.length() - 1)){
					c = string.charAt(i + 1);

					if(c == '\"'){
						sb.append('\"');

						i++;
					} else

					{
						sb.append('\\');
					}

					continue tokens;
				} // End if

				sb.append(c);

				if(c == '\"'){
					result.add(createToken(sb, enableQuotes));

					quoted = false;
				}
			} else

			{
				if(c == '\"' && enableQuotes){

					if(sb.length() > 0){
						result.add(createToken(sb, enableQuotes));
					}

					sb.append('\"');

					quoted = true;
				} else

				if(Character.isWhitespace(c)){

					if(sb.length() > 0){
						result.add(createToken(sb, enableQuotes));
					}
				} else

				{
					sb.append(c);
				}
			}
		}

		if(sb.length() > 0){
			result.add(createToken(sb, enableQuotes));
		}

		return result;
	}

	static
	private String createToken(StringBuilder sb, boolean enableQuotes){
		String result;

		if(sb.length() > 1 && (sb.charAt(0) == '\"' && sb.charAt(sb.length() - 1) == '\"') && enableQuotes){
			result = sb.substring(1, sb.length() - 1);
		} else

		{
			result = sb.substring(0, sb.length());
		}

		result = ArrayUtil.tokenInterner.intern(result);

		sb.setLength(0);

		return result;
	}

	private static Interner<String> tokenInterner = Interners.newWeakInterner();

	private static final Function<String, Integer> INT_PARSER = new Function<String, Integer>(){

		@Override
		public Integer apply(String string){
			return Integer.parseInt(string);
		}
	};

	private static final Function<String, Double> REAL_PARSER = new Function<String, Double>(){

		@Override
		public Double apply(String string){
			return Double.parseDouble(string);
		}
	};

	private static final LoadingCache<Array, List<String>> contentCache = CacheUtil.buildLoadingCache(new CacheLoader<Array, List<String>>(){

		@Override
		public List<String> load(Array array){
			return ImmutableList.copyOf(parse(array));
		}
	});
}