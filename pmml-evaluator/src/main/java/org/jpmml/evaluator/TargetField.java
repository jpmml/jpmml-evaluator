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

import java.util.List;

import org.dmg.pmml.DataField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.OpType;
import org.dmg.pmml.Target;

public class TargetField extends ResultField {

	private MiningField miningField = null;

	private Target target = null;


	TargetField(){
	}

	public TargetField(DataField dataField, MiningField miningField, Target target){
		super(dataField);

		setMiningField(miningField);
		setTarget(target);
	}

	/**
	 * @return The name, or <code>null</code> (in the form of the constant {@link Evaluator#DEFAULT_TARGET_NAME}) if this is a synthetic target field.
	 *
	 * @see #isSynthetic()
	 */
	@Override
	public FieldName getFieldName(){
		return super.getFieldName();
	}

	@Override
	public OpType getOpType(){
		return FieldUtil.getOpType(getField(), getMiningField(), getTarget());
	}

	/**
	 * <p>
	 * Returns the range of categories for this categorical or ordinal field.
	 * </p>
	 *
	 * @return A non-empty list, or <code>null</code>.
	 *
	 * @see #getOpType()
	 *
	 * @see CategoricalResultFeature
	 * @see CategoricalResultFeature#getCategories()
	 */
	public List<Object> getCategories(){
		List<Object> categories = FieldUtil.getCategories(getField());

		if(categories != null && !categories.isEmpty()){
			return categories;
		}

		return null;
	}

	public boolean isSynthetic(){
		MiningField miningField = getMiningField();

		return (miningField == null);
	}

	/**
	 * @return The backing {@link DataField} element.
	 */
	@Override
	public DataField getField(){
		return (DataField)super.getField();
	}

	/**
	 * @return The backing {@link MiningField} element, or <code>null</code> if this a synthetic target field.
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
	 * @return The backing {@link Target} element, or <code>null</code>.
	 */
	public Target getTarget(){
		return this.target;
	}

	private void setTarget(Target target){
		this.target = target;
	}
}
