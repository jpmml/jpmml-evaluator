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

import java.util.List;

import org.dmg.pmml.InlineTable;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Row;
import org.dmg.pmml.Visitor;
import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.nearest_neighbor.TrainingInstances;
import org.jpmml.evaluator.PMMLTransformer;
import org.jpmml.model.visitors.AbstractVisitor;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RemoveTrainingInstanceTransformer implements PMMLTransformer<RuntimeException> {

	private int index;


	public RemoveTrainingInstanceTransformer(int index){
		setIndex(index);
	}

	@Override
	public PMML apply(PMML pmml){
		Visitor visitor = new AbstractVisitor(){

			@Override
			public VisitorAction visit(TrainingInstances trainingInstances){
				InlineTable inlineTable = trainingInstances.getInlineTable();

				List<Row> rows = inlineTable.getRows();

				// XXX
				assertEquals(150, rows.size());

				rows.remove(getIndex());

				trainingInstances.setRecordCount(rows.size());

				return super.visit(trainingInstances);
			}
		};

		visitor.applyTo(pmml);

		return pmml;
	}

	public int getIndex(){
		return this.index;
	}

	private void setIndex(int index){
		this.index = index;
	}
}