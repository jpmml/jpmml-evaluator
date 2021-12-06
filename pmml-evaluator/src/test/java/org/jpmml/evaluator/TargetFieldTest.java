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

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.OpType;
import org.dmg.pmml.Target;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TargetFieldTest {

	@Test
	public void getName(){
		DataField dataField = new DataField("y", OpType.CONTINUOUS, DataType.DOUBLE);

		TargetField targetField = new TargetField(dataField, null, null);

		assertEquals("y", targetField.getName());

		targetField.setName("label");

		assertNotEquals("y", targetField.getName());

		targetField.setName(null);

		assertEquals("y", targetField.getName());
	}

	@Test
	public void getOpType(){
		DataField dataField = new DataField("y", OpType.CONTINUOUS, DataType.DOUBLE);

		MiningField miningField = new MiningField(dataField.getName())
			.setOpType(OpType.CATEGORICAL);

		Target target = new Target()
			.setField(dataField.getName())
			.setOpType(OpType.CONTINUOUS);

		TargetField targetField = new TargetField(dataField, null, null);

		assertEquals(OpType.CONTINUOUS, targetField.getOpType());

		targetField = new TargetField(dataField, miningField, null);

		assertEquals(OpType.CATEGORICAL, targetField.getOpType());

		targetField = new TargetField(dataField, miningField, target);

		assertEquals(OpType.CONTINUOUS, targetField.getOpType());
	}
}