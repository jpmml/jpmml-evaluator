/*
 * Copyright (c) 2018 Villu Ruusmann
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

public interface TypeInfos {

	TypeInfo CATEGORICAL_STRING = new SimpleTypeInfo(OpType.CATEGORICAL, DataType.STRING);
	TypeInfo ORDINAL_STRING = new SimpleTypeInfo(OpType.ORDINAL, DataType.STRING);

	TypeInfo CATEGORICAL_INTEGER = new SimpleTypeInfo(OpType.CATEGORICAL, DataType.INTEGER);
	TypeInfo CONTINUOUS_INTEGER = new SimpleTypeInfo(OpType.CONTINUOUS, DataType.INTEGER);

	TypeInfo CATEGORICAL_FLOAT = new SimpleTypeInfo(OpType.CATEGORICAL, DataType.FLOAT);
	TypeInfo CONTINUOUS_FLOAT = new SimpleTypeInfo(OpType.CONTINUOUS, DataType.FLOAT);

	TypeInfo CATEGORICAL_DOUBLE = new SimpleTypeInfo(OpType.CATEGORICAL, DataType.DOUBLE);
	TypeInfo CONTINUOUS_DOUBLE = new SimpleTypeInfo(OpType.CONTINUOUS, DataType.DOUBLE);

	TypeInfo CATEGORICAL_BOOLEAN = new SimpleTypeInfo(OpType.CATEGORICAL, DataType.BOOLEAN);
}