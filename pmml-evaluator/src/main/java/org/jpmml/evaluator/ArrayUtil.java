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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.dmg.pmml.Array;
import org.dmg.pmml.PMMLAttributes;

public class ArrayUtil {

	private ArrayUtil(){
	}

	static
	public int getSize(Array array){
		Integer n = array.getN();
		if(n != null){
			return n;
		}

		List<?> content = getContent(array);

		return content.size();
	}

	static
	public List<?> getContent(Array array){
		return CacheUtil.getValue(array, ArrayUtil.contentCache);
	}

	static
	public List<? extends Number> asNumberList(Array array){
		List<?> content = getContent(array);

		Array.Type type = array.getType();
		if(type == null){
			throw new MissingAttributeException(array, PMMLAttributes.ARRAY_TYPE);
		}

		switch(type){
			case INT:
			case REAL:
				return (List)content;
			case STRING:
				throw new InvalidElementException(array);
			default:
				throw new UnsupportedAttributeException(array, type);
		}
	}

	static
	public List<?> parse(Array array){
		Array.Type type = array.getType();
		if(type == null){
			throw new MissingAttributeException(array, PMMLAttributes.ARRAY_TYPE);
		}

		List<String> tokens;

		Object value = array.getValue();

		if(value instanceof String){
			String string = (String)value;

			switch(type){
				case INT:
				case REAL:
				case STRING:
					tokens = org.jpmml.model.ArrayUtil.parse(type, string);
					break;
				default:
					throw new UnsupportedAttributeException(array, type);
			}
		} else

		if(value instanceof List){
			List<?> list = (List<?>)value;

			tokens = Lists.transform(list, TypeUtil::format);
		} else

		if(value instanceof Set){
			Set<?> set = (Set<?>)value;

			tokens = set.stream()
				.map(TypeUtil::format)
				.collect(Collectors.toList());
		} else

		{
			throw new InvalidElementException(array);
		}

		Integer n = array.getN();
		if(n != null && n != tokens.size()){
			throw new InvalidElementException(array);
		}

		switch(type){
			case INT:
				return Lists.transform(tokens, token -> Numbers.INTEGER_INTERNER.intern(Integer.parseInt(token)));
			case REAL:
				return Lists.transform(tokens, token -> Numbers.DOUBLE_INTERNER.intern(Double.parseDouble(token)));
			case STRING:
				return Lists.transform(tokens, token -> Strings.INTERNER.intern(token));
			default:
				throw new UnsupportedAttributeException(array, type);
		}
	}

	private static final LoadingCache<Array, List<?>> contentCache = CacheUtil.buildLoadingCache(new CacheLoader<Array, List<?>>(){

		@Override
		public List<?> load(Array array){
			return ImmutableList.copyOf(parse(array));
		}
	});
}