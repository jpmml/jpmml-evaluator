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

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;

public interface FieldValues {

	FieldValue MISSING_VALUE = null;

	FieldValue CONTINUOUS_DOUBLE_ZERO = FieldValueUtil.create(TypeInfos.CONTINUOUS_DOUBLE, Numbers.DOUBLE_ZERO);
	FieldValue CONTINUOUS_DOUBLE_ONE = FieldValueUtil.create(TypeInfos.CONTINUOUS_DOUBLE, Numbers.DOUBLE_ONE);

	FieldValue CATEGORICAL_DOUBLE_ZERO = FieldValueUtil.create(TypeInfos.CATEGORICAL_DOUBLE, Numbers.DOUBLE_ZERO);
	FieldValue CATEGORICAL_DOUBLE_ONE = FieldValueUtil.create(TypeInfos.CATEGORICAL_DOUBLE, Numbers.DOUBLE_ONE);

	FieldValue CATEGORICAL_BOOLEAN_TRUE = FieldValueUtil.create(TypeInfos.CATEGORICAL_BOOLEAN, true);
	FieldValue CATEGORICAL_BOOLEAN_FALSE = FieldValueUtil.create(TypeInfos.CATEGORICAL_BOOLEAN, false);

	Interner<FieldValue> INTERNER = Interners.newWeakInterner();
}