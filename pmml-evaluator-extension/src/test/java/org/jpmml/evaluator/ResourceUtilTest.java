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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ResourceUtilTest {

	@Test
	public void readWriteDoubles() throws IOException {
		Double[] values = {0d, 1d, 2d};
		Double[][] valueArrays = {{0d, 0d}, {1d, 1d}, {2d, 2d}};

		ByteArrayOutputStream os = new ByteArrayOutputStream();

		DataOutput dataOutput = new DataOutputStream(os);

		ResourceUtil.writeDoubles(dataOutput, values);
		ResourceUtil.writeDoubleArrays(dataOutput, valueArrays);

		os.close();

		ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());

		DataInput dataInput = new DataInputStream(is);

		Double[] clonedValues = ResourceUtil.readDoubles(dataInput, 3);
		Double[][] clonedValueArrays = ResourceUtil.readDoubleArrays(dataInput, 3, 2);

		assertTrue(Arrays.equals(values, clonedValues));
		assertTrue(Arrays.deepEquals(valueArrays, clonedValueArrays));

		try {
			dataInput.readByte();

			fail();
		} catch(EOFException ee){
			// Ignored
		}
	}
}