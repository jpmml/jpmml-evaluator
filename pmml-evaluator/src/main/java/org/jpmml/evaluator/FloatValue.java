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

public class FloatValue extends Value<Float> {

	protected float value = 0f;


	@Operation (
		value = "${0}"
	)
	public FloatValue(float value){
		this.value = value;
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
	public FloatValue add(double value){
		this.value += (float)value;

		return this;
	}

	@Override
	public FloatValue add(Value<? extends Number> value){
		this.value += value.floatValue();

		return this;
	}

	@Override
	public FloatValue add(double coefficient, Number factor){
		this.value += (float)coefficient * factor.floatValue();

		return this;
	}

	@Override
	public FloatValue add(double coefficient, Number factor, int exponent){
		this.value += (float)coefficient * FloatValue.pow(factor.floatValue(), exponent);

		return this;
	}

	@Override
	public FloatValue add(double coefficient, List<? extends Number> factors){
		float value = (float)coefficient;

		for(int i = 0; i < factors.size(); i++){
			Number factor = factors.get(i);

			value *= factor.floatValue();
		}

		this.value += value;

		return this;
	}

	@Override
	public FloatValue subtract(double value){
		this.value -= (float)value;

		return this;
	}

	@Override
	public FloatValue subtract(Value<? extends Number> value){
		this.value -= value.floatValue();

		return this;
	}

	@Override
	public FloatValue multiply(double value){
		this.value *= (float)value;

		return this;
	}

	@Override
	public FloatValue multiply(Value<? extends Number> value){
		this.value *= value.floatValue();

		return this;
	}

	@Override
	public FloatValue multiply(Number factor, double exponent){
		this.value *= FloatValue.pow(factor.floatValue(), (float)exponent);

		return this;
	}

	@Override
	public FloatValue divide(double value){
		this.value /= (float)value;

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
	public FloatValue inverseNegbin(double value){
		this.value = 1f / ((float)value * (FloatValue.exp(-this.value) - 1f));

		return this;
	}

	@Override
	public FloatValue inverseOddspower(double value){

		if(value < 0d || value > 0d){
			this.value = 1f / (1f + FloatValue.pow(1f + ((float)value * this.value), -(1f / (float)value)));
		} else

		{
			this.value = 1f / (1f + FloatValue.exp(-this.value));
		}

		return this;
	}

	@Override
	public FloatValue inversePower(double value){

		if(value < 0d || value > 0d){
			this.value = FloatValue.pow(this.value, 1f / (float)value);
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
		throw new EvaluationException();
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
	public FloatValue atan(){
		this.value = (float)Math.atan(this.value);

		return this;
	}

	@Override
	public FloatValue tanh(){
		this.value = (float)Math.tanh(this.value);

		return this;
	}

	@Override
	public FloatValue threshold(double value){
		this.value = (this.value > (float)value ? 1f : 0f);

		return this;
	}

	@Override
	public FloatValue relu(){
		this.value = Math.max(this.value, 0);

		return this;
	}

	@Override
	public FloatValue restrict(double lowValue, double highValue){
		this.value = Math.max(this.value, (float)lowValue);
		this.value = Math.min(this.value, (float)highValue);

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
	public FloatValue denormalize(double leftOrig, double leftNorm, double rightOrig, double rightNorm){
		this.value = ((this.value - (float)leftNorm) / ((float)rightNorm - (float)leftNorm)) * ((float)rightOrig - (float)leftOrig) + (float)leftOrig;

		return this;
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