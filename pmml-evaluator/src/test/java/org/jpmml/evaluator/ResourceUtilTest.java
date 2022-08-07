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
import java.util.List;

import javax.xml.namespace.QName;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ResourceUtilTest {

	@Test
	public void readWriteQNames() throws IOException {
		QName[] names = new QName[]{new QName("a"), new QName("b"), new QName("c")};

		ByteArrayOutputStream os = new ByteArrayOutputStream();

		DataOutput dataOutput = new DataOutputStream(os);

		ResourceUtil.writeQNames(dataOutput, names);

		os.close();

		ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());

		DataInput dataInput = new DataInputStream(is);

		QName[] clonedNames = ResourceUtil.readQNames(dataInput, 3);

		assertTrue(Arrays.equals(names, clonedNames));

		try {
			dataInput.readByte();

			fail();
		} catch(EOFException eofe){
			// Ignored
		}

		is.close();
	}

	@Test
	public void readWriteStrings() throws IOException {
		String[] values = {"a", "b", "c"};
		String[][] valueArrays = {{"a", "aa"}, {"b", "bb"}, {"c", "cc"}};
		List<String>[] valueLists = new List[]{Arrays.asList("a"), Arrays.asList("b", "bb"), Arrays.asList("c", "cc", "ccc")};

		ByteArrayOutputStream os = new ByteArrayOutputStream();

		DataOutput dataOutput = new DataOutputStream(os);

		ResourceUtil.writeStrings(dataOutput, values);
		ResourceUtil.writeStringArrays(dataOutput, valueArrays);
		ResourceUtil.writeStringLists(dataOutput, valueLists);

		os.close();

		ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());

		DataInput dataInput = new DataInputStream(is);

		String[] clonedValues = ResourceUtil.readStrings(dataInput, values.length);
		String[][] clonedValueArrays = ResourceUtil.readStringArrays(dataInput, valueArrays.length, 2);
		List<String>[] clonedValueLists = ResourceUtil.readStringLists(dataInput, valueLists.length);

		assertTrue(Arrays.equals(values, clonedValues));
		assertTrue(Arrays.deepEquals(valueArrays, clonedValueArrays));
		assertTrue(Arrays.deepEquals(valueLists, clonedValueLists));

		try {
			dataInput.readByte();

			fail();
		} catch(EOFException eofe){
			// Ignored
		}

		is.close();
	}

	@Test
	public void readWriteIntegers() throws IOException {
		Integer[] values = {0, 1, 2};
		Integer[][] valueArrays = {{0, -0}, {1, -1}, {2, -2}};

		ByteArrayOutputStream os = new ByteArrayOutputStream();

		DataOutput dataOutput = new DataOutputStream(os);

		ResourceUtil.writeIntegers(dataOutput, values);
		ResourceUtil.writeIntegerArrays(dataOutput, valueArrays);

		os.close();

		ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());

		DataInput dataInput = new DataInputStream(is);

		Integer[] clonedValues = ResourceUtil.readIntegers(dataInput, values.length);
		Integer[][] clonedValueArrays = ResourceUtil.readIntegerArrays(dataInput, valueArrays.length, 2);

		assertTrue(Arrays.equals(values, clonedValues));
		assertTrue(Arrays.deepEquals(valueArrays, clonedValueArrays));

		try {
			dataInput.readByte();

			fail();
		} catch(EOFException eofe){
			// Ignored
		}

		is.close();
	}

	@Test
	public void readWriteDoubles() throws IOException {
		Double[] values = {0d, 1d, 2d};
		Double[][] valueArrays = {{0.5d, -0.5d}, {1.5d, -1.5d}, {2.5d, -2.5d}};

		ByteArrayOutputStream os = new ByteArrayOutputStream();

		DataOutput dataOutput = new DataOutputStream(os);

		ResourceUtil.writeDoubles(dataOutput, values);
		ResourceUtil.writeDoubleArrays(dataOutput, valueArrays);

		os.close();

		ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());

		DataInput dataInput = new DataInputStream(is);

		Double[] clonedValues = ResourceUtil.readDoubles(dataInput, values.length);
		Double[][] clonedValueArrays = ResourceUtil.readDoubleArrays(dataInput, valueArrays.length, 2);

		assertTrue(Arrays.equals(values, clonedValues));
		assertTrue(Arrays.deepEquals(valueArrays, clonedValueArrays));

		try {
			dataInput.readByte();

			fail();
		} catch(EOFException eofe){
			// Ignored
		}

		is.close();
	}
}