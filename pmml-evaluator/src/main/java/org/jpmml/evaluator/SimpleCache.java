/*
 * Copyright (c) 2025 Villu Ruusmann
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.AbstractCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

public class SimpleCache<K, V> extends AbstractCache<K, V> {

	private Map<K, V> cache = new HashMap<>();


	private SimpleCache(){
	}

	@Override
	public V get(K key, Callable<? extends V> callable) throws ExecutionException {
		V value = this.cache.get(key);

		if(value == null){

			try {
				value = callable.call();
			} catch(RuntimeException re){
				throw new UncheckedExecutionException(re);
			} catch(Exception e){
				throw new ExecutionException(e);
			}

			this.cache.put(key, value);
		}

		return value;
	}

	@Override
	public V getIfPresent(Object key){
		return this.cache.get(key);
	}

	@Override
	public void invalidate(Object key){
		this.cache.remove(key);
	}

	@Override
	public void put(K key, V value){
		this.cache.put(key, value);
	}

	static
	public <K, V> SimpleCache<K, V> newInstance(){
		return new SimpleCache<>();
	}
}