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

import java.util.*;

import org.dmg.pmml.*;

import org.junit.*;

import static org.junit.Assert.*;

public class MatrixUtilTest {

	@Test
	public void diagonalMatrix(){
		Matrix matrix = new Matrix();
		matrix.setKind(Matrix.Kind.DIAGONAL);

		List<Array> arrays = matrix.getArrays();
		arrays.add(new Array("1 2 3", Array.Type.INT));

		assertEquals(1, MatrixUtil.getElementAt(matrix, 1, 1));
		assertEquals(2, MatrixUtil.getElementAt(matrix, 2, 2));
		assertEquals(3, MatrixUtil.getElementAt(matrix, 3, 3));

		assertEquals(null, MatrixUtil.getElementAt(matrix, 1, 3));
		assertEquals(null, MatrixUtil.getElementAt(matrix, 3, 1));

		matrix.setOffDiagDefault(0d);
		assertEquals(0d, MatrixUtil.getElementAt(matrix, 1, 3));
		assertEquals(0d, MatrixUtil.getElementAt(matrix, 3, 1));

		assertEquals(3, MatrixUtil.getRows(matrix));
		assertEquals(3, MatrixUtil.getColumns(matrix));

		try {
			MatrixUtil.getElementAt(matrix, 0, 0);

			Assert.fail();
		} catch(IndexOutOfBoundsException ioobe){
			// Ignored
		}

		try {
			MatrixUtil.getElementAt(matrix, 4, 4);

			Assert.fail();
		} catch(IndexOutOfBoundsException ioobe){
			// Ignored
		}
	}

	@Test
	public void symmetricMatrix(){
		Matrix matrix = new Matrix();
		matrix.setKind(Matrix.Kind.SYMMETRIC);

		List<Array> arrays = matrix.getArrays();
		arrays.add(new Array("1", Array.Type.INT));
		arrays.add(new Array("4 2", Array.Type.INT));
		arrays.add(new Array("6 5 3", Array.Type.INT));

		assertEquals(1, MatrixUtil.getElementAt(matrix, 1, 1));
		assertEquals(2, MatrixUtil.getElementAt(matrix, 2, 2));
		assertEquals(3, MatrixUtil.getElementAt(matrix, 3, 3));

		assertEquals(4, MatrixUtil.getElementAt(matrix, 1, 2));
		assertEquals(4, MatrixUtil.getElementAt(matrix, 2, 1));
		assertEquals(5, MatrixUtil.getElementAt(matrix, 2, 3));
		assertEquals(5, MatrixUtil.getElementAt(matrix, 3, 2));
		assertEquals(6, MatrixUtil.getElementAt(matrix, 1, 3));
		assertEquals(6, MatrixUtil.getElementAt(matrix, 1, 3));

		assertEquals(3, MatrixUtil.getRows(matrix));
		assertEquals(3, MatrixUtil.getColumns(matrix));

		try {
			MatrixUtil.getElementAt(matrix, 0, 0);

			Assert.fail();
		} catch(IndexOutOfBoundsException ioobe){
			// Ignored
		}

		try {
			MatrixUtil.getElementAt(matrix, 4, 4);

			Assert.fail();
		} catch(IndexOutOfBoundsException ioobe){
			// Ignored
		}
	}

	@Test
	public void anyMatrixDense(){
		Matrix matrix = new Matrix();

		List<Array> arrays = matrix.getArrays();
		arrays.add(new Array("0 0 0 42 0", Array.Type.REAL));
		arrays.add(new Array("0 1 0 0 0", Array.Type.REAL));
		arrays.add(new Array("5 0 0 0 0", Array.Type.REAL));
		arrays.add(new Array("0 0 0 0 7", Array.Type.REAL));
		arrays.add(new Array("0 0 9 0 0", Array.Type.REAL));

		anyMatrix(matrix);
	}

	@Test
	public void anyMatrixSparse(){
		Matrix matrix = new Matrix();

		List<MatCell> matCells = matrix.getMatCells();
		matCells.add(new MatCell("42", 1, 4));
		matCells.add(new MatCell("1", 2, 2));
		matCells.add(new MatCell("5", 3, 1));
		matCells.add(new MatCell("7", 4, 5));
		matCells.add(new MatCell("9", 5, 3));

		matrix.setDiagDefault(0d);
		matrix.setOffDiagDefault(0d);

		anyMatrix(matrix);
	}

	static
	private void anyMatrix(Matrix matrix){
		assertEquals(42d, MatrixUtil.getElementAt(matrix, 1, 4));
		assertEquals(1d, MatrixUtil.getElementAt(matrix, 2, 2));
		assertEquals(5d, MatrixUtil.getElementAt(matrix, 3, 1));
		assertEquals(7d, MatrixUtil.getElementAt(matrix, 4, 5));
		assertEquals(9d, MatrixUtil.getElementAt(matrix, 5, 3));

		assertEquals(0d, MatrixUtil.getElementAt(matrix, 1, 1));
		assertEquals(0d, MatrixUtil.getElementAt(matrix, 1, 5));
		assertEquals(0d, MatrixUtil.getElementAt(matrix, 5, 1));
		assertEquals(0d, MatrixUtil.getElementAt(matrix, 5, 5));

		assertEquals(5, MatrixUtil.getRows(matrix));
		assertEquals(5, MatrixUtil.getColumns(matrix));

		try {
			MatrixUtil.getElementAt(matrix, 0, 0);

			Assert.fail();
		} catch(IndexOutOfBoundsException ioobe){
			// Ignored
		}
	}
}