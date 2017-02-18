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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.dmg.pmml.Array;
import org.dmg.pmml.MatCell;
import org.dmg.pmml.Matrix;

public class MatrixUtil {

	private MatrixUtil(){
	}

	/**
	 * @param row The row index. The index of the first row is <code>1</code>.
	 * @param column The column index. The index of the first column is <code>1</code>.
	 *
	 * @return The element at the specified location, or <code>null</code>.
	 *
	 * @throws IndexOutOfBoundsException If either the row or column index is out of range.
	 */
	static
	public Number getElementAt(Matrix matrix, int row, int column){
		List<Array> arrays = matrix.getArrays();
		List<MatCell> matCells = matrix.getMatCells();

		Matrix.Kind kind = matrix.getKind();
		switch(kind){
			case DIAGONAL:
				{
					// "The content is just one Array of numbers representing the diagonal values"
					if(arrays.size() == 1){
						Array array = arrays.get(0);

						List<? extends Number> elements = ArrayUtil.asNumberList(array);

						// Diagonal element
						if(row == column){
							return elements.get(row - 1);
						} else

						// Off-diagonal element
						{
							int min = 1;
							int max = elements.size();

							if((row < min || row > max) || (column < min || column > max)){
								throw new IndexOutOfBoundsException();
							}

							return matrix.getOffDiagDefault();
						}
					}
				}
				break;
			case SYMMETRIC:
				{
					// "The content must be represented by Arrays"
					if(arrays.size() > 0){

						// Make sure the specified coordinates target the lower left triangle
						if(column > row){
							int temp = row;

							row = column;
							column = temp;
						}

						return getArrayValue(arrays, row, column);
					}
				}
				break;
			case ANY:
				{
					if(arrays.size() > 0){
						return getArrayValue(arrays, row, column);
					} // End if

					if(matCells.size() > 0){

						if(row < 1 || column < 1){
							throw new IndexOutOfBoundsException();
						}

						Number value = getMatCellValue(matCells, row, column);
						if(value == null){

							if(row == column){
								return matrix.getDiagDefault();
							}

							return matrix.getOffDiagDefault();
						}

						return value;
					}
				}
				break;
			default:
				throw new UnsupportedFeatureException(matrix, kind);
		}

		throw new InvalidFeatureException(matrix);
	}

	static
	private Number getArrayValue(List<Array> arrays, int row, int column){
		Array array = arrays.get(row - 1);

		List<? extends Number> elements = ArrayUtil.asNumberList(array);

		return elements.get(column - 1);
	}

	static
	private Number getMatCellValue(List<MatCell> matCells, final int row, final int column){
		Predicate<MatCell> filter = new Predicate<MatCell>(){

			@Override
			public boolean apply(MatCell matCell){
				return (matCell.getRow() == row) && (matCell.getCol() == column);
			}
		};

		MatCell matCell = Iterables.getFirst(Iterables.filter(matCells, filter), null);
		if(matCell != null){
			return Double.parseDouble(matCell.getValue());
		}

		return null;
	}

	/**
	 * @return The number of rows.
	 */
	static
	public int getRows(Matrix matrix){
		Integer nbRows = matrix.getNbRows();
		if(nbRows != null){
			return nbRows.intValue();
		}

		List<Array> arrays = matrix.getArrays();
		List<MatCell> matCells = matrix.getMatCells();

		Matrix.Kind kind = matrix.getKind();
		switch(kind){
			case DIAGONAL:
				{
					if(arrays.size() == 1){
						Array array = arrays.get(0);

						return ArrayUtil.getSize(array);
					}
				}
				break;
			case SYMMETRIC:
				{
					if(arrays.size() > 0){
						return arrays.size();
					}
				}
				break;
			case ANY:
				{
					if(arrays.size() > 0){
						return arrays.size();
					} // End if

					if(matCells.size() > 0){
						MatCell matCell = Collections.max(matCells, MatrixUtil.rowComparator);

						return matCell.getRow();
					}
				}
				break;
			default:
				throw new UnsupportedFeatureException(matrix, kind);
		}

		throw new InvalidFeatureException(matrix);
	}

	/**
	 * @return The number of columns.
	 */
	static
	public int getColumns(Matrix matrix){
		Integer nbCols = matrix.getNbCols();
		if(nbCols != null){
			return nbCols.intValue();
		}

		List<Array> arrays = matrix.getArrays();
		List<MatCell> matCells = matrix.getMatCells();

		Matrix.Kind kind = matrix.getKind();
		switch(kind){
			case DIAGONAL:
				{
					if(arrays.size() == 1){
						Array array = arrays.get(0);

						return ArrayUtil.getSize(array);
					}
				}
				break;
			case SYMMETRIC:
				{
					if(arrays.size() > 0){
						return arrays.size();
					}
				}
				break;
			case ANY:
				{
					if(arrays.size() > 0){
						Array array = arrays.get(arrays.size() - 1);

						return ArrayUtil.getSize(array);
					} // End if

					if(matCells.size() > 0){
						MatCell matCell = Collections.max(matCells, MatrixUtil.columnComparator);

						return matCell.getCol();
					}
				}
				break;
			default:
				throw new UnsupportedFeatureException(matrix, kind);
		}

		throw new InvalidFeatureException(matrix);
	}

	private static final Comparator<MatCell> rowComparator = new Comparator<MatCell>(){

		@Override
		public int compare(MatCell left, MatCell right){
			return (left.getRow() - right.getRow());
		}
	};

	private static final Comparator<MatCell> columnComparator = new Comparator<MatCell>(){

		@Override
		public int compare(MatCell left, MatCell right){
			return (left.getCol() - right.getCol());
		}
	};
}