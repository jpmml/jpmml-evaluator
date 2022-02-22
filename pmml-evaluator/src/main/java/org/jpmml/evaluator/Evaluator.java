/*
 * Copyright (c) 2013 Villu Ruusmann
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

import java.util.Map;

import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.Model;
import org.jpmml.model.MarkupException;

/**
 * <p>
 * Performs the evaluation of a {@link Model}.
 * </p>
 *
 * <strong>Building and verifying an Evaluator instance</strong>
 * <pre>{@code
 * EvaluatorBuilder evaluatorBuilder = new LoadingModelEvaluatorBuilder()
 *   .load(new File("model.pmml"));
 * Evaluator evaluator = evaluatorBuilder.build();
 * evaluator.verify();
 * }</pre>
 *
 * <strong>Preparing arguments</strong>
 * <br>
 * Transforming an user-supplied map of arguments to a known-good PMML map of arguments:
 * <pre>{@code
 * Map<String, ?> userArguments = ...;
 * Map<String, FieldValue> arguments = new LinkedHashMap<>();
 * List<InputField> inputFields = evaluator.getInputFields();
 * for(InputField inputField : inputFields){
 *   String inputName = inputField.getName();
 *   Object rawValue = userArguments.get(inputName);
 *   FieldValue inputValue = inputField.prepare(rawValue);
 *   arguments.put(inputName, inputValue);
 * }
 * }</pre>
 *
 * <strong>Performing the evaluation</strong>
 * <pre>{@code
 * Map<String, ?> results = evaluator.evaluate(arguments);
 * }</pre>
 *
 * <strong>Processing results</strong>
 * <br>
 * Retrieving the values of {@link #getTargetFields() target fields} (ie. primary results):
 * <pre>{@code
 * List<TargetField> targetFields = evaluator.getTargetFields();
 * for(TargetField targetField : targetFields){
 *   String targetName = targetField.getName();
 *   Object targetValue = results.get(targetName);
 * }
 * }</pre>
 *
 * Decoding a {@link Computable complex value} to a Java primitive value:
 * <pre>{@code
 * if(targetValue instanceof Computable){
 *   Computable computable = (Computable)targetValue;
 *   targetValue = computable.getResult();
 * }
 * }</pre>
 *
 * Retrieving the values of {@link #getOutputFields() output fields} (ie. secondary results):
 * <pre>{@code
 * List<OutputField> outputFields = evaluator.getOutputFields();
 * for(OutputField outputField : outputFields){
 *   String outputName = outputField.getName();
 *   Object outputValue = results.get(outputName);
 * }
 * }</pre>
 *
 * <strong>Handling exceptions</strong>
 * <br>
 * A code block that does exception-prone work should be surrounded with two levels of try-catch statements.
 * The inner try statement should catch {@link EvaluationException} instances that indicate "local" problems, which are related to individual data records.
 * The outer try statement should catch {@link MarkupException} instances that indicate "global" problems, which are related to the class model object.
 * <pre>{@code
 * try {
 *   Lis<Map<String, ?>> records = ...;
 *   for(Map<String, ?> record : records){
 *     try {
 *       // Do exception-prone work
 *     } catch(EvaluationException ee){
 *       // The work failed because of the data record.
 *       // Skip this data record and proceed as usual with the next one
 *     }
 *   }
 * } catch(MarkupException me){
 *   // The work failed because of the class model object.
 *   // This is a persistent problem that is very likely to affect all data records
 *   // Decommission the Evaluator instance
 * }
 * }</pre>
 *
 * @see EvaluatorUtil
 *
 * @see HasGroupFields
 * @see HasOrderFields
 *
 * @see HasPMML
 * @see HasModel
 */
public interface Evaluator extends HasInputFields, HasResultFields {

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
	 * Verifies the model.
	 * </p>
	 *
	 * @throws EvaluationException If the verification fails.
	 * @throws MarkupException
	 */
	Evaluator verify();

	/**
	 * <p>
	 * Evaluates the model with the specified arguments.
	 * </p>
	 *
	 * @param arguments Map of {@link #getInputFields() input field} values.
	 *
	 * @return Map of {@link #getTargetFields() target field} and {@link #getOutputFields() output field} values.
	 * A target field could be mapped to a complex value or a simple value.
	 * An output field is always mapped to a simple value.
	 * Complex values are represented as instances of {@link Computable} that return simple values.
	 * Simple values are represented using the Java equivalents of PMML data types (eg. String, Integer, Float, Double etc.).
	 * A missing value is represented by <code>null</code>.
	 *
	 * @throws EvaluationException If the evaluation fails.
	 * @throws MarkupException
	 *
	 * @see Computable
	 */
	Map<String, ?> evaluate(Map<String, ?> arguments);

	/**
	 * <p>
	 * The name of the default target field.
	 * </p>
	 */
	String DEFAULT_TARGET_NAME = null;
}
