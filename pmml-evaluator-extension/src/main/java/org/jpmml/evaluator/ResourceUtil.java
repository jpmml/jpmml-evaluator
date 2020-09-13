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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.dmg.pmml.FieldName;

public class ResourceUtil {

	private ResourceUtil(){
	}

	static
	public FieldName[] readFieldNames(DataInput dataInput, int count) throws IOException {
		String[] strings = readStrings(dataInput, count);

		return Arrays.stream(strings)
			.map(string -> FieldName.create(string))
			.toArray(FieldName[]::new);
	}

	static
	public void writeFieldNames(DataOutput dataOutput, FieldName[] names) throws IOException {
		String[] strings = Arrays.stream(names)
			.map(name -> name.getValue())
			.toArray(String[]::new);

		writeStrings(dataOutput, strings);
	}

	static
	public QName[] readQNames(DataInput dataInput, int count) throws IOException {
		String[][] stringArrays = readStringArrays(dataInput, count, 3);

		return Arrays.stream(stringArrays)
			.map(stringArray -> new QName(stringArray[0], stringArray[1], stringArray[2]))
			.toArray(QName[]::new);
	}

	static
	public void writeQNames(DataOutput dataOutput, QName[] names) throws IOException {
		String[][] stringArrays = Arrays.stream(names)
			.map(name -> new String[]{name.getNamespaceURI(), name.getLocalPart(), name.getPrefix()})
			.toArray(String[][]::new);

		writeStringArrays(dataOutput, stringArrays);
	}

	static
	public String[] readStrings(DataInput dataInput, int count) throws IOException {
		String[] result = new String[count];

		for(int i = 0; i < count; i++){
			result[i] = dataInput.readUTF();
		}

		return result;
	}

	static
	public String[][] readStringArrays(DataInput dataInput, int count, int length) throws IOException {
		String[][] result = new String[count][length];

		for(int i = 0; i < count; i++){
			result[i] = readStrings(dataInput, length);
		}

		return result;
	}

	static
	public List<String>[] readStringLists(DataInput dataInput) throws IOException {
		int count = dataInput.readInt();

		List<List<String>> result = new ArrayList<>(count);

		for(int i = 0; i < count; i++){
			int length = dataInput.readInt();

			String[] strings = readStrings(dataInput, length);

			result.add(Arrays.asList(strings));
		}

		return result.toArray(new List[result.size()]);
	}

	static
	public void writeStrings(DataOutput dataOutput, String[] strings) throws IOException {

		for(String string : strings){
			dataOutput.writeUTF(string);
		}
	}

	static
	public void writeStringArrays(DataOutput dataOutput, String[][] stringArrays) throws IOException {

		for(String[] stringArray : stringArrays){
			writeStrings(dataOutput, stringArray);
		}
	}

	static
	public void writeStringLists(DataOutput dataOutput, List<String>[] stringLists) throws IOException {
		int count = stringLists.length;

		dataOutput.writeInt(count);

		for(List<String> stringList : stringLists){
			int length = stringList.size();

			dataOutput.writeInt(length);

			writeStrings(dataOutput, stringList.toArray(new String[length]));
		}
	}

	static
	public Double[] readDoubles(DataInput dataInput, int count) throws IOException {
		Double[] result = new Double[count];

		for(int i = 0; i < count; i++){
			result[i] = dataInput.readDouble();
		}

		return result;
	}

	static
	public Double[][] readDoubleArrays(DataInput dataInput, int count, int length) throws IOException {
		Double[][] result = new Double[count][length];

		for(int i = 0; i < count; i++){
			result[i] = readDoubles(dataInput, length);
		}

		return result;
	}

	static
	public void writeDoubles(DataOutput dataOutput, Number[] numbers) throws IOException {

		for(Number number : numbers){
			dataOutput.writeDouble(number.doubleValue());
		}
	}

	static
	public void writeDoubleArrays(DataOutput dataOutput, Number[][] numberArrays) throws IOException {

		for(Number[] numberArray : numberArrays){
			writeDoubles(dataOutput, numberArray);
		}
	}

	static
	public Float[] readFloats(DataInput dataInput, int count) throws IOException {
		Float[] result = new Float[count];

		for(int i = 0; i < count; i++){
			result[i] = dataInput.readFloat();
		}

		return result;
	}

	static
	public Float[][] readFloatArrays(DataInput dataInput, int count, int length) throws IOException {
		Float[][] result = new Float[count][length];

		for(int i = 0; i < count; i++){
			result[i] = readFloats(dataInput, length);
		}

		return result;
	}

	static
	public void writeFloats(DataOutput dataOutput, Number[] numbers) throws IOException {

		for(Number number : numbers){
			dataOutput.writeFloat(number.floatValue());
		}
	}

	static
	public void writeFloatArrays(DataOutput dataOutput, Number[][] numberArrays) throws IOException {

		for(Number[] numberArray : numberArrays){
			writeFloats(dataOutput, numberArray);
		}
	}
}