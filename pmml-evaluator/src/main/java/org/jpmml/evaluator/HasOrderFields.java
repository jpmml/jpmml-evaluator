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

import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Model;

/**
 * <p>
 * A marker interface for models that expect the application to group and order
 * many scalar-valued data records to a single ordered collection-type data record.
 * The grouping and ordering are applied to {@link #getActiveFields() active field} values.
 * </p>
 */
public interface HasOrderFields extends HasInputFields {

	/**
	 * <p>
	 * Gets the order fields of a {@link Model} from its {@link MiningSchema}.
	 * </p>
	 *
	 * <p>
	 * This field set is relevant for {@link MiningFunction#SEQUENCES sequences} and {@link MiningFunction#TIME_SERIES time series} model types.
	 * </p>
	 */
	List<InputField> getOrderFields();
}