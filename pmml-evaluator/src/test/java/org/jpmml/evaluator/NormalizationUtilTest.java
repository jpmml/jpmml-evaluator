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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NormalizationUtilTest implements Deltas {

	@Test
	public void normalize(){
		NormContinuous normContinuous = createNormContinuous();

		assertEquals(BEGIN[1], normalize(normContinuous, BEGIN[0]), DOUBLE_EXACT);
		assertEquals(interpolate(1.212d, BEGIN, MIDPOINT), normalize(normContinuous, 1.212d), DOUBLE_EXACT);
		assertEquals(MIDPOINT[1], normalize(normContinuous, MIDPOINT[0]), DOUBLE_EXACT);
		assertEquals(interpolate(6.5d, MIDPOINT, END), normalize(normContinuous, 6.5d), DOUBLE_EXACT);
		assertEquals(END[1], normalize(normContinuous, END[0]), DOUBLE_EXACT);
	}

	@Test
	public void normalizeOutliers(){
		NormContinuous normContinuous = createNormContinuous();

		assertEquals(interpolate(-1d, BEGIN, MIDPOINT), normalize(normContinuous, -1d), DOUBLE_EXACT);
		assertEquals(interpolate(12.2d, MIDPOINT, END), normalize(normContinuous, 12.2d), DOUBLE_EXACT);

		normContinuous.setOutliers(OutlierTreatmentMethod.AS_MISSING_VALUES);

		assertNull(normalize(normContinuous, -1d));
		assertNull(normalize(normContinuous, 12.2d));

		normContinuous.setOutliers(OutlierTreatmentMethod.AS_EXTREME_VALUES);

		assertEquals(BEGIN[1], normalize(normContinuous, -1d), DOUBLE_EXACT);
		assertEquals(END[1], normalize(normContinuous, 12.2d), DOUBLE_EXACT);
	}

	@Test
	public void denormalize(){
		NormContinuous normContinuous = createNormContinuous();

		assertThrows(NotImplementedException.class, () -> denormalize(normContinuous, -0.5d));

		assertEquals(BEGIN[0], denormalize(normContinuous, BEGIN[1]), DOUBLE_EXACT);
		assertEquals(0.3d, denormalize(normContinuous, interpolate(0.3d, BEGIN, MIDPOINT)), DOUBLE_EXACT);
		assertEquals(MIDPOINT[0], denormalize(normContinuous, MIDPOINT[1]), DOUBLE_EXACT);
		assertEquals(7.123d, denormalize(normContinuous, interpolate(7.123d, MIDPOINT, END)), DOUBLE_EXACT);
		assertEquals(END[0], denormalize(normContinuous, END[1]), DOUBLE_EXACT);

		assertThrows(NotImplementedException.class, () -> denormalize(normContinuous, 1.5d));
	}

	@Test
	public void standardize(){
		double mu = 1.5;
		double stdev = Math.sqrt(2d);

		NormContinuous normContinuous = new NormContinuous("x", null)
			.setOutliers(OutlierTreatmentMethod.AS_IS)
			.addLinearNorms(
				new LinearNorm(0d, -(mu / stdev)),
				new LinearNorm(mu, 0d)
			);

		assertEquals(zScore(-2d, mu, stdev), normalize(normContinuous, -2d), DOUBLE_EXACT);
		assertEquals(zScore(-1d, mu, stdev), normalize(normContinuous, -1d), DOUBLE_EXACT);
		assertEquals(zScore(0d, mu, stdev), normalize(normContinuous, 0d), DOUBLE_EXACT);
		assertEquals(zScore(1d, mu, stdev), normalize(normContinuous, 1d), DOUBLE_EXACT);
		assertEquals(zScore(2d, mu, stdev), normalize(normContinuous, 2d), DOUBLE_EXACT);

		assertEquals(1d, denormalize(normContinuous, zScore(1d, mu, stdev)), DOUBLE_EXACT);
	}

	static
	private Double normalize(NormContinuous normContinuous, double value){
		return (Double)NormalizationUtil.normalize(normContinuous, value);
	}

	static
	private Double denormalize(NormContinuous normContinuous, double value){
		return (Double)NormalizationUtil.denormalize(normContinuous, value);
	}

	static
	private double interpolate(double x, double[] begin, double[] end){
		return begin[1] + (x - begin[0]) / (end[0] - begin[0]) * (end[1] - begin[1]);
	}

	static
	private double zScore(double x, double mu, double stdev){
		return (x - mu) / stdev;
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