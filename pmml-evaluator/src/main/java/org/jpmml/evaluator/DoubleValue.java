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

public class DoubleValue extends Value<Double> {

	protected double value = 0d;


	@Operation (
		value = "${0}"
	)
	public DoubleValue(double value){
		this.value = value;
	}

	@Override
	public int compareTo(Value<Double> that){
		return Double.compare(this.doubleValue(), that.doubleValue());
	}

	@Override
	public String toString(){
		return Double.toString(this.value);
	}

	@Override
	public int hashCode(){
		long bits = Double.doubleToLongBits(this.value);

		return (int)(bits ^ (bits >>> 32));
	}

	@Override
	public boolean equals(Object object){

		if(object instanceof DoubleValue){
			DoubleValue that = (DoubleValue)object;

			return (Double.doubleToLongBits(this.value) == Double.doubleToLongBits(that.value));
		}

		return false;
	}

	@Override
	public DoubleValue copy(){
		return new DoubleValue(this.value);
	}

	@Override
	public DoubleValue add(double value){
		this.value += value;

		return this;
	}

	@Override
	public DoubleValue add(Value<? extends Number> value){
		this.value += value.doubleValue();

		return this;
	}

	@Override
	public DoubleValue add(double coefficient, Number factor){
		this.value += coefficient * factor.doubleValue();

		return this;
	}

	@Override
	public DoubleValue add(double coefficient, Number factor, int exponent){
		this.value += coefficient * Math.pow(factor.doubleValue(), exponent);

		return this;
	}

	@Override
	public DoubleValue add(double coefficient, List<? extends Number> factors){
		double value = coefficient;

		for(int i = 0; i < factors.size(); i++){
			Number factor = factors.get(i);

			value *= factor.doubleValue();
		}

		this.value += value;

		return this;
	}

	@Override
	public DoubleValue subtract(double value){
		this.value -= value;

		return this;
	}

	@Override
	public DoubleValue subtract(Value<? extends Number> value){
		this.value -= value.doubleValue();

		return this;
	}

	@Override
	public DoubleValue multiply(double value){
		this.value *= value;

		return this;
	}

	@Override
	public DoubleValue multiply(Value<? extends Number> value){
		this.value *= value.doubleValue();

		return this;
	}

	@Override
	public DoubleValue multiply(Number factor, double exponent){
		this.value *= Math.pow(factor.doubleValue(), exponent);

		return this;
	}

	@Override
	public DoubleValue divide(double value){
		this.value /= value;

		return this;
	}

	@Override
	public DoubleValue divide(Value<? extends Number> value){
		this.value /= value.doubleValue();

		return this;
	}

	@Override
	public DoubleValue residual(Value<? extends Number> value){
		this.value = 1d - value.doubleValue();

		return this;
	}

	@Override
	public DoubleValue square(){
		this.value *= this.value;

		return this;
	}

	@Override
	public DoubleValue reciprocal(){
		this.value = 1d / this.value;

		return this;
	}

	@Override
	public DoubleValue elliott(){
		this.value /= (1d + Math.abs(this.value));

		return this;
	}

	@Override
	public DoubleValue exp(){
		this.value = Math.exp(this.value);

		return this;
	}

	@Override
	public DoubleValue gauss(){
		this.value = Math.exp(-(this.value * this.value));

		return this;
	}

	@Override
	public DoubleValue inverseLogit(){
		this.value = 1d / (1d + Math.exp(-this.value));

		return this;
	}

	@Override
	public DoubleValue inverseCloglog(){
		this.value = 1d - Math.exp(-Math.exp(this.value));

		return this;
	}

	@Override
	public DoubleValue inverseLoglog(){
		this.value = Math.exp(-Math.exp(-this.value));

		return this;
	}

	@Override
	public DoubleValue inverseLogc(){
		this.value = 1d - Math.exp(this.value);

		return this;
	}

	@Override
	public DoubleValue inverseNegbin(double value){
		this.value = 1d / (value * (Math.exp(-this.value) - 1d));

		return this;
	}

	@Override
	public DoubleValue inverseOddspower(double value){

		if(value < 0d || value > 0d){
			this.value = 1d / (1d + Math.pow(1d + (value * this.value), -(1d / value)));
		} else

		{
			this.value = 1d / (1d + Math.exp(-this.value));
		}

		return this;
	}

	@Override
	public DoubleValue inversePower(double value){

		if(value < 0d || value > 0d){
			this.value = Math.pow(this.value, 1d / value);
		} else

		{
			this.value = Math.exp(this.value);
		}

		return this;
	}

	@Override
	public DoubleValue inverseCauchit(){
		this.value = 0.5d + (1d / DoubleValue.PI) * Math.atan(this.value);

		return this;
	}

	@Override
	public DoubleValue inverseProbit(){
		this.value = NormalDistributionUtil.cumulativeProbability(this.value);

		return this;
	}

	@Override
	public DoubleValue sin(){
		this.value = Math.sin(this.value);

		return this;
	}

	@Override
	public DoubleValue cos(){
		this.value = Math.cos(this.value);

		return this;
	}

	@Override
	public DoubleValue atan(){
		this.value = Math.atan(this.value);

		return this;
	}

	@Override
	public DoubleValue tanh(){
		this.value = Math.tanh(this.value);

		return this;
	}

	@Override
	public DoubleValue threshold(double value){
		this.value = (this.value > value ? 1d : 0d);

		return this;
	}

	@Override
	public DoubleValue relu(){
		this.value = Math.max(this.value, 0);

		return this;
	}

	@Override
	public DoubleValue restrict(double lowValue, double highValue){
		this.value = Math.max(this.value, lowValue);
		this.value = Math.min(this.value, highValue);

		return this;
	}

	@Override
	public DoubleValue round(){
		this.value = Math.round(this.value);

		return this;
	}

	@Override
	public DoubleValue ceiling(){
		this.value = Math.ceil(this.value);

		return this;
	}

	@Override
	public DoubleValue floor(){
		this.value = Math.floor(this.value);

		return this;
	}

	@Override
	public DoubleValue denormalize(double leftOrig, double leftNorm, double rightOrig, double rightNorm){
		this.value = ((this.value - leftNorm) / (rightNorm - leftNorm)) * (rightOrig - leftOrig) + leftOrig;

		return this;
	}

	@Override
	public float floatValue(){
		return (float)this.value;
	}

	@Override
	public double doubleValue(){
		return this.value;
	}

	@Override
	public Double getValue(){
		return this.value;
	}

	public static final double E = Math.E;

	public static final double PI = Math.PI;
}