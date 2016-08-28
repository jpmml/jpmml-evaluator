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

import com.google.common.base.Objects.ToStringHelper;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OpType;

public class OutputField extends ResultField {

	private org.dmg.pmml.OutputField outputField = null;

	private int depth = 0;


	public OutputField(OutputField outputField){
		this(outputField.getOutputField(), outputField.getDepth() + 1);
	}

	public OutputField(org.dmg.pmml.OutputField outputField){
		this(outputField, 0);
	}

	public OutputField(org.dmg.pmml.OutputField outputField, int depth){
		setOutputField(Objects.requireNonNull(outputField));

		if(depth < 0){
			throw new IllegalArgumentException();
		}

		setDepth(depth);
	}

	@Override
	public FieldName getName(){
		org.dmg.pmml.OutputField outputField = getOutputField();

		return outputField.getName();
	}

	/**
	 * @return the data type, or <code>null</code>.
	 *
	 * @see OutputUtil#getDataType(org.dmg.pmml.OutputField, ModelEvaluator)
	 */
	@Override
	public DataType getDataType(){
		org.dmg.pmml.OutputField outputField = getOutputField();

		return outputField.getDataType();
	}

	/**
	 * @return the operational type, or <code>null</code>.
	 */
	@Override
	public OpType getOpType(){
		org.dmg.pmml.OutputField outputField = getOutputField();

		return outputField.getOpType();
	}

	@Override
	protected ToStringHelper toStringHelper(){
		ToStringHelper helper = super.toStringHelper()
			.add("depth", getDepth());

		return helper;
	}

	/**
	 * @return the backing {@link org.dmg.pmml.OutputField} element.
	 */
	public org.dmg.pmml.OutputField getOutputField(){
		return this.outputField;
	}

	private void setOutputField(org.dmg.pmml.OutputField outputField){
		this.outputField = outputField;
	}

	/**
	 * <p>
	 * Returns the nesting depth relative to the "host" {@link Evaluator} instance.
	 * </p>
	 *
	 * <p>
	 * Filtering output fields based on their origin:
	 * <pre>
	 * Evaluator evaluator = ...;
	 * List&lt;OutputField&gt; outputFields = evaluator.getOutputFields();
	 * for(OutputField outputField : outputFields){
	 *   int depth = outputField.getDepth();
	 *
	 *   if(depth == 0){
	 *     // Defined by the top-level model
	 *   } else
	 *
	 *   if(depth > 0){
	 *     // Defined by one of the nested models
	 *   }
	 * }
	 * </pre>
	 * </p>
	 *
	 * @return the nesting depth.
	 */
	public int getDepth(){
		return this.depth;
	}

	private void setDepth(int depth){
		this.depth = depth;
	}
}