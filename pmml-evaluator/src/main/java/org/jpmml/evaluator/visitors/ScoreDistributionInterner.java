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
package org.jpmml.evaluator.visitors;

import java.util.List;
import java.util.ListIterator;

import org.dmg.pmml.ScoreDistribution;
import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.tree.Node;
import org.jpmml.model.visitors.AbstractVisitor;

/**
 * <p>
 * A Visitor that interns {@link ScoreDistribution} elements.
 * </p>
 */
public class ScoreDistributionInterner extends AbstractVisitor {

	private ElementHashMap<ScoreDistribution> cache = new ElementHashMap<ScoreDistribution>(){

		@Override
		public ElementKey createKey(ScoreDistribution scoreDistribution){
			Object[] content = {scoreDistribution.getValue(), scoreDistribution.getRecordCount(), scoreDistribution.getProbability(), scoreDistribution.getConfidence()};

			return new ElementKey(content);
		}
	};


	@Override
	public VisitorAction visit(Node node){

		if(node.hasScoreDistributions()){
			List<ScoreDistribution> scoreDistributions = node.getScoreDistributions();

			for(ListIterator<ScoreDistribution> it = scoreDistributions.listIterator(); it.hasNext(); ){
				it.set(intern(it.next()));
			}
		}

		return super.visit(node);
	}

	private ScoreDistribution intern(ScoreDistribution scoreDistribution){

		if(scoreDistribution == null || scoreDistribution.hasExtensions()){
			return scoreDistribution;
		}

		return this.cache.intern(scoreDistribution);
	}
}