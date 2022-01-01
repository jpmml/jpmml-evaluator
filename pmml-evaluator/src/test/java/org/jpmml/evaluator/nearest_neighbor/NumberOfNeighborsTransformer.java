/*
 * Copyright (c) 2021 Villu Ruusmann
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
package org.jpmml.evaluator.nearest_neighbor;

import org.dmg.pmml.PMML;
import org.dmg.pmml.Visitor;
import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.nearest_neighbor.NearestNeighborModel;
import org.jpmml.evaluator.PMMLTransformer;
import org.jpmml.model.visitors.AbstractVisitor;

public class NumberOfNeighborsTransformer implements PMMLTransformer<RuntimeException> {

	private int numberOfNeighbors;


	public NumberOfNeighborsTransformer(int numberOfNeighbors){
		setNumberOfNeighbors(numberOfNeighbors);
	}

	@Override
	public PMML apply(PMML pmml){
		Visitor visitor = new AbstractVisitor(){

			@Override
			public VisitorAction visit(NearestNeighborModel nearestNeighborModel){
				nearestNeighborModel.setNumberOfNeighbors(getNumberOfNeighbors());

				return super.visit(nearestNeighborModel);
			}
		};

		visitor.applyTo(pmml);

		return pmml;
	}

	public int getNumberOfNeighbors(){
		return this.numberOfNeighbors;
	}

	private void setNumberOfNeighbors(int numberOfNeighbors){
		this.numberOfNeighbors = numberOfNeighbors;
	}
}