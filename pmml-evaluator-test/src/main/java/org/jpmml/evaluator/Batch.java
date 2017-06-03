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

import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import org.dmg.pmml.FieldName;

public interface Batch extends AutoCloseable {

	Evaluator getEvaluator() throws Exception;

	/**
	 * <p>
	 * Input data records.
	 * </p>
	 *
	 * @see Evaluator#getInputFields()
	 * @see Evaluator#getGroupFields()
	 */
	List<? extends Map<FieldName, ?>> getInput() throws Exception;

	/**
	 * <p>
	 * Expected output data records.
	 * </p>
	 *
	 * @see Evaluator#getTargetFields()
	 * @see Evaluator#getOutputFields()
	 */
	List<? extends Map<FieldName, ?>> getOutput() throws Exception;

	/**
	 * <p>
	 * Predicate for selecting columns that will be checked for equivalence
	 * (between expected and actual output data records).
	 * </p>
	 */
	Predicate<FieldName> getPredicate();
}