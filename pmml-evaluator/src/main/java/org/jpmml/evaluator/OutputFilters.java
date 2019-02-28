/*
 * Copyright (c) 2019 Villu Ruusmann
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

import org.dmg.pmml.OutputField;

public interface OutputFilters {

	/**
	 * <p>
	 * A predicate that keeps all output fields.
	 * </p>
	 */
	OutputFilter KEEP_ALL = new OutputFilter(){

		@Override
		public boolean test(OutputField outputField){
			return true;
		}
	};

	/**
	 * <p>
	 * A predicate that keeps output fields that have been explicitly marked as "final results".
	 * </p>
	 *
	 * @see org.dmg.pmml.OutputField#isFinalResult()
	 */
	OutputFilter KEEP_FINAL_RESULTS = new OutputFilter(){

		@Override
		public boolean test(OutputField outputField){
			return outputField.isFinalResult();
		}
	};
}