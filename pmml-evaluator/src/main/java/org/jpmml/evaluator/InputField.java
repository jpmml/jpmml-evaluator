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
import java.util.Objects;

import com.google.common.collect.RangeSet;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Field;
import org.dmg.pmml.HasContinuousDomain;
import org.dmg.pmml.HasDiscreteDomain;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.OpType;
import org.jpmml.model.MarkupException;

public class InputField extends ModelField {

	private MiningField miningField = null;


	InputField(){
	}

	public InputField(Field<?> field, MiningField miningField){
		super(field);

		setMiningField(miningField);

		if(!Objects.equals(field.requireName(), miningField.requireName())){
			throw new IllegalArgumentException();
		}
	}

	@Override
	public OpType getOpType(){
		return FieldUtil.getOpType(getField(), getMiningField());
	}

	/**
	 * <p>
	 * Prepares an input value for a field.
	 * </p>
	 *
	 * <p>
	 * First, the value is converted from the user-supplied representation to PMML representation.
	 * After that, the value is subjected to missing value treatment, invalid value treatment and outlier treatment.
	 * </p>
	 *
	 * @param value The input value in user-supplied representation.
	 * Use {@code null} to represent a missing input value.
	 *
	 * @throws EvaluationException If the input value preparation fails.
	 * @throws MarkupException
	 */
	public FieldValue prepare(Object value){
		return InputFieldUtil.prepareInputValue(getField(), getMiningField(), value);
	}

	/**
	 * <p>
	 * Prepares a column of input values for a field.
	 * </p>
	 *
	 * <p>
	 * The column is replaced with a new column holding preparation results as {@link FieldValue} objects.
	 * If the preparation of some input value fails, then the corresponding element is set to {@code null},
	 * and the exception is stored in table's {@link Table#getExceptions() exceptions pseudo-column}.
	 * </p>
	 *
	 * @see Table#hasExceptions()
	 * @see Table#clearExceptions()
	 */
	@IgnoreJRERequirement
	public Table prepare(Table table){
		String name = getName();

		List<?> values = table.getValues(name);
		if(values != null){
			table = values.stream()
				.map(this::prepare)
				.collect(new ColumnCollector(table, name));
		}

		return table;
	}

	/**
	 * <p>
	 * Returns the domain of valid values for this continuous field.
	 * If specified, then all input values that are contained in this set shall be considered valid, and all others invalid.
	 * If not specified, then all input values shall be considered valid.
	 * </p>
	 *
	 * @return A non-empty set, or <code>null</code>.
	 *
	 * @see #getOpType()
	 */
	public RangeSet<Double> getContinuousDomain(){
		Field<?> field = getField();

		if(field instanceof HasContinuousDomain){
			RangeSet<Double> validRanges = FieldUtil.getValidRanges((Field & HasContinuousDomain)field);

			if(validRanges != null && !validRanges.isEmpty()){
				return validRanges;
			}
		}

		return null;
	}

	/**
	 * <p>
	 * Returns the domain of valid values for this categorical or ordinal field.
	 * If specified, then all input values that are contained in this list shall be considered valid, and all others invalid.
	 * In not specified, then all input values shall be considered valid.
	 * </p>
	 *
	 * <p>
	 * List elements are all valid values in PMML representation.
	 * For example, if the data type of this field is {@link DataType#INTEGER}, then all list elements shall be {@link Integer}.
	 * </p>
	 *
	 * @return A non-empty list, or <code>null</code>.
	 *
	 * @see #getDataType()
	 * @see #getOpType()
	 *
	 * @see TypeUtil#format(Object)
	 * @see TypeUtil#parse(DataType, String)
	 * @see TypeUtil#parseOrCast(DataType, Object)
	 */
	public List<?> getDiscreteDomain(){
		Field<?> field = getField();

		if(field instanceof HasDiscreteDomain){
			List<?> validValues = FieldUtil.getValidValues((Field & HasDiscreteDomain)field);

			if(validValues != null && !validValues.isEmpty()){
				return validValues;
			}
		}

		return null;
	}

	/**
	 * @return The backing {@link MiningField} element.
	 */
	public MiningField getMiningField(){
		return this.miningField;
	}

	private void setMiningField(MiningField miningField){
		this.miningField = Objects.requireNonNull(miningField);
	}
}
