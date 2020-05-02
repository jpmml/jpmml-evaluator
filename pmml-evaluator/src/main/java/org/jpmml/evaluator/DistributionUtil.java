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

import org.apache.commons.math3.distribution.NormalDistribution;
import org.dmg.pmml.ContinuousDistribution;
import org.dmg.pmml.DataType;
import org.dmg.pmml.GaussianDistribution;
import org.dmg.pmml.PMMLAttributes;
import org.dmg.pmml.PoissonDistribution;

public class DistributionUtil {

	private DistributionUtil(){
	}

	/**
	 * <p>
	 * Calculates the value of the specified probability function at the specified point.
	 * </p>
	 */
	static
	public double probability(ContinuousDistribution distribution, Number x){

		if(distribution instanceof GaussianDistribution){
			return probability((GaussianDistribution)distribution, x);
		} else

		if(distribution instanceof PoissonDistribution){
			return probability((PoissonDistribution)distribution, x);
		}

		throw new UnsupportedElementException(distribution);
	}

	static
	public double probability(GaussianDistribution gaussianDistribution, Number x){
		Number mean = gaussianDistribution.getMean();
		if(mean == null){
			throw new MissingAttributeException(gaussianDistribution, PMMLAttributes.GAUSSIANDISTRIBUTION_MEAN);
		}

		Number variance = gaussianDistribution.getVariance();
		if(variance == null){
			throw new MissingAttributeException(gaussianDistribution, PMMLAttributes.GAUSSIANDISTRIBUTION_VARIANCE);
		} // End if

		if(variance.doubleValue() <= 0d){
			throw new InvalidAttributeException(gaussianDistribution, PMMLAttributes.GAUSSIANDISTRIBUTION_VARIANCE, variance);
		}

		NormalDistribution distribution = new NormalDistribution(mean.doubleValue(), Math.sqrt(variance.doubleValue()));

		return distribution.density(x.doubleValue());
	}

	static
	public double probability(PoissonDistribution poissonDistribution, Number x){
		Number mean = poissonDistribution.getMean();
		if(mean == null){
			throw new MissingAttributeException(poissonDistribution, PMMLAttributes.POISSONDISTRIBUTION_MEAN);
		}

		org.apache.commons.math3.distribution.PoissonDistribution distribution = new org.apache.commons.math3.distribution.PoissonDistribution(null, mean.doubleValue(), org.apache.commons.math3.distribution.PoissonDistribution.DEFAULT_EPSILON, org.apache.commons.math3.distribution.PoissonDistribution.DEFAULT_MAX_ITERATIONS);

		x = (Number)TypeUtil.cast(DataType.INTEGER, x);

		return distribution.probability(x.intValue());
	}
}