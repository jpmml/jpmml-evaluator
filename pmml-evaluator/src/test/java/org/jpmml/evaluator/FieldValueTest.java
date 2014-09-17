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

import java.util.Arrays;

import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class FieldValueTest {

	@Test
	public void compareToString(){
		FieldValue zero = FieldValueUtil.create(DataType.INTEGER, OpType.CONTINUOUS, 0);

		assertTrue(zero.compareToString("-1") > 0);
		assertTrue(zero.compareToString("-1.5") > 0);

		assertTrue(zero.compareToString("0") == 0);

		assertTrue(zero.compareToString("1") < 0);
		assertTrue(zero.compareToString("1.5") < 0);
	}

	@Test
	public void ordinalString(){
		OrdinalValue loud = new OrdinalValue(DataType.STRING, "loud");
		OrdinalValue louder = new OrdinalValue(DataType.STRING, "louder");
		OrdinalValue insane = new OrdinalValue(DataType.STRING, "insane");

		assertTrue(louder.equalsString("louder"));

		assertTrue(louder.equalsValue(FieldValueUtil.create(DataType.STRING, OpType.CATEGORICAL, "louder")));
		assertTrue(louder.equalsValue(FieldValueUtil.create(DataType.STRING, OpType.ORDINAL, "louder")));

		// Implicit (ie. lexicographic) ordering
		louder.setOrdering(null);

		assertTrue(louder.compareToString("loud") > 0);
		assertTrue(louder.compareToString("louder") == 0);
		assertTrue(louder.compareToString("insane") > 0);

		assertTrue(louder.compareToValue(loud) > 0);
		assertTrue(louder.compareToValue(louder) == 0);
		assertTrue(louder.compareToValue(insane) > 0);

		// Explicit ordering
		louder.setOrdering(Arrays.asList("loud", "louder", "insane"));

		assertTrue(louder.compareToString("loud") > 0);
		assertTrue(louder.compareToString("louder") == 0);
		assertTrue(louder.compareToString("insane") < 0);

		assertTrue(louder.compareToValue(loud) > 0);
		assertTrue(louder.compareToValue(louder) == 0);
		assertTrue(louder.compareToValue(insane) < 0);
	}
}