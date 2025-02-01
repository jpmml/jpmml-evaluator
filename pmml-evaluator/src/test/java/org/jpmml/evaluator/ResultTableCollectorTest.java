/*
 * Copyright (c) 2024 Villu Ruusmann
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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.OpType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ResultTableCollectorTest {

	@Test
	public void collect(){
		DataField dataField = new DataField("Species", OpType.CATEGORICAL, DataType.STRING);
		MiningField miningField = new MiningField(dataField.getName());

		List<ResultField> resultFields = Arrays.asList(
			new TargetField(dataField, miningField, null),
			new OutputField(new org.dmg.pmml.OutputField("probability(versicolor)", OpType.CONTINUOUS, DataType.DOUBLE))
		);

		List<Map<String, ?>> results = Arrays.asList(
			createRecord("setosa", 0.7, 0.0, 0.3),
			createRecord("versicolor", 0.2, 0.6, 0.2),
			createRecord("virginica", 0.0, 0.1, 0.9)
		);

		Table table = results.stream()
			.collect(new TableCollector());

		assertEquals(Arrays.asList("probability(setosa)", "probability(versicolor)", "probability(virginica)", "Species"), table.getColumns());

		assertEquals(3, table.getNumberOfRows());
		assertEquals(4, table.getNumberOfColumns());

		table = results.stream()
			.collect(new ResultTableCollector(resultFields, true));

		assertEquals(Arrays.asList("Species", "probability(versicolor)"), table.getColumns());

		assertEquals(3, table.getNumberOfRows());
		assertEquals(2, table.getNumberOfColumns());

		assertFalse(table.hasExceptions());

		assertEquals(Arrays.asList("setosa", "versicolor", "virginica"), table.getValues("Species"));
		assertNull(table.getValues("probability(setosa)"));
		assertEquals(Arrays.asList(0.0, 0.6, 0.1), table.getValues("probability(versicolor)"));
		assertNull(table.getValues("probability(virginica)"));
	}

	static
	private Map<String, ?> createRecord(String species, double probabilitySetosa, double probabilityVersicolor, double probabilityVirginica){
		Computable targetValue = new Computable(){

			@Override
			public Object getResult(){
				return species;
			}
		};

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("probability(setosa)", probabilitySetosa);
		result.put("probability(versicolor)", probabilityVersicolor);
		result.put("probability(virginica)", probabilityVersicolor);
		result.put("Species", targetValue);

		return result;
	}
}