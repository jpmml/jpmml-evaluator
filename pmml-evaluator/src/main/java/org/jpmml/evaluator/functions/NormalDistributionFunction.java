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
package org.jpmml.evaluator.functions;

import java.util.Arrays;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.InvalidArgumentException;
import org.jpmml.evaluator.TypeInfos;

abstract
public class NormalDistributionFunction extends TernaryFunction {

	public NormalDistributionFunction(String name){
		super(name, Arrays.asList("x", "mu", "sigma"));
	}

	abstract
	public Double evaluate(Number x, NormalDistribution distribution);

	@Override
	public FieldValue evaluate(FieldValue first, FieldValue second, FieldValue third){
		Number x = first.asNumber();
		Number mu = second.asNumber();
		Number sigma = third.asNumber();

		if(sigma.doubleValue() <= 0d){
			throw new InvalidArgumentException(getName(), 1, InvalidArgumentException.formatMessage(getName(), "sigma", sigma) + ". Must be greater than 0");
		}

		NormalDistribution distribution = new NormalDistribution(mu.doubleValue(), sigma.doubleValue());

		Double result = evaluate(x, distribution);

		return FieldValueUtil.create(TypeInfos.CONTINUOUS_DOUBLE, result);
	}
}