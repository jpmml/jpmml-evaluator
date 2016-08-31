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

import java.util.Iterator;

import org.dmg.pmml.ContinuousDistribution;
import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.naive_bayes.TargetValueStat;
import org.dmg.pmml.naive_bayes.TargetValueStats;
import org.jpmml.evaluator.DistributionUtil;
import org.jpmml.model.visitors.AbstractVisitor;

public class TargetValueStatCleaner extends AbstractVisitor {

	@Override
	public VisitorAction visit(TargetValueStats targetValueStats){

		for(Iterator<TargetValueStat> it = targetValueStats.iterator(); it.hasNext(); ){
			TargetValueStat targetValueStat = it.next();

			ContinuousDistribution distribution = targetValueStat.getContinuousDistribution();
			if(DistributionUtil.isNoOp(distribution)){
				it.remove();
			}
		}

		return super.visit(targetValueStats);
	}
}