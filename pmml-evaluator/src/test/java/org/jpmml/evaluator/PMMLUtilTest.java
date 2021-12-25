/*
 * Copyright (c) 2018 Villu Ruusmann
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

import org.dmg.pmml.PMML;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.model.MissingElementException;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class PMMLUtilTest {

	@Test
	public void findModel(){
		PMML pmml = new PMML();

		try {
			PMMLUtil.findModel(pmml, (String)null);

			fail();
		} catch(MissingElementException mee){
			// Ignored
		}

		TreeModel firstTreeModel = new TreeModel()
			.setModelName("first");

		TreeModel secondTreeModel = new TreeModel()
			.setModelName("second");

		pmml.addModels(firstTreeModel, secondTreeModel);

		assertSame(firstTreeModel, PMMLUtil.findModel(pmml, (String)null));
		assertSame(firstTreeModel, PMMLUtil.findModel(pmml, "first"));
		assertSame(firstTreeModel, PMMLUtil.findModel(pmml, TreeModel.class));

		firstTreeModel.setScorable(false);

		assertSame(secondTreeModel, PMMLUtil.findModel(pmml, (String)null));
		assertSame(secondTreeModel, PMMLUtil.findModel(pmml, "second"));
		assertSame(secondTreeModel, PMMLUtil.findModel(pmml, TreeModel.class));

		secondTreeModel.setScorable(false);

		try {
			PMMLUtil.findModel(pmml, (String)null);

			fail();
		} catch(MissingElementException mee){
			// Ignored
		} // End try

		try {
			PMMLUtil.findModel(pmml, TreeModel.class);

			fail();
		} catch(MissingElementException mee){
			// Ignored
		}
	}
}