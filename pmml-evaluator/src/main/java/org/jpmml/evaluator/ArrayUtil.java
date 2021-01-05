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

import com.google.common.base.Function;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
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

			Function<Object, String> function = new Function<Object, String>(){

				@Override
				public String apply(Object value){
					return TypeUtil.format(value);
				}
			};

			tokens = Lists.transform(list, function);
		} else

		if(value instanceof Set){
			Set<?> set = (Set<?>)value;

			Function<Object, String> function = new Function<Object, String>(){

				@Override
				public String apply(Object value){
					return TypeUtil.format(value);
				}
			};

			tokens = Lists.newArrayList(Iterables.transform(set, function));
		} else

		{
			throw new InvalidElementException(array);
		}

		Integer n = array.getN();
		if(n != null && n != tokens.size()){
			throw new InvalidElementException(array);
		}

		Function<String, ?> function;

		switch(type){
			case INT:
				function = new Function<String, Integer>(){

					@Override
					public Integer apply(String string){
						return Numbers.INTEGER_INTERNER.intern(Integer.parseInt(string));
					}
				};
				break;
			case REAL:
				function = new Function<String, Double>(){

					@Override
					public Double apply(String string){
						return Numbers.DOUBLE_INTERNER.intern(Double.parseDouble(string));
					}
				};
				break;
			case STRING:
				function = new Function<String, String>(){

					@Override
					public String apply(String string){
						return Strings.INTERNER.intern(string);
					}
				};
				break;
			default:
				throw new UnsupportedAttributeException(array, type);
		}

		return Lists.transform(tokens, function);
	}

	private static final LoadingCache<Array, List<?>> contentCache = CacheUtil.buildLoadingCache(new CacheLoader<Array, List<?>>(){

		@Override
		public List<?> load(Array array){
			return ImmutableList.copyOf(parse(array));
		}
	});
}