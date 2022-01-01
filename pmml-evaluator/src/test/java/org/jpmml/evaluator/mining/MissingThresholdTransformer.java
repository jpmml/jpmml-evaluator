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
package org.jpmml.evaluator.mining;

import org.dmg.pmml.PMML;
import org.dmg.pmml.Visitor;
import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.mining.Segmentation;
import org.jpmml.evaluator.PMMLTransformer;
import org.jpmml.model.visitors.AbstractVisitor;

public class MissingThresholdTransformer implements PMMLTransformer<RuntimeException> {

	private double missingThreshold;


	public MissingThresholdTransformer(double missingThreshold){
		setMissingThreshold(missingThreshold);
	}

	@Override
	public PMML apply(PMML pmml){
		Visitor visitor = new AbstractVisitor(){

			@Override
			public VisitorAction visit(Segmentation segmentation){
				segmentation.setMissingThreshold(getMissingThreshold());

				return super.visit(segmentation);
			}
		};

		visitor.applyTo(pmml);

		return pmml;
	}

	public double getMissingThreshold(){
		return this.missingThreshold;
	}

	private void setMissingThreshold(double missingThreshold){
		this.missingThreshold = missingThreshold;
	}
}