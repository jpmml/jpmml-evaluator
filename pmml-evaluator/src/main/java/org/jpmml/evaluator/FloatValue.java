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

public class FloatValue extends Value<Float> {

	protected float value = 0f;


	@Operation (
		value = "${0}"
	)
	public FloatValue(float value){
		this.value = value;
	}

	@Operation (
		value = "${0}"
	)
	public FloatValue(Number value){
		this.value = value.floatValue();
	}

	@Override
	public int compareTo(Value<Float> that){
		return Float.compare(this.floatValue(), that.floatValue());
	}

	@Override
	public String toString(){
		return Float.toString(this.value);
	}

	@Override
	public int hashCode(){
		return Float.floatToIntBits(this.value);
	}

	@Override
	public boolean equals(Object object){

		if(object instanceof FloatValue){
			FloatValue that = (FloatValue)object;

			return (Float.floatToIntBits(this.value) == Float.floatToIntBits(that.value));
		}

		return false;
	}

	@Override
	public FloatValue copy(){
		return new FloatValue(this.value);
	}

	@Override
	public FloatValue add(Number value){
		this.value += value.floatValue();

		return this;
	}

	@Override
	public FloatValue add(Value<? extends Number> value){
		this.value += value.floatValue();

		return this;
	}

	@Override
	public FloatValue add(Number coefficient, Number factor){
		this.value += coefficient.floatValue() * factor.floatValue();

		return this;
	}

	@Override
	public FloatValue add(Number coefficient, Number firstFactor, Number secondFactor){
		this.value += coefficient.floatValue() * firstFactor.floatValue() * secondFactor.floatValue();

		return this;
	}

	@Override
	public FloatValue add(Number coefficient, Number... factors){
		float value = coefficient.floatValue();

		for(int i = 0; i < factors.length; i++){
			Number factor = factors[i];

			value *= factor.floatValue();
		}

		this.value += value;

		return this;
	}

	@Override
	public FloatValue add(Number coefficient, Number factor, int exponent){
		this.value += coefficient.floatValue() * FloatValue.pow(factor.floatValue(), exponent);

		return this;
	}

	@Override
	public FloatValue subtract(Number value){
		this.value -= value.floatValue();

		return this;
	}

	@Override
	public FloatValue subtract(Value<? extends Number> value){
		this.value -= value.floatValue();

		return this;
	}

	@Override
	public FloatValue multiply(Number value){
		this.value *= value.floatValue();

		return this;
	}

	@Override
	public FloatValue multiply(Value<? extends Number> value){
		this.value *= value.floatValue();

		return this;
	}

	@Override
	public FloatValue multiply(Number factor, Number exponent){
		this.value *= FloatValue.pow(factor.floatValue(), exponent.floatValue());

		return this;
	}

	@Override
	public FloatValue divide(Number value){
		this.value /= value.floatValue();

		return this;
	}

	@Override
	public FloatValue divide(Value<? extends Number> value){
		this.value /= value.floatValue();

		return this;
	}

	@Override
	public FloatValue residual(Value<? extends Number> value){
		this.value = 1f - value.floatValue();

		return this;
	}

	@Override
	public FloatValue square(){
		this.value *= this.value;

		return this;
	}

	@Override
	public FloatValue power(Number value){
		this.value = FloatValue.pow(this.value, value.floatValue());

		return this;
	}

	@Override
	public FloatValue reciprocal(){
		this.value = 1f / this.value;

		return this;
	}

	@Override
	public FloatValue elliott(){
		this.value /= (1f + Math.abs(this.value));

		return this;
	}

	@Override
	public FloatValue exp(){
		this.value = FloatValue.exp(this.value);

		return this;
	}

	@Override
	public FloatValue ln(){
		this.value = (float)Math.log(this.value);

		return this;
	}

	@Override
	public FloatValue gauss(){
		this.value = FloatValue.exp(-(this.value * this.value));

		return this;
	}

	@Override
	public FloatValue inverseLogit(){
		this.value = 1f / (1f + FloatValue.exp(-this.value));

		return this;
	}

	@Override
	public FloatValue inverseCloglog(){
		this.value = 1f - FloatValue.exp(-FloatValue.exp(this.value));

		return this;
	}

	@Override
	public FloatValue inverseLoglog(){
		this.value = FloatValue.exp(-FloatValue.exp(-this.value));

		return this;
	}

	@Override
	public FloatValue inverseLogc(){
		this.value = 1f - FloatValue.exp(this.value);

		return this;
	}

	@Override
	public FloatValue inverseNegbin(Number value){
		this.value = 1f / (value.floatValue() * (FloatValue.exp(-this.value) - 1f));

		return this;
	}

	@Override
	public FloatValue inverseOddspower(Number value){
		return inverseOddspowerInternal(value.floatValue());
	}

	private FloatValue inverseOddspowerInternal(float value){

		if(value < 0f || value > 0f){
			this.value = 1f / (1f + FloatValue.pow(1f + (value * this.value), -(1f / value)));
		} else

		{
			this.value = 1f / (1f + FloatValue.exp(-this.value));
		}

		return this;
	}

	@Override
	public FloatValue inversePower(Number value){
		return inversePowerInternal(value.floatValue());
	}

	private FloatValue inversePowerInternal(float value){

		if(value < 0f || value > 0f){
			this.value = FloatValue.pow(this.value, 1f / value);
		} else

		{
			this.value = FloatValue.exp(this.value);
		}

		return this;
	}

	@Override
	public FloatValue inverseCauchit(){
		this.value = 0.5f + (1f / (float)FloatValue.PI) * (float)Math.atan(this.value);

		return this;
	}

	@Override
	public FloatValue inverseProbit(){
		throw new NotImplementedException();
	}

	@Override
	public FloatValue sin(){
		this.value = (float)Math.sin(this.value);

		return this;
	}

	@Override
	public FloatValue cos(){
		this.value = (float)Math.cos(this.value);

		return this;
	}

	@Override
	public FloatValue arctan(){
		this.value = (2f * (float)Math.atan(this.value)) / (float)FloatValue.PI;

		return this;
	}

	@Override
	public FloatValue tanh(){
		this.value = (float)Math.tanh(this.value);

		return this;
	}

	@Override
	public FloatValue threshold(Number value){
		this.value = (this.value > value.floatValue() ? 1f : 0f);

		return this;
	}

	@Override
	public FloatValue relu(){
		this.value = Math.max(this.value, 0);

		return this;
	}

	@Override
	public FloatValue abs(){
		this.value = Math.abs(this.value);

		return this;
	}

	@Override
	public FloatValue gaussSim(Number value){
		return gaussSimInternal(value.floatValue());
	}

	private FloatValue gaussSimInternal(float value){
		this.value = FloatValue.exp((-(float)Math.log(2f) * this.value * this.value) / (value * value));

		return this;
	}

	@Override
	public FloatValue restrict(Number lowValue, Number highValue){
		this.value = Math.max(this.value, lowValue.floatValue());
		this.value = Math.min(this.value, highValue.floatValue());

		return this;
	}

	@Override
	public FloatValue round(){
		this.value = Math.round(this.value);

		return this;
	}

	@Override
	public FloatValue ceiling(){
		this.value = (float)Math.ceil(this.value);

		return this;
	}

	@Override
	public FloatValue floor(){
		this.value = (float)Math.floor(this.value);

		return this;
	}

	@Override
	public FloatValue normalize(Number leftOrig, Number leftNorm, Number rightOrig, Number rightNorm){
		return normalizeInternal(leftOrig.floatValue(), leftNorm.floatValue(), rightOrig.floatValue(), rightNorm.floatValue());
	}

	private FloatValue normalizeInternal(float leftOrig, float leftNorm, float rightOrig, float rightNorm){
		this.value = leftNorm + ((this.value - leftOrig) / (rightOrig - leftOrig)) * (rightNorm - leftNorm);

		return this;
	}

	@Override
	public FloatValue denormalize(Number leftOrig, Number leftNorm, Number rightOrig, Number rightNorm){
		return denormalizeInternal(leftOrig.floatValue(), leftNorm.floatValue(), rightOrig.floatValue(), rightNorm.floatValue());
	}

	private FloatValue denormalizeInternal(float leftOrig, float leftNorm, float rightOrig, float rightNorm){
		this.value = ((this.value - leftNorm) / (rightNorm - leftNorm)) * (rightOrig - leftOrig) + leftOrig;

		return this;
	}

	@Override
	public boolean isZero(){
		return this.value == 0f;
	}

	@Override
	public boolean isOne(){
		return this.value == 1f;
	}

	@Override
	public boolean equals(Number value){
		return this.value == value.floatValue();
	}

	@Override
	public int compareTo(Number value){
		return Float.compare(this.value, value.floatValue());
	}

	@Override
	public float floatValue(){
		return this.value;
	}

	@Override
	public double doubleValue(){
		return (double)this.value;
	}

	@Override
	public Float getValue(){
		return this.value;
	}

	/**
	 * <p>
	 * Computes <code>exp(float)</code>.
	 * </p>
	 *
	 * The function <code>exp(float)</code> can be reasonably emulated as <code>(float)Math#pow(2.7182817d, double)</code>.
	 *
	 * The constant <code>2.7182817d</code> has to be hard-coded as double literal, because a float value,
	 * which could be either hard-coded as float literal or computed as <code>(float)Math#E</code>,
	 * would be promoted to a double value <code>2.7182817459106445d</code> (via a widening primitive conversion) by the method invocation expression.
	 */
	static
	public float exp(float value){
		return (float)Math.pow(FloatValue.E, value);
	}

	static
	public float pow(float value, float power){
		return (float)Math.pow(value, power);
	}

	public static final double E = 2.7182817d;
	public static final double PI = 3.1415927d;
}
