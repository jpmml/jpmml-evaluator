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
package org.jpmml.knime.visitors;

import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Header;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Target;
import org.dmg.pmml.Targets;
import org.dmg.pmml.regression.RegressionModel;
import org.jpmml.evaluator.IndexableUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class RegressionTargetCorrectorTest {

	@Test
	public void correct(){
		FieldName name = FieldName.create("y");

		DataField dataField = new DataField(name, OpType.CONTINUOUS, DataType.DOUBLE);

		DataDictionary dataDictionary = new DataDictionary()
			.addDataFields(dataField);

		MiningField miningField = new MiningField(name)
			.setUsageType(MiningField.UsageType.PREDICTED);

		MiningSchema miningSchema = new MiningSchema()
			.addMiningFields(miningField);

		RegressionModel regressionModel = new RegressionModel(MiningFunction.REGRESSION, miningSchema, null);

		PMML pmml = new PMML("4.3", new Header(), dataDictionary)
			.addModels(regressionModel);

		RegressionTargetCorrector corrector = new RegressionTargetCorrector();
		corrector.applyTo(pmml);

		Targets targets = regressionModel.getTargets();

		assertNull(targets);

		dataField.setDataType(DataType.INTEGER);

		corrector.applyTo(pmml);

		targets = regressionModel.getTargets();

		assertNotNull(targets);

		Target target = IndexableUtil.find(name, targets.getTargets());

		assertNotNull(target);

		assertEquals(Target.CastInteger.ROUND, target.getCastInteger());

		corrector = new RegressionTargetCorrector(Target.CastInteger.FLOOR);
		corrector.applyTo(pmml);

		assertEquals(Target.CastInteger.ROUND, target.getCastInteger());

		target.setCastInteger(null);

		corrector.applyTo(pmml);

		assertEquals(Target.CastInteger.FLOOR, target.getCastInteger());
	}
}