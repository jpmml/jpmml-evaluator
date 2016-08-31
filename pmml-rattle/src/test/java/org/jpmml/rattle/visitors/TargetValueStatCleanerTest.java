/*
 * Copyright (c) 2016 Villu Ruusmann
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
package org.jpmml.rattle.visitors;

import java.util.Arrays;

import org.dmg.pmml.ContinuousDistribution;
import org.dmg.pmml.GaussianDistribution;
import org.dmg.pmml.naive_bayes.BayesInput;
import org.dmg.pmml.naive_bayes.TargetValueStat;
import org.dmg.pmml.naive_bayes.TargetValueStats;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TargetValueStatCleanerTest {

	@Test
	public void clean(){
		TargetValueStat zero = createTargetValueStat("0", new GaussianDistribution(0d, 1d));
		TargetValueStat one = createTargetValueStat("1", new GaussianDistribution());

		TargetValueStats targetValueStats = new TargetValueStats()
			.addTargetValueStats(zero, one);

		BayesInput bayesInput = new BayesInput()
			.setTargetValueStats(targetValueStats);

		assertEquals(Arrays.asList(zero, one), targetValueStats.getTargetValueStats());

		TargetValueStatCleaner cleaner = new TargetValueStatCleaner();
		cleaner.applyTo(bayesInput);

		assertEquals(Arrays.asList(zero), targetValueStats.getTargetValueStats());
	}

	static
	private TargetValueStat createTargetValueStat(String value, ContinuousDistribution distribution){
		TargetValueStat targetValueStat = new TargetValueStat(value)
			.setContinuousDistribution(distribution);

		return targetValueStat;
	}
}