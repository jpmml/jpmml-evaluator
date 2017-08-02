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

import java.util.List;

abstract
public class Value<V extends Number> implements Comparable<Value<V>> {

	abstract
	public Value<V> copy();

	abstract
	public Value<V> add(double value);

	abstract
	public Value<V> add(Value<?> value);

	abstract
	public Value<V> add(Number factor, int exponent, double coefficient);

	abstract
	public Value<V> add(Value<?> factor, int exponent, double coefficient);

	abstract
	public Value<V> add(List<? extends Number> factors, double coefficient);

	abstract
	public Value<V> subtract(double value);

	abstract
	public Value<V> subtract(Value<?> value);

	abstract
	public Value<V> multiply(double value);

	abstract
	public Value<V> multiply(Value<?> value);

	abstract
	public Value<V> multiply(Number factor, double exponent);

	abstract
	public Value<V> multiply(Value<?> factor, double exponent);

	abstract
	public Value<V> divide(double value);

	abstract
	public Value<V> divide(Value<?> value);

	abstract
	public Value<V> residual(Value<?> value);

	abstract
	public Value<V> square();

	abstract
	public Value<V> reciprocal();

	abstract
	public Value<V> elliott();

	abstract
	public Value<V> exp();

	abstract
	public Value<V> gauss();

	abstract
	public Value<V> inverseLogit();

	abstract
	public Value<V> inverseCloglog();

	abstract
	public Value<V> inverseLoglog();

	abstract
	public Value<V> inverseLogc();

	abstract
	public Value<V> inverseNegbin(double value);

	abstract
	public Value<V> inverseOddspower(double value);

	abstract
	public Value<V> inversePower(double value);

	abstract
	public Value<V> inverseCauchit();

	abstract
	public Value<V> inverseProbit();

	abstract
	public Value<V> sin();

	abstract
	public Value<V> cos();

	abstract
	public Value<V> atan();

	abstract
	public Value<V> tanh();

	abstract
	public Value<V> threshold(double value);

	abstract
	public Value<V> relu();

	abstract
	public Value<V> restrict(double lowValue, double highValue);

	abstract
	public Value<V> round();

	abstract
	public Value<V> ceiling();

	abstract
	public Value<V> floor();

	abstract
	public Value<V> denormalize(double leftOrig, double leftNorm, double rightOrig, double rightNorm);

	abstract
	public float floatValue();

	abstract
	public double doubleValue();

	abstract
	public V getValue();
}