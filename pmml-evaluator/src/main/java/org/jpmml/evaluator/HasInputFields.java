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

import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Model;

public interface HasInputFields {

	List<InputField> getInputFields();

	/**
	 * <p>
	 * Gets the active fields of a {@link Model} from its {@link MiningSchema}.
	 * </p>
	 */
	List<InputField> getActiveFields();

	/**
	 * <p>
	 * Gets the supplementary fields of a {@link Model} from its {@link MiningSchema}.
	 * </p>
	 */
	List<InputField> getSupplementaryFields();
}