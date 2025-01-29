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

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.dmg.pmml.Array;
import org.dmg.pmml.DataType;
import org.dmg.pmml.MatCell;
import org.dmg.pmml.Matrix;
import org.jpmml.model.InvalidElementException;
import org.jpmml.model.UnsupportedAttributeException;

public class MatrixUtil {

	private MatrixUtil(){
	}

	static
	public RealMatrix asRealMatrix(Matrix matrix){
		Integer nbRows = matrix.getNbRows();
		Integer nbCols = matrix.getNbCols();

		List<Array> arrays = matrix.getArrays();
		List<MatCell> matCells = matrix.getMatCells();

		Integer nbMax = nbRows;

		Matrix.Kind kind = matrix.getKind();
		switch(kind){
			case DIAGONAL:
			case SYMMETRIC:
				{
					if(nbCols != null){

						if(nbMax == null){
							nbMax = nbCols;
						} else

						{
							if(nbCols.intValue() != nbMax.intValue()){
								throw new InvalidElementException(matrix);
							}
						}
					}
				}
				break;
			default:
				break;
		} // End switch

		switch(kind){
			case DIAGONAL:
				{
					if(arrays.size() == 1){
						Array array = arrays.get(0);

						List<? extends Number> elements = ArrayUtil.asNumberList(array);

						int max = elements.size();

						if(nbMax != null && max != nbMax.intValue()){
							throw new InvalidElementException(matrix);
						}

						RealMatrix result = MatrixUtils.createRealMatrix(max, max);

						Number offDiagDefault = matrix.getOffDiagDefault();
						if(offDiagDefault != null && offDiagDefault.doubleValue() == 0d){
							offDiagDefault = null;
						}

						for(int i = 0; i < max; i++){
							Number element = elements.get(i);

							result.setEntry(i, i, element.doubleValue());

							if(offDiagDefault != null){

								for(int j = 0; j < max; j++){

									if(i != j){
										result.setEntry(i, j, offDiagDefault.doubleValue());
									}
								}
							}
						}

						return result;
					}
				}
				break;
			case SYMMETRIC:
				{
					if(!arrays.isEmpty()){
						int max = arrays.size();

						if(nbMax != null && max != nbMax.intValue()){
							throw new InvalidElementException(matrix);
						}

						RealMatrix result = MatrixUtils.createRealMatrix(max, max);

						for(int i = 0; i < max; i++){
							Array array = arrays.get(i);

							List<? extends Number> elements = ArrayUtil.asNumberList(array);

							for(int j = 0; j <= i; j++){
								Number element = elements.get(j);

								result.setEntry(i, j, element.doubleValue());

								if(i != j){
									result.setEntry(j, i, element.doubleValue());
								}
							}
						}

						return result;
					}
				}
				break;
			case ANY:
				{
					if(!arrays.isEmpty()){

						if(nbRows != null && arrays.size() != nbRows.intValue()){
							throw new InvalidElementException(matrix);
						}

						RealMatrix result = null;

						for(int i = 0; i < arrays.size(); i++){
							Array array = arrays.get(i);

							List<? extends Number> elements = ArrayUtil.asNumberList(array);

							if(nbCols != null && elements.size() != nbCols.intValue()){
								throw new InvalidElementException(matrix);
							} // End if

							if(result == null){
								result = MatrixUtils.createRealMatrix(arrays.size(), elements.size());
							}

							for(int j = 0; j < elements.size(); j++){
								Number element = elements.get(j);

								result.setEntry(i, j, element.doubleValue());
							}
						}

						return result;
					} // End if

					if(!matCells.isEmpty()){

						if(nbRows == null){
							MatCell matCell = Collections.max(matCells, MatrixUtil.rowComparator);

							nbRows = matCell.requireRow();
						} // End if

						if(nbCols == null){
							MatCell matCell = Collections.max(matCells, MatrixUtil.columnComparator);

							nbCols = matCell.requireCol();
						}

						Number diagDefault = matrix.getDiagDefault();
						if(diagDefault != null && diagDefault.doubleValue() == 0d){
							diagDefault = null;
						}

						Number offDiagDefault = matrix.getOffDiagDefault();
						if(offDiagDefault != null && offDiagDefault.doubleValue() == 0d){
							offDiagDefault = null;
						}

						RealMatrix result = MatrixUtils.createRealMatrix(nbRows, nbCols);

						if(diagDefault != null || offDiagDefault != null){

							for(int i = 0; i < nbRows; i++){

								for(int j = 0; j < nbCols; j++){

									if(i == j){

										if(diagDefault != null){
											result.setEntry(i, j, diagDefault.doubleValue());
										}
									} else

									{
										if(offDiagDefault != null){
											result.setEntry(i, j, offDiagDefault.doubleValue());
										}
									}
								}
							}
						}

						for(MatCell matCell : matCells){
							Number value = (Number)TypeUtil.parseOrCast(DataType.DOUBLE, matCell.getValue());

							result.setEntry(matCell.requireRow() - 1, matCell.requireCol() - 1, value.doubleValue());
						}

						return result;
					}
				}
				break;
			default:
				throw new UnsupportedAttributeException(matrix, kind);
		}

		throw new InvalidElementException(matrix);
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
					if(!arrays.isEmpty()){

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
					if(!arrays.isEmpty()){
						return getArrayValue(arrays, row, column);
					} // End if

					if(!matCells.isEmpty()){

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
				throw new UnsupportedAttributeException(matrix, kind);
		}

		throw new InvalidElementException(matrix);
	}

	static
	private Number getArrayValue(List<Array> arrays, int row, int column){
		Array array = arrays.get(row - 1);

		List<? extends Number> elements = ArrayUtil.asNumberList(array);

		return elements.get(column - 1);
	}

	static
	private Number getMatCellValue(List<MatCell> matCells, int row, int column){

		for(int i = 0, max = matCells.size(); i < max; i++){
			MatCell matCell = matCells.get(i);

			if((matCell.requireRow() == row) && (matCell.requireCol() == column)){
				return (Number)TypeUtil.parseOrCast(DataType.DOUBLE, matCell.getValue());
			}
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
			return nbRows;
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
					if(!arrays.isEmpty()){
						return arrays.size();
					}
				}
				break;
			case ANY:
				{
					if(!arrays.isEmpty()){
						return arrays.size();
					} // End if

					if(!matCells.isEmpty()){
						MatCell matCell = Collections.max(matCells, MatrixUtil.rowComparator);

						return matCell.requireRow();
					}
				}
				break;
			default:
				throw new UnsupportedAttributeException(matrix, kind);
		}

		throw new InvalidElementException(matrix);
	}

	/**
	 * @return The number of columns.
	 */
	static
	public int getColumns(Matrix matrix){
		Integer nbCols = matrix.getNbCols();
		if(nbCols != null){
			return nbCols;
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
					if(!arrays.isEmpty()){
						return arrays.size();
					}
				}
				break;
			case ANY:
				{
					if(!arrays.isEmpty()){
						Array array = arrays.get(arrays.size() - 1);

						return ArrayUtil.getSize(array);
					} // End if

					if(!matCells.isEmpty()){
						MatCell matCell = Collections.max(matCells, MatrixUtil.columnComparator);

						return matCell.requireCol();
					}
				}
				break;
			default:
				throw new UnsupportedAttributeException(matrix, kind);
		}

		throw new InvalidElementException(matrix);
	}

	private static final Comparator<MatCell> rowComparator = new Comparator<>(){

		@Override
		public int compare(MatCell left, MatCell right){
			return (left.requireRow() - right.requireRow());
		}
	};

	private static final Comparator<MatCell> columnComparator = new Comparator<>(){

		@Override
		public int compare(MatCell left, MatCell right){
			return (left.requireCol() - right.requireCol());
		}
	};
}