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

import org.dmg.pmml.FieldName;
import org.dmg.pmml.LinearNorm;
import org.dmg.pmml.NormContinuous;
import org.dmg.pmml.OutlierTreatmentMethod;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NormalizationUtilTest {

	@Test
	public void normalize(){
		NormContinuous normContinuous = createNormContinuous();

		assertEquals(BEGIN[1], NormalizationUtil.normalize(normContinuous, BEGIN[0]), 1e-8);
		assertEquals(interpolate(1.212d, BEGIN, MIDPOINT), NormalizationUtil.normalize(normContinuous, 1.212d), 1e-8);
		assertEquals(MIDPOINT[1], NormalizationUtil.normalize(normContinuous, MIDPOINT[0]), 1e-8);
		assertEquals(interpolate(6.5d, MIDPOINT, END), NormalizationUtil.normalize(normContinuous, 6.5d), 1e-8);
		assertEquals(END[1], NormalizationUtil.normalize(normContinuous, END[0]), 1e-8);
	}

	@Test
	public void normalizeOutliers(){
		NormContinuous normContinuous = createNormContinuous();

		assertEquals(interpolate(-1d, BEGIN, MIDPOINT), NormalizationUtil.normalize(normContinuous, -1d), 1e-8);
		assertEquals(interpolate(12.2d, MIDPOINT, END), NormalizationUtil.normalize(normContinuous, 12.2d), 1e-8);

		normContinuous.setOutliers(OutlierTreatmentMethod.AS_MISSING_VALUES)
			.setMapMissingTo(0.5d);

		assertEquals(0.5d, NormalizationUtil.normalize(normContinuous, -1d), 1e-8);
		assertEquals(0.5d, NormalizationUtil.normalize(normContinuous, 12.2d), 1e-8);

		normContinuous.setOutliers(OutlierTreatmentMethod.AS_EXTREME_VALUES);

		assertEquals(BEGIN[1], NormalizationUtil.normalize(normContinuous, -1d), 1e-8);
		assertEquals(END[1], NormalizationUtil.normalize(normContinuous, 12.2d), 1e-8);
	}

	@Test
	public void denormalize(){
		NormContinuous normContinuous = createNormContinuous();

		assertEquals(BEGIN[0], NormalizationUtil.denormalize(normContinuous, BEGIN[1]), 1e-8);
		assertEquals(0.3d, NormalizationUtil.denormalize(normContinuous, interpolate(0.3d, BEGIN, MIDPOINT)), 1e-8);
		assertEquals(MIDPOINT[0], NormalizationUtil.denormalize(normContinuous, MIDPOINT[1]), 1e-8);
		assertEquals(7.123d, NormalizationUtil.denormalize(normContinuous, interpolate(7.123d, MIDPOINT, END)), 1e-8);
		assertEquals(END[0], NormalizationUtil.denormalize(normContinuous, END[1]), 1e-8);
	}

	static
	private double interpolate(double x, double[] begin, double[] end){
		return begin[1] + (x - begin[0]) / (end[0] - begin[0]) * (end[1] - begin[1]);
	}

	static
	private NormContinuous createNormContinuous(){
		NormContinuous result = new NormContinuous(FieldName.create("x"), null)
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