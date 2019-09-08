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

public class ResourceUtil {

	private ResourceUtil(){
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