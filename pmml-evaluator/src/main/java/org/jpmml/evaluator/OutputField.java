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

import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.jpmml.model.ToStringHelper;

public class OutputField extends ResultField {

	private int depth = 0;


	private OutputField(){
	}

	public OutputField(org.dmg.pmml.OutputField outputField){
		this(outputField, 0);
	}

	public OutputField(org.dmg.pmml.OutputField outputField, int depth){
		super(outputField);

		if(depth < 0){
			throw new IllegalArgumentException();
		}

		setDepth(depth);
	}

	/**
	 * @return The data type, or <code>null</code>.
	 */
	@Override
	public DataType getDataType(){
		return super.getDataType();
	}

	/**
	 * @return The operational type, or <code>null</code>.
	 */
	@Override
	public OpType getOpType(){
		return super.getOpType();
	}

	/**
	 * <p>
	 * Returns the "finality" (as decided by the PMML producer) of this output field.
	 * Final values are suitable for displaying to the end user.
	 * Non-final values correspond to intermediate states of the prediction
	 * (eg. value transformations, value transfers between models) and are not suitable for displaying to the end user.
	 * </p>
	 *
	 * <p>
	 * Typically, final values are always available in the result data record,
	 * whereas non-final values may or may not be available depending on the "evaluation path" of a particular argument data record.
	 * </p>
	 *
	 * Filtering output fields based on their "finality":
	 * <pre>
	 * List&lt;OutputField&gt; outputFields = evaluator.getOutputFields();
	 * for(OutputField outputField : outputFields){
	 *   boolean finalResult = outputField.isFinalResult();
	 *
	 *   if(!finalResult){
	 *     continue;
	 *   }
	 * }
	 * </pre>
	 */
	public boolean isFinalResult(){
		org.dmg.pmml.OutputField outputField = getField();

		return outputField.isFinalResult();
	}

	@Override
	protected ToStringHelper toStringHelper(){
		ToStringHelper helper = super.toStringHelper()
			.add("finalResult", isFinalResult())
			.add("depth", getDepth());

		return helper;
	}

	/**
	 * @return The backing {@link org.dmg.pmml.OutputField} element.
	 */
	@Override
	public org.dmg.pmml.OutputField getField(){
		return (org.dmg.pmml.OutputField)super.getField();
	}

	/**
	 * <p>
	 * Returns the nesting depth relative to the "host" model evaluator.
	 * </p>
	 *
	 * Filtering output fields based on their origin:
	 * <pre>
	 * List&lt;OutputField&gt; outputFields = evaluator.getOutputFields();
	 * for(OutputField outputField : outputFields){
	 *   int depth = outputField.getDepth();
	 *
	 *   if(depth == 0){
	 *     // Defined by the top-level model
	 *   } else
	 *
	 *   if(depth &gt; 0){
	 *     // Defined by one of the nested models
	 *   }
	 * }
	 * </pre>
	 *
	 * @return The nesting depth.
	 */
	public int getDepth(){
		return this.depth;
	}

	private void setDepth(int depth){
		this.depth = depth;
	}
}
