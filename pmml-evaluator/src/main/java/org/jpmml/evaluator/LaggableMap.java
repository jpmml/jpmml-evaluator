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

import java.util.List;
import java.util.Map;

public interface LaggableMap<K, V> extends Map<K, V> {

	/**
	 * <p>
	 * Gets the field value at the current position.
	 * </p>
	 *
	 * @param key The field name.
	 *
	 * @return The field value or {@code null}.
	 */
	@Override
	V get(Object key);

	/**
	 * <p>
	 * Gets the field value after applying a &quot;lag transformation&quot; to the current position.
	 * </p>
	 *
	 * @param key The field name.
	 * @param n The number of steps to move backwards from the current position.
	 * The previous position is {@code 1} step backwards.
	 * @param blockIndicatorKeys Block indicator field names.
	 *
	 * @return The field value or {@code null}.
	 */
	V getLagged(K key, int n, List<K> blockIndicatorKeys);
}