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
package org.jpmml.evaluator.reporting;

import org.dmg.pmml.MathContext;
import org.jpmml.evaluator.HasReport;
import org.jpmml.evaluator.Numbers;
import org.jpmml.evaluator.Report;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueFactoryFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReportingValueFactoryTest {

	private ValueFactoryFactory valueFactoryFactory = ReportingValueFactoryFactory.newInstance();


	@Test
	public void newSilentZero(){
		ValueFactory<Float> valueFactory = (ValueFactory)this.valueFactoryFactory.newValueFactory(MathContext.FLOAT);

		Value<Float> value = valueFactory.newValue();

		assertEquals(0f, value.getValue());

		HasReport hasReport = (HasReport)value;

		Report report = hasReport.getReport();

		assertFalse(report.hasEntries());

		assertThrows(IllegalStateException.class, () -> report.tailEntry());

		value.add(Numbers.FLOAT_ZERO);

		assertEquals(0f, value.getValue());

		assertFalse(report.hasEntries());

		value.add(Numbers.FLOAT_ONE);

		assertEquals(1f, value.getValue());

		assertTrue(report.hasEntries());

		Report.Entry entry = report.tailEntry();

		assertEquals("<cn>" + 1f + "</cn>", entry.getExpression());
		assertEquals(1f, entry.getValue());

		value.reset(Numbers.FLOAT_ZERO);

		assertEquals(0f, value.getValue());

		assertTrue(report.hasEntries());

		entry = report.tailEntry();

		assertEquals("<cn>" + 0f + "</cn>", entry.getExpression());
		assertEquals(0f, entry.getValue());
	}

	@Test
	public void newVocalZero(){
		ValueFactory<Float> valueFactory = (ValueFactory)this.valueFactoryFactory.newValueFactory(MathContext.FLOAT);

		Value<Float> value = valueFactory.newValue(0d);

		assertEquals(0f, value.getValue());

		HasReport hasReport = (HasReport)value;

		Report report = hasReport.getReport();

		assertTrue(report.hasEntries());

		Report.Entry entry = report.tailEntry();

		assertEquals("<cn>" + 0f + "</cn>", entry.getExpression());
		assertEquals(0f, entry.getValue());
	}
}