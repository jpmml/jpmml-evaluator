/*
 * Copyright (c) 2017 Villu Ruusmann
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

abstract
public class Vector<V extends Number> {

	abstract
	public int size();

	@Operation (
		value = "${this}${0}"
	)
	abstract
	public Vector<V> add(double value);

	@Operation (
		value = "${this}${0}"
	)
	abstract
	public Vector<V> add(Number value);

	/**
	 * <p>
	 * Adds <code>coefficient * factor</code>.
	 * </p>
	 */
	@Operation (
		value = "${this}<apply><times/>${0}${1}</apply>"
	)
	abstract
	public Vector<V> add(double coefficient, Number factor);

	abstract
	public Value<V> get(int index);

	abstract
	public Value<V> max();

	abstract
	public Value<V> sum();

	abstract
	public Value<V> median();
}