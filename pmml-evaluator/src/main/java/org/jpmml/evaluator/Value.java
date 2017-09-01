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

	@Operation (
		value = "<apply><plus/>${this}${0}</apply>",
		initialValue = "${0}"
	)
	abstract
	public Value<V> add(double value);

	@Operation (
		value = "<apply><plus/>${this}${0}</apply>",
		initialValue = "${0}"
	)
	abstract
	public Value<V> add(Value<? extends Number> value);

	/**
	 * <p>
	 * Adds <code>coefficient * factor</code>.
	 * </p>
	 */
	@Operation (
		value = "<apply><plus/>${this}<apply><times/>${0}${1}</apply></apply>",
		initialValue = "<apply><times/>${0}${1}</apply>"
	)
	abstract
	public Value<V> add(double coefficient, Number factor);

	/**
	 * <p>
	 * Adds <code>coefficient * (factor ^ exponent)</code>.
	 * </p>
	 */
	@Operation (
		value = "<apply><plus/>${this}<apply><times/>${0}<apply><power/>${1}${2}</apply></apply></apply>",
		initialValue = "<apply><times/>${0}<apply><power/>${1}${2}</apply></apply>"
	)
	abstract
	public Value<V> add(double coefficient, Number factor, int exponent);

	/**
	 * <p>
	 * Adds <code>coefficient * product(factors)</code>.
	 * </p>
	 */
	@Operation (
		value = "<apply><plus/>${this}<apply><times/>${0}${1}</apply></apply>",
		initialValue = "<apply><times/>${0}${1}</apply>"
	)
	abstract
	public Value<V> add(double coefficient, List<? extends Number> factors);

	@Operation (
		value = "<apply><minus/>${this}${0}</apply>",
		initialValue = "<apply><minus/>${0}</apply>"
	)
	abstract
	public Value<V> subtract(double value);

	@Operation (
		value = "<apply><minus/>${this}${0}</apply>",
		initialValue = "<apply><minus/>${0}</apply>"
	)
	abstract
	public Value<V> subtract(Value<? extends Number> value);

	@Operation (
		value = "<apply><times/>${this}${0}</apply>"
	)
	abstract
	public Value<V> multiply(double value);

	@Operation (
		value = "<apply><times/>${this}${0}</apply>"
	)
	abstract
	public Value<V> multiply(Value<? extends Number> value);

	/**
	 * <p>
	 * Multiplies by <code>factor ^ exponent</code>.
	 * </p>
	 */
	@Operation (
		value = "<apply><times/>${this}<apply><power/>${0}${1}</apply></apply>"
	)
	abstract
	public Value<V> multiply(Number factor, double exponent);

	@Operation (
		value = "<apply><divide/>${this}${0}</apply>"
	)
	abstract
	public Value<V> divide(double value);

	@Operation (
		value = "<apply><divide/>${this}${0}</apply>"
	)
	abstract
	public Value<V> divide(Value<? extends Number> value);

	@Operation (
		value = "<apply><minus/><cn>1</cn>${0}</apply>"
	)
	abstract
	public Value<V> residual(Value<? extends Number> value);

	@Operation (
		value = "<apply><power/>${this}<cn>2</cn></apply>"
	)
	abstract
	public Value<V> square();

	@Operation (
		value = "<apply><divide/><cn>1</cn>${this}</apply>"
	)
	abstract
	public Value<V> reciprocal();

	@Operation (
		value = "<apply><ci>elliott</ci>${this}</apply>"
	)
	abstract
	public Value<V> elliott();

	@Operation (
		value = "<apply><exp/>${this}</apply>"
	)
	abstract
	public Value<V> exp();

	@Operation (
		value = "<apply><ci>gauss</ci>${this}</apply>"
	)
	abstract
	public Value<V> gauss();

	@Operation (
		value = "<apply><apply><inverse/><ci>logit</ci></apply>${this}</apply>"
	)
	abstract
	public Value<V> inverseLogit();

	@Operation (
		value = "<apply><apply><inverse/><ci>cloglog</ci></apply>${this}</apply>"
	)
	abstract
	public Value<V> inverseCloglog();

	@Operation (
		value = "<apply><apply><inverse/><ci>loglog</ci></apply>${this}</apply>"
	)
	abstract
	public Value<V> inverseLoglog();

	@Operation (
		value = "<apply><apply><inverse/><ci>logc</ci></apply>${this}</apply>"
	)
	abstract
	public Value<V> inverseLogc();

	@Operation (
		value = "<apply><apply><inverse/><ci>negbin</ci></apply>${this}${0}</apply>"
	)
	abstract
	public Value<V> inverseNegbin(double value);

	@Operation (
		value = "<apply><apply><inverse/><ci>oddspower</ci></apply>${this}${0}</apply>"
	)
	abstract
	public Value<V> inverseOddspower(double value);

	@Operation (
		value = "<apply><power/>${this}<apply><divide/><cn>1</cn>${0}</apply></apply>"
	)
	abstract
	public Value<V> inversePower(double value);

	@Operation (
		value = "<apply><apply><inverse/><ci>cauchit</ci></apply>${this}</apply>"
	)
	abstract
	public Value<V> inverseCauchit();

	@Operation (
		value = "<apply><apply><inverse/><ci>probit</ci></apply>${this}</apply>"
	)
	abstract
	public Value<V> inverseProbit();

	@Operation (
		value = "<apply><sin/>${this}</apply>"
	)
	abstract
	public Value<V> sin();

	@Operation (
		value = "<apply><cos/>${this}</apply>"
	)
	abstract
	public Value<V> cos();

	@Operation (
		value = "<apply><arctan/>${this}</apply>"
	)
	abstract
	public Value<V> atan();

	@Operation (
		value = "<apply><tanh/>${this}</apply>"
	)
	abstract
	public Value<V> tanh();

	@Operation (
		value = "<apply><ci>threshold</ci>${this}${0}</apply>"
	)
	abstract
	public Value<V> threshold(double value);

	@Operation (
		value = "<apply><max/><cn>0</cn>${this}</apply>"
	)
	abstract
	public Value<V> relu();

	@Operation (
		value = "<apply><max/>${0}<apply><min/>${1}${this}</apply></apply>"
	)
	abstract
	public Value<V> restrict(double lowValue, double highValue);

	@Operation (
		value = "<apply><ci>round</ci>${this}</apply>"
	)
	abstract
	public Value<V> round();

	@Operation (
		value = "<apply><ceiling/>${this}</apply>"
	)
	abstract
	public Value<V> ceiling();

	@Operation (
		value = "<apply><floor/>${this}</apply>"
	)
	abstract
	public Value<V> floor();

	@Operation (
		value = "<apply><ci>denormalize</ci>${this}${0}${1}${2}${3}</apply>"
	)
	abstract
	public Value<V> denormalize(double leftOrig, double leftNorm, double rightOrig, double rightNorm);

	abstract
	public float floatValue();

	abstract
	public double doubleValue();

	abstract
	public V getValue();
}
