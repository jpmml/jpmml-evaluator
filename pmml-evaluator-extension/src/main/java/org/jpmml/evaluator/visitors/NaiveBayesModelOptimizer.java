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
package org.jpmml.evaluator.visitors;

import java.util.List;
import java.util.ListIterator;

import org.dmg.pmml.BayesInput;
import org.dmg.pmml.BayesInputs;
import org.dmg.pmml.VisitorAction;
import org.jpmml.evaluator.RichBayesInput;
import org.jpmml.model.visitors.AbstractVisitor;

public class NaiveBayesModelOptimizer extends AbstractVisitor {

	@Override
	public VisitorAction visit(BayesInputs bayesInputs){

		if(bayesInputs.hasBayesInputs()){
			List<BayesInput> content = bayesInputs.getBayesInputs();

			for(ListIterator<BayesInput> it = content.listIterator(); it.hasNext(); ){
				it.set(new RichBayesInput(it.next()));
			}
		}

		return super.visit(bayesInputs);
	}
}