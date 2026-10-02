/*
 * Copyright (c) 2026 Villu Ruusmann
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

import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.Header;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Version;

abstract
public class ParserTest {

	static
	public DataDictionary createDataDictionary(DataField... dataFields){
		DataDictionary dataDictionary = new DataDictionary()
			.addDataFields(dataFields);

		return dataDictionary;
	}

	static
	public MiningSchema createMiningSchema(List<DataField> activeFields, List<DataField> targetFields){
		MiningSchema miningSchema = new MiningSchema();

		for(DataField activeField : activeFields){
			MiningField miningField = new MiningField(activeField);

			miningSchema.addMiningFields(miningField);
		}

		for(DataField targetField : targetFields){
			MiningField miningField = new MiningField(targetField)
				.setUsageType(MiningField.UsageType.TARGET);

			miningSchema.addMiningFields(miningField);
		}

		return miningSchema;
	}

	static
	public PMML createPMML(DataDictionary dataDictionary, Model model){
		PMML pmml = new PMML(Version.PMML_4_4.getVersion(), new Header(), dataDictionary)
			.addModels(model);

		return pmml;
	}
}