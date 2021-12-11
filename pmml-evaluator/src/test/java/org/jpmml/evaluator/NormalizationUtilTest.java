/*
 * Copyright (c) 2014 Villu Ruusmann
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

import org.dmg.pmml.LinearNorm;
import org.dmg.pmml.NormContinuous;
import org.dmg.pmml.OutlierTreatmentMethod;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NormalizationUtilTest implements Deltas {

	@Test
	public void normalize(){
		NormContinuous normContinuous = createNormContinuous();

		assertEquals(BEGIN[1], (double)NormalizationUtil.normalize(normContinuous, BEGIN[0]), DOUBLE_EXACT);
		assertEquals(interpolate(1.212d, BEGIN, MIDPOINT), (double)NormalizationUtil.normalize(normContinuous, 1.212d), DOUBLE_EXACT);
		assertEquals(MIDPOINT[1], (double)NormalizationUtil.normalize(normContinuous, MIDPOINT[0]), DOUBLE_EXACT);
		assertEquals(interpolate(6.5d, MIDPOINT, END), (double)NormalizationUtil.normalize(normContinuous, 6.5d), DOUBLE_EXACT);
		assertEquals(END[1], (double)NormalizationUtil.normalize(normContinuous, END[0]), DOUBLE_EXACT);
	}

	@Test
	public void normalizeOutliers(){
		NormContinuous normContinuous = createNormContinuous();

		assertEquals(interpolate(-1d, BEGIN, MIDPOINT), (double)NormalizationUtil.normalize(normContinuous, -1d), DOUBLE_EXACT);
		assertEquals(interpolate(12.2d, MIDPOINT, END), (double)NormalizationUtil.normalize(normContinuous, 12.2d), DOUBLE_EXACT);

		normContinuous.setOutliers(OutlierTreatmentMethod.AS_MISSING_VALUES);

		assertNull(NormalizationUtil.normalize(normContinuous, -1d));
		assertNull(NormalizationUtil.normalize(normContinuous, 12.2d));

		normContinuous.setOutliers(OutlierTreatmentMethod.AS_EXTREME_VALUES);

		assertEquals(BEGIN[1], (double)NormalizationUtil.normalize(normContinuous, -1d), DOUBLE_EXACT);
		assertEquals(END[1], (double)NormalizationUtil.normalize(normContinuous, 12.2d), DOUBLE_EXACT);
	}

	@Test
	public void denormalize(){
		NormContinuous normContinuous = createNormContinuous();

		assertEquals(BEGIN[0], (double)NormalizationUtil.denormalize(normContinuous, BEGIN[1]), DOUBLE_EXACT);
		assertEquals(0.3d, (double)NormalizationUtil.denormalize(normContinuous, interpolate(0.3d, BEGIN, MIDPOINT)), DOUBLE_EXACT);
		assertEquals(MIDPOINT[0], (double)NormalizationUtil.denormalize(normContinuous, MIDPOINT[1]), DOUBLE_EXACT);
		assertEquals(7.123d, (double)NormalizationUtil.denormalize(normContinuous, interpolate(7.123d, MIDPOINT, END)), DOUBLE_EXACT);
		assertEquals(END[0], (double)NormalizationUtil.denormalize(normContinuous, END[1]), DOUBLE_EXACT);
	}

	static
	private double interpolate(double x, double[] begin, double[] end){
		return begin[1] + (x - begin[0]) / (end[0] - begin[0]) * (end[1] - begin[1]);
	}

	static
	private NormContinuous createNormContinuous(){
		NormContinuous result = new NormContinuous("x", null)
			.addLinearNorms(
				new LinearNorm(BEGIN[0], BEGIN[1]),
				new LinearNorm(MIDPOINT[0], MIDPOINT[1]),
				new LinearNorm(END[0], END[1])
			);

		return result;
	}

	private static final double[] BEGIN = {0.01d, 0d};
	private static final double[] MIDPOINT = {3.07897d, 0.5d};
	private static final double[] END = {11.44d, 1.0d};
}