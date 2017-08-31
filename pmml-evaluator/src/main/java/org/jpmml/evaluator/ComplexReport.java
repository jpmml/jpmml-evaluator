/*
 * Copyright (c) 2017 Villu Ruusmann
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

import java.util.ArrayList;
import java.util.List;

public class ComplexReport extends Report {

	private List<Entry> entries = new ArrayList<>();


	@Override
	public ComplexReport copy(){
		ComplexReport result = new ComplexReport();
		result.setEntries(new ArrayList<>(getEntries()));

		return result;
	}

	@Override
	public void add(Entry entry){
		this.entries.add(entry);
	}

	@Override
	public List<Entry> getEntries(){
		return this.entries;
	}

	private void setEntries(List<Entry> entries){
		this.entries = entries;
	}
}