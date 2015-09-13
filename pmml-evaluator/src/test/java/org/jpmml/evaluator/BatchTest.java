/*
 * Copyright (c) 2015 Villu Ruusmann
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
import java.util.Set;

import org.dmg.pmml.FieldName;

import static org.junit.Assert.assertTrue;

abstract
public class BatchTest {

	public void evaluate(Batch batch) throws Exception {
		evaluate(batch, null);
	}

	public void evaluate(Batch batch, Set<FieldName> ignoredFields) throws Exception {
		evaluate(batch, ignoredFields, BatchTest.precision, BatchTest.zeroThreshold);
	}

	public void evaluate(Batch batch, Set<FieldName> ignoredFields, double precision, double zeroThreshold) throws Exception {
		List<Conflict> conflicts = BatchUtil.evaluate(batch, ignoredFields, precision, zeroThreshold);

		assertTrue("Found " + conflicts.size() + " conflict(s)", conflicts.isEmpty());
	}

	// One part per billion parts
	private static final double precision = 1d / (1000L * 1000L * 1000L);

	static {
		assertTrue(Double.compare(precision, 1e-9) <= 0);
	}

	private static final double zeroThreshold = precision;
}