/*
 * Copyright (c) 2020 Villu Ruusmann
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

import org.dmg.pmml.MathContext;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.tree.DecisionTree;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.model.visitors.AbstractVisitor;
import org.jpmml.model.visitors.Resettable;

/**
 * <p>
 * A Visitor that pre-parses the score attribute of regression-type tree models.
 * </p>
 */
public class NodeScoreOptimizer extends AbstractVisitor implements Resettable {

	private MathContext mathContext = null;


	@Override
	public void reset(){
		this.mathContext = null;
	}

	@Override
	public VisitorAction visit(DecisionTree decisionTree){
		throw new UnsupportedOperationException();
	}

	@Override
	public VisitorAction visit(TreeModel treeModel){
		MiningFunction miningFunction = treeModel.getMiningFunction();

		if(miningFunction != null){

			switch(miningFunction){
				case REGRESSION:
					this.mathContext = treeModel.getMathContext();
					break;
				default:
					this.mathContext = null;
					break;
			}
		} // End if

		if(this.mathContext == null){
			return VisitorAction.SKIP;
		}

		return super.visit(treeModel);
	}

	@Override
	public VisitorAction visit(Node node){
		MathContext mathContext = this.mathContext;

		if(mathContext != null && node.hasScore()){
			Object score = node.getScore();

			if(score instanceof String){
				String stringScore = (String)score;

				try {
					switch(mathContext){
						case DOUBLE:
							node.setScore(Double.parseDouble(stringScore));
							break;
						case FLOAT:
							node.setScore(Float.parseFloat(stringScore));
							break;
						default:
							break;
					}
				} catch(NumberFormatException nfe){
					// Ignored
				}
			}
		}

		return super.visit(node);
	}
}