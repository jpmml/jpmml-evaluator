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

import java.io.InputStream;
import java.util.Arrays;

import javax.xml.transform.Source;

import org.dmg.pmml.ModelVerification;
import org.jpmml.model.JAXBSerializer;
import org.jpmml.model.SAXUtil;
import org.jpmml.model.filters.ImportFilter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ModelVerificationTest {

	@Test
	public void iris() throws Exception {
		ModelVerification modelVerification = loadModelVerification("ModelVerificationIris");

		ModelEvaluator.VerificationBatch verificationBatch = ModelEvaluator.parseModelVerification(modelVerification);

		Table table = verificationBatch.getTable();

		assertEquals(4, table.getNumberOfRows());
		assertEquals(10, table.getNumberOfColumns());

		Table.Row row = table.createReaderRow(0);

		assertEquals("1.4", row.get("sepal length"));
		assertEquals("0.2", row.get("sepal width"));
		assertEquals("africa", row.get("continent"));

		row.advance();

		assertEquals("7.0", row.get("sepal length"));
		assertEquals(null, row.get("sepal width"));
		assertEquals("", row.get("continent"));

		row.advance();

		assertEquals(null, row.get("sepal length"));
		assertEquals("0.2", row.get("sepal width"));
		assertEquals(null, row.get("continent"));

		row.advance();

		assertEquals("7.0", row.get("sepal length"));
		assertEquals("0.2", row.get("sepal width"));
		assertEquals("asia", row.get("continent"));
	}

	@Test
	public void shopping() throws Exception {
		ModelVerification modelVerification = loadModelVerification("ModelVerificationShopping");

		ModelEvaluator.VerificationBatch verificationBatch = ModelEvaluator.parseModelVerification(modelVerification);

		Table table = verificationBatch.getTable();

		assertEquals(5, table.getNumberOfRows());
		assertEquals(4, table.getNumberOfColumns());

		table = EvaluatorUtil.groupRows("OrderID", table);

		assertEquals(2, table.getNumberOfRows());
		assertEquals(4, table.getNumberOfColumns());

		Table.Row row = table.createReaderRow(0);

		assertEquals("1", row.get("OrderID"));
		assertEquals(Arrays.asList("Cracker", "Coke", "Water"), row.get("Product"));
		assertEquals(Arrays.asList("1", "", ""), row.get("Rule Id"));
		assertEquals(Arrays.asList("Nachos", "Banana", ""), row.get("Consequent"));

		row.advance();

		assertEquals("2", row.get("OrderID"));
		assertEquals(Arrays.asList("Cracker", "Banana"), row.get("Product"));
		assertEquals(Arrays.asList("3", ""), row.get("Rule Id"));
		assertEquals(Arrays.asList("", "Pear"), row.get("Consequent"));
	}

	static
	public ModelVerification loadModelVerification(String name) throws Exception {
		Class<?> clazz = ModelVerificationTest.class;

		try(InputStream is = clazz.getResourceAsStream("/pmml/" + name + ".pmml")){
			JAXBSerializer serializer = new JAXBSerializer();

			Source source = SAXUtil.createFilteredSource(is, new ImportFilter());

			return (ModelVerification)serializer.unmarshal(source);
		}
	}
}