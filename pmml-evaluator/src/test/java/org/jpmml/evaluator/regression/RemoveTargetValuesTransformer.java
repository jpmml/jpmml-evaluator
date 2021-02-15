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
package org.jpmml.evaluator.regression;

import java.util.List;

import org.dmg.pmml.PMML;
import org.dmg.pmml.Target;
import org.dmg.pmml.TargetValue;
import org.dmg.pmml.Visitor;
import org.dmg.pmml.VisitorAction;
import org.jpmml.evaluator.PMMLTransformer;
import org.jpmml.model.visitors.AbstractVisitor;

public class RemoveTargetValuesTransformer implements PMMLTransformer<Exception> {

	@Override
	public PMML apply(PMML pmml){
		Visitor visitor = new AbstractVisitor(){

			@Override
			public VisitorAction visit(Target target){

				if(target.hasTargetValues()){
					List<TargetValue> targetValues = target.getTargetValues();

					targetValues.clear();
				}

				return super.visit(target);
			}
		};

		visitor.applyTo(pmml);

		return pmml;
	}
}