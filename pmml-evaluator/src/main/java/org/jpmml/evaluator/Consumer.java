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

import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Model;
import org.dmg.pmml.Output;

/**
 * @see HasGroupFields
 * @see HasOrderFields
 */
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
	 * Gets the independent (ie. input) fields of a {@link Model} from its {@link MiningSchema}.
	 * </p>
	 */
	List<InputField> getActiveFields();

	/**
	 * <p>
	 * Gets the dependent (ie. target in supervised training) fields of a {@link Model} from its {@link MiningSchema}.
	 * </p>
	 */
	List<TargetField> getTargetFields();

	/**
	 * <p>
	 * Gets the output fields of a {@link Model} from its {@link Output}.
	 * </p>
	 */
	List<OutputField> getOutputFields();

	/**
	 * <p>
	 * The name of the default target field.
	 * </p>
	 */
	public static final FieldName DEFAULT_TARGET_NAME = null;
}