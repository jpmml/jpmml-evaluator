/*
 * Copyright (c) 2013 KNIME.com AG, Zurich, Switzerland
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

			throw (PMMLException)new InvalidFeatureException(key)
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

			throw (PMMLException)new InvalidFeatureException(key)
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