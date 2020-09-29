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
package org.jpmml.evaluator.tree;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.dmg.pmml.ResultFeature;
import org.dmg.pmml.Targets;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.PMMLAttributes;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.MissingAttributeException;
import org.jpmml.evaluator.PMMLUtil;
import org.jpmml.evaluator.PredicateUtil;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TypeUtil;
import org.jpmml.evaluator.UnsupportedAttributeException;
import org.jpmml.evaluator.UnsupportedElementException;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.annotations.Functionality;

@Functionality (
	value = {
		ResultFeature.PREDICTED_VALUE,
		ResultFeature.PREDICTED_DISPLAY_VALUE
	}
)
public class SimpleTreeModelEvaluator extends TreeModelEvaluator {

	private SimpleTreeModelEvaluator(){
	}

	public SimpleTreeModelEvaluator(PMML pmml){
		this(pmml, PMMLUtil.findModel(pmml, TreeModel.class));
	}

	public SimpleTreeModelEvaluator(PMML pmml, TreeModel treeModel){
		super(pmml, treeModel);

		TreeModel.MissingValueStrategy missingValueStrategy = treeModel.getMissingValueStrategy();
		switch(missingValueStrategy){
			case NULL_PREDICTION:
			case LAST_PREDICTION:
			case DEFAULT_CHILD:
			case NONE:
				break;
			default:
				throw new UnsupportedAttributeException(treeModel, missingValueStrategy);
		}

		Targets targets = treeModel.getTargets();
		if(targets != null){
			throw new UnsupportedElementException(targets);
		}
	}

	@Override
	protected <V extends Number> Map<FieldName, ?> evaluateRegression(ValueFactory<V> valueFactory, EvaluationContext context){
		return evaluateAny(context);
	}

	@Override
	protected <V extends Number> Map<FieldName, ?> evaluateClassification(ValueFactory<V> valueFactory, EvaluationContext context){
		return evaluateAny(context);
	}

	private Map<FieldName, ?> evaluateAny(EvaluationContext context){
		TargetField targetField = getTargetField();

		Object result = null;

		Node node = evaluateTree(context);
		if(node != null){
			Object score = node.getScore();

			result = TypeUtil.parseOrCast(targetField.getDataType(), score);
		}

		return Collections.singletonMap(targetField.getFieldName(), result);
	}

	private Node evaluateTree(EvaluationContext context){
		TreeModel treeModel = getModel();

		Node root = treeModel.getNode();

		{
			Boolean status = PredicateUtil.evaluatePredicateContainer(root, context);

			if(status == null || !status.booleanValue()){
				return null;
			}
		}

		Node node = root;

		children:
		while(node.hasNodes()){
			List<Node> children = node.getNodes();

			for(int i = 0, max = children.size(); i < max; i++){
				Node child = children.get(i);

				Boolean status = PredicateUtil.evaluatePredicateContainer(child, context);

				if(status == null){
					TreeModel.MissingValueStrategy missingValueStrategy = treeModel.getMissingValueStrategy();
					switch(missingValueStrategy){
						case NULL_PREDICTION:
							return null;
						case LAST_PREDICTION:
							break children;
						case DEFAULT_CHILD:
							{
								Node defaultChild = findDefaultChild(node);

								node = defaultChild;

								continue children;
							}
						case NONE:
							continue;
						default:
							throw new UnsupportedAttributeException(treeModel, missingValueStrategy);
					}
				} else

				if(status.booleanValue()){
					node = child;

					continue children;
				}
			}

			TreeModel.NoTrueChildStrategy noTrueChildStrategy = treeModel.getNoTrueChildStrategy();
			switch(noTrueChildStrategy){
				case RETURN_NULL_PREDICTION:
					return null;
				case RETURN_LAST_PREDICTION:

					// "Return the parent Node only if it specifies a score attribute"
					if(node.hasScore()){
						break children;
					}

					return null;
				default:
					throw new UnsupportedAttributeException(treeModel, noTrueChildStrategy);
			}
		}

		// "It is not possible that the scoring process ends in a Node which does not have a score attribute"
		if(!node.hasScore()){
			throw new MissingAttributeException(node, PMMLAttributes.COMPLEXNODE_SCORE);
		}

		return node;
	}
}