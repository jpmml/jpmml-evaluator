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

import java.util.Deque;
import java.util.List;
import java.util.Objects;

import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.Target;
import org.dmg.pmml.Targets;
import org.dmg.pmml.VisitorAction;
import org.jpmml.evaluator.IndexableUtil;
import org.jpmml.evaluator.MissingFieldException;
import org.jpmml.evaluator.UnsupportedFeatureException;
import org.jpmml.model.visitors.AbstractModelVisitor;

public class RegressionTargetCorrector extends AbstractModelVisitor {

	private Target.CastInteger castInteger = null;


	public RegressionTargetCorrector(){
		this(Target.CastInteger.ROUND);
	}

	public RegressionTargetCorrector(Target.CastInteger castInteger){
		setCastInteger(Objects.requireNonNull(castInteger));
	}

	@Override
	public VisitorAction visit(Model model){
		MiningFunction miningFunction = model.getMiningFunction();

		switch(miningFunction){
			case REGRESSION:
				processRegressionModel(model);
				break;
			default:
				break;
		}

		return VisitorAction.CONTINUE;
	}

	private void processRegressionModel(Model model){
		PMML pmml = getPMML();

		MiningField miningField = getTargetField(model);
		if(miningField == null){
			return;
		}

		FieldName name = miningField.getName();

		DataDictionary dataDictionary = pmml.getDataDictionary();

		DataField dataField = IndexableUtil.find(name, dataDictionary.getDataFields());
		if(dataField == null){
			throw new MissingFieldException(name, miningField);
		}

		DataType dataType = dataField.getDataType();
		switch(dataType){
			case INTEGER:
				break;
			case FLOAT:
			case DOUBLE:
				return;
			default:
				throw new UnsupportedFeatureException(dataField, dataType);
		}

		Targets targets = model.getTargets();

		if(targets != null){
			Target target = IndexableUtil.find(name, targets.getTargets());

			if(target != null){

				if(target.getCastInteger() != null){
					return;
				} else

				{
					target.setCastInteger(getCastInteger());
				}
			} else

			{
				targets.addTargets(createTarget(name));
			}
		} else

		{
			targets = new Targets()
				.addTargets(createTarget(name));

			model.setTargets(targets);
		}
	}

	private Target createTarget(FieldName name){
		Target target = new Target()
			.setField(name)
			.setCastInteger(getCastInteger());

		return target;
	}

	private PMML getPMML(){
		Deque<PMMLObject> parents = getParents();

		return (PMML)parents.getLast();
	}

	public Target.CastInteger getCastInteger(){
		return this.castInteger;
	}

	private void setCastInteger(Target.CastInteger castInteger){
		this.castInteger = castInteger;
	}

	static
	private MiningField getTargetField(Model model){
		MiningSchema miningSchema = model.getMiningSchema();

		MiningField result = null;

		List<MiningField> miningFields = miningSchema.getMiningFields();
		for(MiningField miningField : miningFields){
			MiningField.UsageType usageType = miningField.getUsageType();

			switch(usageType){
				case TARGET:
				case PREDICTED:
					if(result != null){
						throw new UnsupportedFeatureException(miningSchema);
					}
					result = miningField;
					break;
				default:
					break;
			}
		}

		return result;
	}
}