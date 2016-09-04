/*
 * Copyright (c) 2016 Villu Ruusmann
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

import org.dmg.pmml.DataField;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.OpType;
import org.dmg.pmml.Target;

public class TargetReferenceField extends InputField {

	private Target target = null;


	public TargetReferenceField(DataField dataField, MiningField miningField, Target target){
		super(dataField, miningField);

		setTarget(target);
	}

	@Override
	public FieldValue prepare(Object value){
		return FieldValueUtil.prepareTargetValue(getField(), getMiningField(), getTarget(), value);
	}

	@Override
	public OpType getOpType(){
		return FieldValueUtil.getOpType(getField(), getMiningField(), getTarget());
	}

	@Override
	public DataField getField(){
		return (DataField)super.getField();
	}

	public Target getTarget(){
		return this.target;
	}

	private void setTarget(Target target){
		this.target = target;
	}
}