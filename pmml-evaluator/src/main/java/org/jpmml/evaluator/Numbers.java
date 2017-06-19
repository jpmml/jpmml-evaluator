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

public interface Numbers {

	Integer INTEGER_ZERO = Integer.valueOf(0);
	Integer INTEGER_ONE = Integer.valueOf(1);

	Float FLOAT_MINUS_ONE = Float.valueOf(-1f);
	Float FLOAT_ZERO = Float.valueOf(0f);
	Float FLOAT_ONE = Float.valueOf(1f);

	Double DOUBLE_MINUS_ONE = Double.valueOf(-1d);
	Double DOUBLE_ZERO = Double.valueOf(0d);
	Double DOUBLE_ONE_HALF = Double.valueOf(0.5d);
	Double DOUBLE_ONE = Double.valueOf(1d);
	Double DOUBLE_TWO = Double.valueOf(2d);
}