/*
 * Copyright (c) 2012 University of Tartu
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jpmml.evaluator;

import java.io.Serializable;
import java.util.List;

import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Model;
import org.dmg.pmml.Output;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.Target;
import org.dmg.pmml.Targets;

public interface Consumer extends Serializable {

	/**
	 * <p>
	 * Gets a short description of the {@link Model}.
	 * </p>
	 */
	String getSummary();

	/**
	 * <p>
	 * Gets the type of the {@link Model}.
	 * </p>
	 */
	MiningFunction getMiningFunction();

	/**
	 * <p>
	 * Gets the definition of a field from the {@link DataDictionary}.
	 * </p>
	 */
	DataField getDataField(FieldName name);

	/**
	 * <p>
	 * Gets the independent (ie. input) fields of a {@link Model} from its {@link MiningSchema}.
	 * </p>
	 */
	List<FieldName> getActiveFields();

	/**
	 * <p>
	 * Gets the group fields of a {@link Model} from its {@link MiningSchema}.
	 * </p>
	 *
	 * <p>
	 * This field set is relevant for {@link MiningFunction#ASSOCIATION_RULES association rules} model type only.
	 * </p>
	 */
	List<FieldName> getGroupFields();

	/**
	 * <p>
	 * Gets the order fields of a {@link Model} from its {@link MiningSchema}.
	 * </p>
	 *
	 * <p>
	 * This field set is relevant for {@link MiningFunction#SEQUENCES sequences} and {@link MiningFunction#TIME_SERIES time series} model types.
	 * </p>
	 */
	List<FieldName> getOrderFields();

	/**
	 * <p>
	 * Gets the dependent (ie. target in supervised training) fields of a {@link Model} from its {@link MiningSchema}.
	 * </p>
	 *
	 * @see #getTarget(FieldName)
	 */
	List<FieldName> getTargetFields();

	/**
	 * <p>
	 * Gets the definition of a field from the {@link MiningSchema}.
	 * </p>
	 *
	 * @param name The name of the field.
	 *
	 * @see #getActiveFields()
	 * @see #getGroupFields()
	 * @see #getOrderFields()
	 * @see #getTargetFields()
	 */
	MiningField getMiningField(FieldName name);

	/**
	 * <p>
	 * Gets the definition of a field from the {@link Targets}.
	 * </p>
	 *
	 * @see #getTargetFields()
	 */
	Target getTarget(FieldName name);

	/**
	 * <p>
	 * Gets the output fields of a {@link Model} from its {@link Output}.
	 * </p>
	 *
	 * @see #getOutputField(FieldName)
	 */
	List<FieldName> getOutputFields();

	/**
	 * <p>
	 * Gets the definition of a field from the {@link Output}
	 * </p>
	 *
	 * @see #getOutputFields()
	 */
	OutputField getOutputField(FieldName name);
}