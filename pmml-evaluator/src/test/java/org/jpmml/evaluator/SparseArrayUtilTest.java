/*
 * Copyright (c) 2014 Villu Ruusmann
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

import java.util.Arrays;

import org.dmg.pmml.IntSparseArray;
import org.dmg.pmml.RealSparseArray;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SparseArrayUtilTest {

	@Test
	public void intSparseArray(){
		IntSparseArray sparseArray = new IntSparseArray()
			.setN(7)
			.addIndices(2, 5)
			.addEntries(3, 42);

		assertEquals(Arrays.asList(0, 3, 0, 0, 42, 0, 0), SparseArrayUtil.asNumberList(sparseArray));
	}

	@Test
	public void realSparseArray(){
		RealSparseArray sparseArray = new RealSparseArray()
			.setN(7);

		assertEquals(Arrays.asList(0d, 0d, 0d, 0d, 0d, 0d, 0d), SparseArrayUtil.asNumberList(sparseArray));
	}
}