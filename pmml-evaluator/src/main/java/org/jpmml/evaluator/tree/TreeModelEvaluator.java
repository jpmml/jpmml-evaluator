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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.dmg.pmml.EmbeddedModel;
import org.dmg.pmml.Output;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Score;
import org.dmg.pmml.Targets;
import org.dmg.pmml.regression.Regression;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.PMMLAttributes;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.PMMLUtil;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.regression.RegressionTableUtil;
import org.jpmml.model.InvalidAttributeException;
import org.jpmml.model.InvalidElementListException;
import org.jpmml.model.UnsupportedElementException;

abstract
public class TreeModelEvaluator extends ModelEvaluator<TreeModel> {

	protected TreeModelEvaluator(){
	}

	public TreeModelEvaluator(PMML pmml){
		this(pmml, PMMLUtil.findModel(pmml, TreeModel.class));
	}

	public TreeModelEvaluator(PMML pmml, TreeModel treeModel){
		super(pmml, treeModel);

		@SuppressWarnings("unused")
		Node root = treeModel.requireNode();
	}

	@Override
	public String getSummary(){
		return "Tree model";
	}

	static
	protected Node findDefaultChild(Node node){
		Object defaultChild = node.requireDefaultChild();

		if(defaultChild instanceof Node){
			return (Node)defaultChild;
		}

		List<Node> children = node.getNodes();
		for(int i = 0, max = children.size(); i < max; i++){
			Node child = children.get(i);

			Object id = child.getId();
			if(id != null && Objects.equals(id, defaultChild)){
				return child;
			}
		}

		// "Only Nodes which are immediate children of the respective Node can be referenced"
		throw new InvalidAttributeException(node, PMMLAttributes.COMPLEXNODE_DEFAULTCHILD, defaultChild);
	}

	static
	protected boolean hasScore(Node node){
		return node.hasScore() || node.hasScores();
	}

	static
	protected Map<String, Object> resolveScores(List<TargetField> targetFields, Node node){
		List<Score> scores = node.requireScores();

		if(scores.size() != targetFields.size()){
			throw new InvalidElementListException(scores);
		}

		Map<String, Object> results = new LinkedHashMap<>(2 * targetFields.size());

		for(int i = 0, max = targetFields.size(); i < max; i++){
			TargetField targetField = targetFields.get(i);

			results.put(targetField.getName(), targetField);
		} // End for

		for(int i = 0, max = scores.size(); i < max; i++){
			Score score = scores.get(i);

			String name = score.requireTargetField();
			Object value = score.requireValue();

			Object placeholderValue = results.get(name);
			if(placeholderValue instanceof TargetField){
				results.put(name, value);
			} else

			{
				throw new InvalidAttributeException(score, org.dmg.pmml.PMMLAttributes.SCORE_TARGETFIELD, name);
			}
		}

		return results;
	}

	static
	protected <V extends Number> Value<V> evaluateEmbeddedRegression(ValueFactory<V> valueFactory, EmbeddedModel embeddedModel, EvaluationContext context){

		if(embeddedModel instanceof Regression){
			Regression regression = (Regression)embeddedModel;

			Targets targets = regression.getTargets();
			if(targets != null && targets.hasTargets()){
				throw new UnsupportedElementException(targets);
			}

			Output output = regression.getOutput();
			if(output != null && output.hasOutputFields()){
				throw new UnsupportedElementException(output);
			}

			return RegressionTableUtil.evaluateRegression(valueFactory, regression, context);
		}

		throw new UnsupportedElementException(embeddedModel);
	}
}