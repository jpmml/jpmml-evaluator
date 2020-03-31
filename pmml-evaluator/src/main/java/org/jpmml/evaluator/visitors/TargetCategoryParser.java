/*
 * Copyright (c) 2020 Villu Ruusmann
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Iterables;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Model;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.PMMLAttributes;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.ResultFeature;
import org.dmg.pmml.ScoreDistribution;
import org.dmg.pmml.Target;
import org.dmg.pmml.TargetValue;
import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.general_regression.GeneralRegressionModel;
import org.dmg.pmml.general_regression.PCell;
import org.dmg.pmml.general_regression.PCovCell;
import org.dmg.pmml.general_regression.PPCell;
import org.dmg.pmml.naive_bayes.TargetValueCount;
import org.dmg.pmml.naive_bayes.TargetValueStat;
import org.dmg.pmml.regression.RegressionTable;
import org.dmg.pmml.rule_set.RuleSet;
import org.dmg.pmml.rule_set.SimpleRule;
import org.dmg.pmml.support_vector_machine.SupportVectorMachine;
import org.dmg.pmml.support_vector_machine.SupportVectorMachineModel;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.evaluator.MissingAttributeException;
import org.jpmml.evaluator.TypeUtil;

public class TargetCategoryParser extends AbstractParser {

	private Set<FieldName> targetNames = new HashSet<>();


	@Override
	public void reset(){
		super.reset();

		this.targetNames.clear();
	}

	@Override
	public void pushParent(PMMLObject parent){
		super.pushParent(parent);

		if(parent instanceof Model){
			Model model = (Model)parent;

			processModel(model);
		}
	}

	@Override
	public PMMLObject popParent(){
		PMMLObject parent = super.popParent();

		if(parent instanceof Model){
			this.targetNames.clear();
		}

		return parent;
	}

	@Override
	public VisitorAction visit(GeneralRegressionModel generalRegressionModel){
		generalRegressionModel.setTargetReferenceCategory(parseTargetValue(generalRegressionModel.getTargetReferenceCategory()));

		return super.visit(generalRegressionModel);
	}

	@Override
	public VisitorAction visit(Node node){
		PMMLObject parent = getParent();

		if(parent instanceof TreeModel){
			TreeModel treeModel = (TreeModel)parent;

			MiningFunction miningFunction = treeModel.getMiningFunction();
			switch(miningFunction){
				case CLASSIFICATION:
					break;
				default:
					return VisitorAction.SKIP;
			}
		}

		node.setScore(parseTargetValue(node.getScore()));

		return super.visit(node);
	}

	@Override
	public VisitorAction visit(OutputField outputField){
		ResultFeature resultFeature = outputField.getResultFeature();

		switch(resultFeature){
			case PROBABILITY:
			case CONFIDENCE:
			case AFFINITY:
				{
					Object value = outputField.getValue();

					FieldName targetName = outputField.getTargetField();
					if(targetName != null){
						outputField.setValue(parseTargetValue(targetName, value));
					} else

					{
						outputField.setValue(parseTargetValue(value));
					}
				}
				break;
			default:
				break;
		}

		return super.visit(outputField);
	}

	@Override
	public VisitorAction visit(PCell pCell){
		pCell.setTargetCategory(parseTargetValue(pCell.getTargetCategory()));

		return super.visit(pCell);
	}

	@Override
	public VisitorAction visit(PCovCell pCovCell){
		pCovCell.setTargetCategory(parseTargetValue(pCovCell.getTargetCategory()));

		return super.visit(pCovCell);
	}

	@Override
	public VisitorAction visit(PPCell ppCell){
		ppCell.setTargetCategory(parseTargetValue(ppCell.getTargetCategory()));

		return super.visit(ppCell);
	}

	@Override
	public VisitorAction visit(RegressionTable regressionTable){
		regressionTable.setTargetCategory(parseTargetValue(regressionTable.getTargetCategory()));

		return super.visit(regressionTable);
	}

	@Override
	public VisitorAction visit(RuleSet ruleSet){
		ruleSet.setDefaultScore(parseTargetValue(ruleSet.getDefaultScore()));

		return super.visit(ruleSet);
	}

	@Override
	public VisitorAction visit(ScoreDistribution scoreDistribution){
		Object value = scoreDistribution.getValue();
		if(value == null){
			throw new MissingAttributeException(scoreDistribution, PMMLAttributes.SCOREDISTRIBUTION_VALUE);
		}

		scoreDistribution.setValue(parseTargetValue(value));

		return super.visit(scoreDistribution);
	}

	@Override
	public VisitorAction visit(SimpleRule simpleRule){
		Object score = simpleRule.getScore();
		if(score == null){
			throw new MissingAttributeException(simpleRule, org.dmg.pmml.rule_set.PMMLAttributes.SIMPLERULE_SCORE);
		}

		simpleRule.setScore(parseTargetValue(simpleRule.getScore()));

		return super.visit(simpleRule);
	}

	@Override
	public VisitorAction visit(SupportVectorMachine supportVectorMachine){
		supportVectorMachine.setTargetCategory(parseTargetValue(supportVectorMachine.getTargetCategory()));
		supportVectorMachine.setAlternateTargetCategory(parseTargetValue(supportVectorMachine.getAlternateTargetCategory()));

		return super.visit(supportVectorMachine);
	}

	@Override
	public VisitorAction visit(SupportVectorMachineModel supportVectorMachineModel){
		supportVectorMachineModel.setAlternateBinaryTargetCategory(parseTargetValue(supportVectorMachineModel.getAlternateBinaryTargetCategory()));

		return super.visit(supportVectorMachineModel);
	}

	@Override
	public VisitorAction visit(TargetValue targetValue){
		Target target = (Target)getParent();

		FieldName targetName = target.getField();
		if(targetName != null){
			targetValue.setValue(parseTargetValue(targetName, targetValue.getValue()));
		}

		return super.visit(targetValue);
	}

	@Override
	public VisitorAction visit(TargetValueCount targetValueCount){
		Object value = targetValueCount.getValue();
		if(value == null){
			throw new MissingAttributeException(targetValueCount, org.dmg.pmml.naive_bayes.PMMLAttributes.TARGETVALUECOUNT_VALUE);
		}

		targetValueCount.setValue(parseTargetValue(value));

		return super.visit(targetValueCount);
	}

	@Override
	public VisitorAction visit(TargetValueStat targetValueStat){
		Object value = targetValueStat.getValue();
		if(value == null){
			throw new MissingAttributeException(targetValueStat, org.dmg.pmml.naive_bayes.PMMLAttributes.TARGETVALUESTAT_VALUE);
		}

		targetValueStat.setValue(parseTargetValue(value));

		return super.visit(targetValueStat);
	}

	private void processModel(Model model){
		MiningSchema miningSchema = model.getMiningSchema();

		this.targetNames.clear();

		if(miningSchema != null && miningSchema.hasMiningFields()){
			List<MiningField> miningFields = miningSchema.getMiningFields();

			for(MiningField miningField : miningFields){
				FieldName name = miningField.getName();
				if(name == null){
					throw new MissingAttributeException(miningField, PMMLAttributes.MININGFIELD_NAME);
				}

				MiningField.UsageType usageType = miningField.getUsageType();
				switch(usageType){
					case PREDICTED:
					case TARGET:
						this.targetNames.add(name);
						break;
					default:
						break;
				}
			}
		}
	}

	private Object parseTargetValue(Object value){

		if(value == null || this.targetNames.size() != 1){
			return value;
		}

		FieldName targetName = Iterables.getOnlyElement(this.targetNames);

		return parseTargetValue(targetName, value);
	}

	private Object parseTargetValue(FieldName targetName, Object value){

		if(value == null || targetName == null){
			return value;
		}

		DataType dataType = resolveDataType(targetName);
		if(dataType != null){
			return TypeUtil.parseOrCast(dataType, value);
		}

		return value;
	}
}