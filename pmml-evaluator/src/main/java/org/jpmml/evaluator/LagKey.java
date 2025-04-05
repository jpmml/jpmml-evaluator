/*
 * Copyright (c) 2025 Villu Ruusmann
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

import java.util.List;
import java.util.Objects;

class LagKey {

	private String name = null;

	private int n = -1;

	private List<String> blockIndicatorFields = null;


	LagKey(String name, int n, List<String> blockIndicatorFields){
		setName(name);
		setN(n);
		setBlockIndicatorFields(blockIndicatorFields);
	}

	@Override
	public int hashCode(){
		int result = 1;

		result += (31 * result) + getName().hashCode();
		result += (31 * result) + getN();
		result += (31 * result) + getBlockIndicatorFields().hashCode();

		return result;
	}

	@Override
	public boolean equals(Object object){

		if(object instanceof LagKey){
			LagKey that = (LagKey)object;

			return Objects.equals(this.getName(), that.getName()) && (this.getN() == that.getN()) && Objects.equals(this.getBlockIndicatorFields(), that.getBlockIndicatorFields());
		}

		return false;
	}

	public String getName(){
		return this.name;
	}

	private void setName(String name){
		this.name = Objects.requireNonNull(name);
	}

	public int getN(){
		return this.n;
	}

	private void setN(int n){

		if(n < 1){
			throw new IllegalArgumentException();
		}

		this.n = n;
	}

	public List<String> getBlockIndicatorFields(){
		return this.blockIndicatorFields;
	}

	private void setBlockIndicatorFields(List<String> blockIndicatorFields){
		this.blockIndicatorFields = Objects.requireNonNull(blockIndicatorFields);
	}
}