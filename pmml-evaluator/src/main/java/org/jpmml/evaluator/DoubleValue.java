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

public class DoubleValue extends Value<Double> {

	protected double value = 0d;


	@Operation (
		value = "${0}"
	)
	public DoubleValue(double value){
		this.value = value;
	}

	@Operation (
		value = "${0}"
	)
	public DoubleValue(Number value){
		this.value = value.doubleValue();
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
	public DoubleValue add(Number value){
		this.value += value.doubleValue();

		return this;
	}

	@Override
	public DoubleValue add(Value<? extends Number> value){
		this.value += value.doubleValue();

		return this;
	}

	@Override
	public DoubleValue add(Number coefficient, Number factor){
		this.value += coefficient.doubleValue() * factor.doubleValue();

		return this;
	}

	@Override
	public DoubleValue add(Number coefficient, Number firstFactor, Number secondFactor){
		this.value += coefficient.doubleValue() * firstFactor.doubleValue() * secondFactor.doubleValue();

		return this;
	}

	@Override
	public DoubleValue add(Number coefficient, Number... factors){
		double value = coefficient.doubleValue();

		for(int i = 0; i < factors.length; i++){
			Number factor = factors[i];

			value *= factor.doubleValue();
		}

		this.value += value;

		return this;
	}

	@Override
	public DoubleValue add(Number coefficient, Number factor, int exponent){
		this.value += coefficient.doubleValue() * Math.pow(factor.doubleValue(), exponent);

		return this;
	}

	@Override
	public DoubleValue subtract(Number value){
		this.value -= value.doubleValue();

		return this;
	}

	@Override
	public DoubleValue subtract(Value<? extends Number> value){
		this.value -= value.doubleValue();

		return this;
	}

	@Override
	public DoubleValue multiply(Number value){
		this.value *= value.doubleValue();

		return this;
	}

	@Override
	public DoubleValue multiply(Value<? extends Number> value){
		this.value *= value.doubleValue();

		return this;
	}

	@Override
	public DoubleValue multiply(Number factor, Number exponent){
		this.value *= Math.pow(factor.doubleValue(), exponent.doubleValue());

		return this;
	}

	@Override
	public DoubleValue divide(Number value){
		this.value /= value.doubleValue();

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
	public DoubleValue power(Number value){
		this.value = Math.pow(this.value, value.doubleValue());

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
	public DoubleValue ln(){
		this.value = Math.log(this.value);

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
	public DoubleValue inverseNegbin(Number value){
		this.value = 1d / (value.doubleValue() * (Math.exp(-this.value) - 1d));

		return this;
	}

	@Override
	public DoubleValue inverseOddspower(Number value){
		return inverseOddspowerInternal(value.doubleValue());
	}

	private DoubleValue inverseOddspowerInternal(double value){

		if(value < 0d || value > 0d){
			this.value = 1d / (1d + Math.pow(1d + (value * this.value), -(1d / value)));
		} else

		{
			this.value = 1d / (1d + Math.exp(-this.value));
		}

		return this;
	}

	@Override
	public DoubleValue inversePower(Number value){
		return inversePowerInternal(value.doubleValue());
	}

	private DoubleValue inversePowerInternal(double value){

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
	public DoubleValue arctan(){
		this.value = (2d * Math.atan(this.value)) / DoubleValue.PI;

		return this;
	}

	@Override
	public DoubleValue tanh(){
		this.value = Math.tanh(this.value);

		return this;
	}

	@Override
	public DoubleValue threshold(Number value){
		this.value = (this.value > value.doubleValue() ? 1d : 0d);

		return this;
	}

	@Override
	public DoubleValue relu(){
		this.value = Math.max(this.value, 0);

		return this;
	}

	@Override
	public DoubleValue abs(){
		this.value = Math.abs(this.value);

		return this;
	}

	@Override
	public DoubleValue gaussSim(Number value){
		this.value = Math.exp((-Math.log(2d) * this.value * this.value) / (value.doubleValue() * value.doubleValue()));

		return this;
	}

	@Override
	public DoubleValue restrict(Number lowValue, Number highValue){
		this.value = Math.max(this.value, lowValue.doubleValue());
		this.value = Math.min(this.value, highValue.doubleValue());

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
	public DoubleValue normalize(Number leftOrig, Number leftNorm, Number rightOrig, Number rightNorm){
		return normalizeInternal(leftOrig.doubleValue(), leftNorm.doubleValue(), rightOrig.doubleValue(), rightNorm.doubleValue());
	}

	private DoubleValue normalizeInternal(double leftOrig, double leftNorm, double rightOrig, double rightNorm){
		this.value = leftNorm + ((this.value - leftOrig) / (rightOrig - leftOrig)) * (rightNorm - leftNorm);

		return this;
	}

	@Override
	public DoubleValue denormalize(Number leftOrig, Number leftNorm, Number rightOrig, Number rightNorm){
		return denormalizeInternal(leftOrig.doubleValue(), leftNorm.doubleValue(), rightOrig.doubleValue(), rightNorm.doubleValue());
	}

	private DoubleValue denormalizeInternal(double leftOrig, double leftNorm, double rightOrig, double rightNorm){
		this.value = ((this.value - leftNorm) / (rightNorm - leftNorm)) * (rightOrig - leftOrig) + leftOrig;

		return this;
	}

	@Override
	public boolean isZero(){
		return this.value == 0d;
	}

	@Override
	public boolean isOne(){
		return this.value == 1d;
	}

	@Override
	public boolean equals(Number value){
		return this.value == value.doubleValue();
	}

	@Override
	public int compareTo(Number value){
		return Double.compare(this.value, value.doubleValue());
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
