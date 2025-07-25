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
import java.util.Objects;

import com.google.common.collect.Interner;

public class SimpleInterner<V> implements Interner<V> {

	private Map<V, V> cache = new HashMap<>();


	private SimpleInterner(){
	}

	@Override
	public V intern(V object){
		Objects.requireNonNull(object);

		V existing = this.cache.get(object);
		if(existing != null){
			return existing;
		}

		this.cache.put(object, object);

		return object;
	}

	public void clear(){
		this.cache.clear();
	}

	static
	public <V> SimpleInterner<V> newInstance(){
		return new SimpleInterner<>();
	}
}