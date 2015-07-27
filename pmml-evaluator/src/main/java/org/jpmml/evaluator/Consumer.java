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
import org.dmg.pmml.MiningFunctionType;
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
	MiningFunctionType getMiningFunction();

	/**
	 * <p>
	 * Gets the definition of a field from the {@link DataDictionary}.
	 * </p>
	 *
	 * @param name The name of the field. The name of the default target field is <code>null</code>.
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
	 * A model should have no more than 1 group field.
	 * </p>
	 */
	List<FieldName> getGroupFields();

	/**
	 * <p>
	 * Gets the order fields of a {@link Model} from its {@link MiningSchema}.
	 * </p>
	 *
	 * <p>
	 * This field set is relevant only for {@link MiningFunctionType#ASSOCIATION_RULES association rules} model type.
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
	 * Convenience method for retrieving the sole target field.
	 * </p>
	 *
	 * <p>
	 * A supervised model should, but is not required to, define a target field. An unsupervised model, by definition, does not define a target field.
	 * If the {@link #getTargetFields() collection of target fields} is empty, then the model consumer should assume that the model defines a default target field, which is represented by <code>null</code>.
	 * The default target field could be either "real" or "phantom". They can be distinguished from one another by looking up the definition of the field from the {@link DataDictionary}.
	 * </p>
	 *
	 * <pre>
	 * Consumer consumer = ...;
	 *
	 * List&lt;FieldName&gt; targetFields = consumer.getTargetFields();
	 * if(targetFields.isEmpty()){
	 *   FieldName targetField = consumer.getTargetField();
	 *
	 *   DataField dataField = consumer.getDataField(targetField);
	 *   if(dataField != null){
	 *     // A "real" default target field
	 *   } else
	 *
	 *   {
	 *     // A "phantom" default target field
	 *   }
	 * }
	 * </pre>
	 *
	 * @return The sole target field.
	 *
	 * @throws InvalidFeatureException If the number of target fields is greater than one.
	 */
	FieldName getTargetField();

	/**
	 * <p>
	 * Gets the definition of a field from the {@link MiningSchema}.
	 * </p>
	 *
	 * @param name The name of the field.
	 *
	 * @see #getActiveFields()
	 * @see #getGroupFields()
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