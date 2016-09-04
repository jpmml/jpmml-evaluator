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

import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.Model;

/**
 * <p>
 * Performs the evaluation of a {@link Model}.
 * </p>
 *
 * <h3>Obtaining and verifying an Evaluator instance</h3>
 * <pre>
 * PMML pmml = ...;
 * ModelEvaluatorFactory modelEvaluatorFactory = ModelEvaluatorFactory.newInstance();
 * Evaluator evaluator = (Evaluator)modelEvaluatorFactory.newModelEvaluator(pmml);
 * evaluator.verify();
 * </pre>
 *
 * <h3>Preparing arguments</h3>
 * Converting an user-supplied map of arguments to a prepared map of arguments:
 * <pre>
 * Map&lt;FieldName, ?&gt; userArguments = ...;
 * Map&lt;FieldName, FieldValue&gt; arguments = new LinkedHashMap&lt;&gt;();
 * List&lt;InputField&gt; inputFields = evaluator.getInputFields();
 * for(InputField inputField : inputFields){
 *   FieldName inputFieldName = inputField.getName();
 *   FieldValue inputFieldValue = inputField.prepare(userArguments.get(inputFieldName));
 *   arguments.put(inputFieldName, inputFieldValue);
 * }
 * </pre>
 *
 * <h3>Performing the evaluation</h3>
 * <pre>
 * Map&lt;FieldName, ?&gt; result = evaluator.evaluate(arguments);
 * </pre>
 *
 * <h3>Processing results</h3>
 * Retrieving the values of {@link #getTargetField() target fields} (ie. primary results):
 * <pre>
 * List&lt;TargetField&gt; targetFields = evaluator.getTargetFields();
 * for(TargetField targetField : targetFields){
 *   FieldName targetFieldName = targetField.getName();
 *   Object targetFieldValue = result.get(targetFieldName);
 * }
 * </pre>
 *
 * Decoding a {@link Computable complex value} to a Java primitive value:
 * <pre>
 * if(targetFieldValue instanceof Computable){
 *   Computable computable = (Computable)targetFieldValue;
 *
 *   targetFieldValue = computable.getResult();
 * }
 * </pre>
 *
 * Retrieving the values of {@link #getOutputFields() output fields} (ie. secondary results):
 * <pre>
 * List&lt;OutputField&gt; outputFields = evaluator.getOutputFields();
 * for(OutputField outputField : outputFields){
 *   FieldName outputFieldName = outputField.getName();
 *   Object outputFieldValue = result.get(outputFieldName);
 * }
 * </pre>
 *
 * <h3>Handling exceptions</h3>
 * A code block that does exception-prone work should be surrounded with two levels of try-catch statements.
 * The inner try statement should catch {@link EvaluationException} instances that indicate "local" problems, which are related to individual data records.
 * The outer try statement should catch {@link InvalidFeatureException} and {@link UnsupportedFeatureException} instances that indicate "global" problems, which are related to the class model object.
 * <pre>
 * try {
 *   List&lt;Map&lt;FieldName, ?&gt;&gt; records = ...;
 *   for(Map&lt;FieldName, ?&gt; record : records){
 *     try {
 *       // Do exception-prone work
 *     } catch(EvaluationException ee){
 *       // The work failed because of the data record.
 *       // Skip this data record and proceed as usual with the next one
 *     }
 *   }
 * } catch(InvalidFeatureException | UnsupportedFeatureException fe){
 *   // The work failed because of the class model object.
 *   // This is a persistent problem that is very likely to affect all data records
 *   // Decommission the Evaluator instance
 * }
 * </pre>
 *
 * @see EvaluatorUtil
 *
 * @see HasGroupFields
 * @see HasOrderFields
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
	 * @throws InvalidFeatureException
	 * @throws UnsupportedFeatureException
	 */
	void verify();

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
	 * @throws InvalidFeatureException
	 * @throws UnsupportedFeatureException
	 *
	 * @see Computable
	 */
	Map<FieldName, ?> evaluate(Map<FieldName, ?> arguments);

	/**
	 * <p>
	 * The name of the default target field.
	 * </p>
	 */
	public static final FieldName DEFAULT_TARGET_NAME = null;
}