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
import org.dmg.pmml.Field;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.OpType;

public class InputField extends ModelField {

	private Field field = null;

	private MiningField miningField = null;


	public InputField(Field field, MiningField miningField){
		setField(Objects.requireNonNull(field));
		setMiningField(Objects.requireNonNull(miningField));

		if(!Objects.equals(field.getName(), miningField.getName())){
			throw new IllegalArgumentException();
		}
	}

	/**
	 * <p>
	 * Prepares the input value for a field.
	 * </p>
	 *
	 * <p>
	 * First, the value is converted from the user-supplied representation to PMML representation.
	 * After that, the value is subjected to missing value treatment, invalid value treatment and outlier treatment.
	 * </p>
	 *
	 * @param value The input value in user-supplied representation.
	 * Use <code>null</code> to represent a missing input value.
	 *
	 * @throws EvaluationException If the input value preparation fails.
	 * @throws InvalidFeatureException
	 * @throws UnsupportedFeatureException
	 */
	public FieldValue prepare(Object value){
		return FieldValueUtil.prepareInputValue(getField(), getMiningField(), value);
	}

	@Override
	public FieldName getName(){
		Field field = getField();

		return field.getName();
	}

	@Override
	public DataType getDataType(){
		Field field = getField();

		return field.getDataType();
	}

	@Override
	public OpType getOpType(){
		return FieldValueUtil.getOpType(getField(), getMiningField());
	}

	/**
	 * @return the backing {@link Field} element.
	 * For top-level models, this is always the {@link DataField} element.
	 */
	public Field getField(){
		return this.field;
	}

	private void setField(Field field){
		this.field = field;
	}

	/**
	 * @return the backing {@link MiningField} element.
	 */
	public MiningField getMiningField(){
		return this.miningField;
	}

	private void setMiningField(MiningField miningField){
		this.miningField = miningField;
	}
}