/*
 * Copyright (c) 2014 Villu Ruusmann
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

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.dmg.pmml.PMMLObject;
import org.jpmml.model.InvalidElementException;
import org.jpmml.model.PMMLException;

public class CacheUtil {

	private CacheUtil(){
	}

	static
	public <K extends PMMLObject, V> V getValue(K key, LoadingCache<K, V> cache){

		try {
			return cache.get(key);
		} catch(ExecutionException | UncheckedExecutionException e){
			Throwable cause = e.getCause();

			if(cause instanceof PMMLException){
				throw (PMMLException)cause;
			}

			throw new InvalidElementException(key)
				.initCause(cause);
		}
	}

	static
	public <K extends PMMLObject, V> V getValue(K key, Cache<K, V> cache, Callable<? extends V> loader){

		try {
			return cache.get(key, loader);
		} catch(ExecutionException | UncheckedExecutionException e){
			Throwable cause = e.getCause();

			if(cause instanceof PMMLException){
				throw (PMMLException)cause;
			}

			throw new InvalidElementException(key)
				.initCause(cause);
		}
	}

	static
	public <K, V> Cache<K, V> buildCache(){
		CacheBuilder<Object, Object> cacheBuilder = newCacheBuilder();

		return cacheBuilder.build();
	}

	static
	public <K, V> LoadingCache<K, V> buildLoadingCache(CacheLoader<K, V> cacheLoader){
		CacheBuilder<Object, Object> cacheBuilder = newCacheBuilder();

		return cacheBuilder.build(cacheLoader);
	}

	static
	private CacheBuilder<Object, Object> newCacheBuilder(){
		CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.from(CacheUtil.cacheBuilderSpec);

		return cacheBuilder;
	}

	static
	public CacheBuilderSpec getCacheBuilderSpec(){
		return CacheUtil.cacheBuilderSpec;
	}

	static
	public void setCacheBuilderSpec(CacheBuilderSpec cacheBuilderSpec){
		CacheUtil.cacheBuilderSpec = Objects.requireNonNull(cacheBuilderSpec);
	}

	private static CacheBuilderSpec cacheBuilderSpec = CacheBuilderSpec.parse("weakKeys");
}