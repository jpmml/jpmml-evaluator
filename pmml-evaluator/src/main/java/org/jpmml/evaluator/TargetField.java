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

import java.util.Objects;

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.OpType;
import org.dmg.pmml.Target;

public class TargetField extends ResultField {

	private DataField dataField = null;

	private MiningField miningField = null;

	private Target target = null;


	public TargetField(DataField dataField, MiningField miningField, Target target){
		setDataField(Objects.requireNonNull(dataField));
		setMiningField(miningField);
		setTarget(target);
	}

	/**
	 * @return the name, or <code>null</code> (in the form of the constant {@link Evaluator#DEFAULT_TARGET_NAME}) if this is a synthetic target field.
	 *
	 * @see #isSynthetic()
	 */
	@Override
	public FieldName getName(){
		DataField dataField = getDataField();

		return dataField.getName();
	}

	@Override
	public DataType getDataType(){
		DataField dataField = getDataField();

		return dataField.getDataType();
	}

	@Override
	public OpType getOpType(){
		return FieldValueUtil.getOpType(getDataField(), getMiningField(), getTarget());
	}

	public boolean isSynthetic(){
		MiningField miningField = getMiningField();

		return (miningField == null);
	}

	/**
	 * @return the backing {@link DataField} element.
	 */
	public DataField getDataField(){
		return this.dataField;
	}

	private void setDataField(DataField dataField){
		this.dataField = dataField;
	}

	/**
	 * @return the backing {@link MiningField} element, or <code>null</code> if this a synthetic target field.
	 *
	 * @see #isSynthetic()
	 */
	public MiningField getMiningField(){
		return this.miningField;
	}

	private void setMiningField(MiningField miningField){
		this.miningField = miningField;
	}

	/**
	 * @return the backing {@link Target} element, or <code>null</code>.
	 */
	public Target getTarget(){
		return this.target;
	}

	private void setTarget(Target target){
		this.target = target;
	}
}
