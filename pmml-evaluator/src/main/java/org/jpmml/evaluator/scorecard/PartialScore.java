/*
 * Copyright (c) 2021 Villu Ruusmann
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
package org.jpmml.evaluator.scorecard;

import java.util.Objects;

import org.dmg.pmml.scorecard.Attribute;
import org.dmg.pmml.scorecard.Characteristic;
import org.dmg.pmml.scorecard.PMMLAttributes;
import org.jpmml.model.MissingAttributeException;

public class PartialScore {

	private Characteristic characteristic = null;

	private Attribute attribute = null;

	private Number value = null;


	PartialScore(Characteristic characteristic, Attribute attribute, Number value){
		setCharacteristic(characteristic);
		setAttribute(attribute);

		setValue(value);
	}

	public Number getBaselineScore(Number defaultBaselineScore){
		Characteristic characteristic = getCharacteristic();

		Number baselineScore = characteristic.getBaselineScore();
		if(baselineScore == null){
			baselineScore = defaultBaselineScore;
		} // End if

		if(baselineScore == null){
			throw new MissingAttributeException(characteristic, PMMLAttributes.CHARACTERISTIC_BASELINESCORE);
		}

		return baselineScore;
	}

	public String getReasonCode(){
		Characteristic characteristic = getCharacteristic();
		Attribute attribute = getAttribute();

		String reasonCode = attribute.getReasonCode();
		if(reasonCode == null){
			reasonCode = characteristic.getReasonCode();
		} // End if

		if(reasonCode == null){
			throw new MissingAttributeException(attribute, PMMLAttributes.ATTRIBUTE_REASONCODE);
		}

		return reasonCode;
	}

	public Characteristic getCharacteristic(){
		return this.characteristic;
	}

	private void setCharacteristic(Characteristic characteristic){
		this.characteristic = Objects.requireNonNull(characteristic);
	}

	public Attribute getAttribute(){
		return this.attribute;
	}

	private void setAttribute(Attribute attribute){
		this.attribute = Objects.requireNonNull(attribute);
	}

	public Number getValue(){
		return this.value;
	}

	private void setValue(Number value){
		this.value = Objects.requireNonNull(value);
	}
}