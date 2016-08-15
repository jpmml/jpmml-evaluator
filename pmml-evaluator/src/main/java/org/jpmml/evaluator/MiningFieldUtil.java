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
package org.jpmml.evaluator;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.InvalidValueTreatmentMethod;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.OpType;
import org.dmg.pmml.OutlierTreatmentMethod;

public class MiningFieldUtil {

	private MiningFieldUtil(){
	}

	static
	public boolean isDefault(MiningField miningField){
		FieldName name = miningField.getName();

		OpType opType = miningField.getOpType();
		if(opType != null){
			return false;
		}

		String missingValueReplacement = miningField.getMissingValueReplacement();
		if(missingValueReplacement != null){
			return false;
		}

		OutlierTreatmentMethod outlierTreatmentMethod = miningField.getOutlierTreatment();
		switch(outlierTreatmentMethod){
			case AS_IS:
				break;
			default:
				return false;
		}

		InvalidValueTreatmentMethod invalidValueTreatmentMethod = miningField.getInvalidValueTreatment();
		switch(invalidValueTreatmentMethod){
			case RETURN_INVALID:
				break;
			default:
				return false;
		}

		return true;
	}
}