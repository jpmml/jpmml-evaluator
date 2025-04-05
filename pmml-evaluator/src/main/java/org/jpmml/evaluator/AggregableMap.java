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

import org.dmg.pmml.Lag;

public interface AggregableMap<K, V> extends Map<K, V> {

	/**
	 * <p>
	 * Gets the aggregate field value after applying a &quot;window transformation&quot; to the current position.
	 * </p>
	 *
	 * @param key The field name.
	 * @param function The aggregation function name.
	 * The implementation should recognize and support all {@link Lag.Aggregate} enum constant values (except for the `none` value).
	 * @param n The window size.
	 * @param blockIndicatorKeys Block indicator field names.
	 *
	 * @return The aggregate field value.
	 */
	V getAggregated(K key, String function, int n, List<K> blockIndicatorKeys);
}