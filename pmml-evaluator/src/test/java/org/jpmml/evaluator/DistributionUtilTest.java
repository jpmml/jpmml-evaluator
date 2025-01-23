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

import org.dmg.pmml.ContinuousDistribution;
import org.dmg.pmml.GaussianDistribution;
import org.dmg.pmml.PoissonDistribution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DistributionUtilTest implements Deltas {

	@Test
	public void estimateDensity(){
		ContinuousDistribution distribution = new GaussianDistribution(5d, Math.pow(1.5d, 2d));

		assertEquals(0.00759732d, DistributionUtil.probability(distribution, 1d), DOUBLE_INEXACT);
		assertEquals(0.10934005d, DistributionUtil.probability(distribution, 3d), DOUBLE_INEXACT);
		assertEquals(0.26596152d, DistributionUtil.probability(distribution, 5d), DOUBLE_INEXACT);
	}

	@Test
	public void estimateMass(){
		ContinuousDistribution distribution = new PoissonDistribution(5d);

		assertEquals(0.03368973d, DistributionUtil.probability(distribution, 1), DOUBLE_INEXACT);
		assertEquals(0.14037390d, DistributionUtil.probability(distribution, 3), DOUBLE_INEXACT);
		assertEquals(0.17546737d, DistributionUtil.probability(distribution, 5), DOUBLE_INEXACT);
		assertEquals(0.10444486d, DistributionUtil.probability(distribution, 7), DOUBLE_INEXACT);
		assertEquals(0.03626558d, DistributionUtil.probability(distribution, 9), DOUBLE_INEXACT);
	}
}