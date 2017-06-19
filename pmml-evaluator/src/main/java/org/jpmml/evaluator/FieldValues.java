/*
 * Copyright (c) 2017 Villu Ruusmann
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

public interface FieldValues {

	public static final FieldValue CONTINUOUS_DOUBLE_ZERO = FieldValueUtil.create(DataType.DOUBLE, OpType.CONTINUOUS, Numbers.DOUBLE_ZERO);
	public static final FieldValue CONTINUOUS_DOUBLE_ONE = FieldValueUtil.create(DataType.DOUBLE, OpType.CONTINUOUS, Numbers.DOUBLE_ONE);

	public static final FieldValue CATEGORICAL_DOUBLE_ZERO = FieldValueUtil.create(DataType.DOUBLE, OpType.CATEGORICAL, Numbers.DOUBLE_ZERO);
	public static final FieldValue CATEGORICAL_DOUBLE_ONE = FieldValueUtil.create(DataType.DOUBLE, OpType.CATEGORICAL, Numbers.DOUBLE_ONE);
}