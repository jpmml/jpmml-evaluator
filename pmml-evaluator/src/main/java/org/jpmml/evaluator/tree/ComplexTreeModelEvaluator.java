/*
 * Copyright (c) 2013 Villu Ruusmann
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.dmg.pmml.CompoundPredicate;
import org.dmg.pmml.EmbeddedModel;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.ScoreDistribution;
import org.dmg.pmml.Visitor;
import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.PMMLAttributes;
import org.dmg.pmml.tree.PMMLElements;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.evaluator.EntityUtil;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.EvaluationException;
import org.jpmml.evaluator.InvalidAttributeException;
import org.jpmml.evaluator.MisplacedAttributeException;
import org.jpmml.evaluator.MissingAttributeException;
import org.jpmml.evaluator.MissingElementException;
import org.jpmml.evaluator.PMMLUtil;
import org.jpmml.evaluator.PredicateUtil;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TargetUtil;
import org.jpmml.evaluator.UndefinedResultException;
import org.jpmml.evaluator.UnsupportedAttributeException;
import org.jpmml.evaluator.UnsupportedElementException;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueMap;
import org.jpmml.model.visitors.AbstractVisitor;

public class ComplexTreeModelEvaluator extends TreeModelEvaluator implements HasNodeRegistry {

	private BiMap<String, Node> entityRegistry = ImmutableBiMap.of();


	private ComplexTreeModelEvaluator(){
	}

	public ComplexTreeModelEvaluator(PMML pmml){
		this(pmml, PMMLUtil.findModel(pmml, TreeModel.class));
	}

	public ComplexTreeModelEvaluator(PMML pmml, TreeModel treeModel){
		super(pmml, treeModel);

		Node root = treeModel.getNode();
		if(root == null){
			throw new MissingElementException(treeModel, PMMLElements.TREEMODEL_NODE);
		} else

		{
			List<Node> nodes = collectNodes(treeModel);

			this.entityRegistry = ImmutableBiMap.copyOf(EntityUtil.buildBiMap(nodes));
		}
	}

	@Override
	public BiMap<String, Node> getEntityRegistry(){
		return this.entityRegistry;
	}

	@Override
	public List<Node> getPath(String id){
		return getPath(resolveNode(id));
	}

	@Override
	public List<Node> getPathBetween(String parentId, String childId){
		return getPathBetween(resolveNode(parentId), resolveNode(childId));
	}

	@Override
	protected <V extends Number> Map<FieldName, ?> evaluateRegression(ValueFactory<V> valueFactory, EvaluationContext context){
		TargetField targetField = getTargetField();

		Trail trail = new Trail();

		Node node = evaluateTree(trail, context);
		if(node == null){
			return TargetUtil.evaluateRegressionDefault(valueFactory, targetField);
		}

		NodeScore<V> result = createNodeScore(valueFactory, targetField, node);

		return TargetUtil.evaluateRegression(targetField, result);
	}

	@Override
	protected <V extends Number> Map<FieldName, ?> evaluateClassification(ValueFactory<V> valueFactory, EvaluationContext context){
		TreeModel treeModel = getModel();

		TargetField targetField = getTargetField();

		Trail trail = new Trail();

		Node node = evaluateTree(trail, context);
		if(node == null){
			return TargetUtil.evaluateClassificationDefault(valueFactory, targetField);
		} // End if

		if(!node.hasScoreDistributions()){
			NodeVote result = createNodeVote(node);

			return TargetUtil.evaluateVote(targetField, result);
		}

		double missingValuePenalty = 1d;

		int missingLevels = trail.getMissingLevels();
		if(missingLevels > 0){
			missingValuePenalty = (treeModel.getMissingValuePenalty()).doubleValue();

			if(missingLevels > 1){
				missingValuePenalty = Math.pow(missingValuePenalty, missingLevels);
			}
		}

		NodeScoreDistribution<V> result = createNodeScoreDistribution(valueFactory, node, missingValuePenalty);

		return TargetUtil.evaluateClassification(targetField, result);
	}

	private Node evaluateTree(Trail trail, EvaluationContext context){
		TreeModel treeModel = getModel();

		Node root = treeModel.getNode();

		Boolean status = evaluateNode(trail, root, context);

		if(status != null && status.booleanValue()){
			trail = handleTrue(trail, root, context);

			Node node = trail.getResult();

			// "It is not possible that the scoring process ends in a Node which does not have a score attribute"
			if(node != null && !node.hasScore()){
				throw new MissingAttributeException(node, PMMLAttributes.COMPLEXNODE_SCORE);
			}

			return node;
		}

		return null;
	}

	private Boolean evaluateNode(Trail trail, Node node, EvaluationContext context){
		EmbeddedModel embeddedModel = node.getEmbeddedModel();
		if(embeddedModel != null){
			throw new UnsupportedElementException(embeddedModel);
		}

		Predicate predicate = PredicateUtil.ensurePredicate(node);

		// A compound predicate whose boolean operator is "surrogate" represents a special case
		if(predicate instanceof CompoundPredicate){
			CompoundPredicate compoundPredicate = (CompoundPredicate)predicate;

			PredicateUtil.CompoundPredicateResult result = PredicateUtil.evaluateCompoundPredicateInternal(compoundPredicate, context);
			if(result.isAlternative()){
				trail.addMissingLevel();
			}

			return result.getResult();
		} else

		{
			return PredicateUtil.evaluate(predicate, context);
		}
	}

	private Trail handleTrue(Trail trail, Node node, EvaluationContext context){

		// A "true" leaf node
		if(!node.hasNodes()){
			return trail.selectNode(node);
		}

		trail.push(node);

		List<Node> children = node.getNodes();
		for(int i = 0, max = children.size(); i < max; i++){
			Node child = children.get(i);

			Boolean status = evaluateNode(trail, child, context);

			if(status == null){
				Trail destination = handleMissingValue(trail, node, child, context);

				if(destination != null){
					return destination;
				}
			} else

			if(status.booleanValue()){
				return handleTrue(trail, child, context);
			}
		}

		// A "true" non-leaf node
		return handleNoTrueChild(trail);
	}

	private Trail handleDefaultChild(Trail trail, Node node, EvaluationContext context){
		Node defaultChild = findDefaultChild(node);

		trail.addMissingLevel();

		// The predicate of the referenced Node is not evaluated
		return handleTrue(trail, defaultChild, context);
	}

	private Trail handleNoTrueChild(Trail trail){
		TreeModel treeModel = getModel();

		TreeModel.NoTrueChildStrategy noTrueChildStrategy = treeModel.getNoTrueChildStrategy();
		switch(noTrueChildStrategy){
			case RETURN_NULL_PREDICTION:
				return trail.selectNull();
			case RETURN_LAST_PREDICTION:
				Node lastPrediction = trail.getLastPrediction();

				// "Return the parent Node only if it specifies a score attribute"
				if(lastPrediction.hasScore()){
					return trail.selectLastPrediction();
				}
				return trail.selectNull();
			default:
				throw new UnsupportedAttributeException(treeModel, noTrueChildStrategy);
		}
	}

	/**
	 * @param parent The parent Node of the Node that evaluated to the missing value.
	 * @param node The Node that evaluated to the missing value.
	 */
	private Trail handleMissingValue(Trail trail, Node parent, Node node, EvaluationContext context){
		TreeModel treeModel = getModel();

		TreeModel.MissingValueStrategy missingValueStrategy = treeModel.getMissingValueStrategy();
		switch(missingValueStrategy){
			case NULL_PREDICTION:
				return trail.selectNull();
			case LAST_PREDICTION:
				return trail.selectLastPrediction();
			case DEFAULT_CHILD:
				return handleDefaultChild(trail, parent, context);
			case NONE:
				return null;
			default:
				throw new UnsupportedAttributeException(treeModel, missingValueStrategy);
		}
	}

	private <V extends Number> NodeScore<V> createNodeScore(ValueFactory<V> valueFactory, TargetField targetField, Node node){
		Object score = node.getScore();

		Value<V> value;

		if(score instanceof Number){
			value = valueFactory.newValue((Number)score);
		} else

		{
			value = valueFactory.newValue((String)score);
		}

		value = TargetUtil.evaluateRegressionInternal(targetField, value);

		NodeScore<V> result = new NodeScore<V>(value, node){

			@Override
			public BiMap<String, Node> getEntityRegistry(){
				return ComplexTreeModelEvaluator.this.getEntityRegistry();
			}

			@Override
			public List<Node> getDecisionPath(){
				return ComplexTreeModelEvaluator.this.getPath(getNode());
			}
		};

		return result;
	}

	private NodeVote createNodeVote(Node node){
		NodeVote result = new NodeVote(node){

			@Override
			public BiMap<String, Node> getEntityRegistry(){
				return ComplexTreeModelEvaluator.this.getEntityRegistry();
			}

			@Override
			public List<Node> getDecisionPath(){
				return ComplexTreeModelEvaluator.this.getPath(getNode());
			}
		};

		return result;
	}

	private <V extends Number> NodeScoreDistribution<V> createNodeScoreDistribution(ValueFactory<V> valueFactory, Node node, double missingValuePenalty){
		List<ScoreDistribution> scoreDistributions = node.getScoreDistributions();

		NodeScoreDistribution<V> result = new NodeScoreDistribution<V>(new ValueMap<Object, V>(2 * scoreDistributions.size()), node){

			@Override
			public BiMap<String, Node> getEntityRegistry(){
				return ComplexTreeModelEvaluator.this.getEntityRegistry();
			}

			@Override
			public List<Node> getDecisionPath(){
				return ComplexTreeModelEvaluator.this.getPath(getNode());
			}
		};

		Value<V> sum = valueFactory.newValue();

		// "If the predicted probability is defined for any class label, then it must be defined for all"
		boolean hasProbabilities = false;

		for(int i = 0, max = scoreDistributions.size(); i < max; i++){
			ScoreDistribution scoreDistribution = scoreDistributions.get(i);

			Number probability = scoreDistribution.getProbability();

			if(i == 0){
				hasProbabilities = (probability != null);
			}

			Value<V> value;

			if(hasProbabilities){

				if(probability == null){
					throw new MissingAttributeException(scoreDistribution, org.dmg.pmml.PMMLAttributes.SCOREDISTRIBUTION_PROBABILITY);
				} // End if

				if(probability.doubleValue() < 0d || probability.doubleValue() > 1d){
					throw new InvalidAttributeException(scoreDistribution, org.dmg.pmml.PMMLAttributes.SCOREDISTRIBUTION_PROBABILITY, probability);
				}

				sum.add(probability);

				value = valueFactory.newValue(probability);
			} else

			{
				if(probability != null){
					throw new MisplacedAttributeException(scoreDistribution, org.dmg.pmml.PMMLAttributes.SCOREDISTRIBUTION_PROBABILITY, probability);
				}

				Number recordCount = scoreDistribution.getRecordCount();
				if(recordCount == null){
					throw new MissingAttributeException(scoreDistribution, org.dmg.pmml.PMMLAttributes.SCOREDISTRIBUTION_RECORDCOUNT);
				}

				sum.add(recordCount);

				value = valueFactory.newValue(recordCount);
			}

			Object targetCategory = scoreDistribution.getValue();
			if(targetCategory == null){
				throw new MissingAttributeException(scoreDistribution, org.dmg.pmml.PMMLAttributes.SCOREDISTRIBUTION_VALUE);
			}

			result.put(targetCategory, value);

			Number confidence = scoreDistribution.getConfidence();
			if(confidence != null){
				value = valueFactory.newValue(confidence)
					.multiply(missingValuePenalty);

				result.putConfidence(targetCategory, value);
			}
		}

		// "The predicted probabilities must sum to 1"
		if(!sum.isOne()){
			ValueMap<Object, V> values = result.getValues();

			if(sum.isZero()){
				throw new UndefinedResultException();
			}

			for(Value<V> value : values){
				value.divide(sum);
			}
		}

		return result;
	}

	private List<Node> getPath(Node node){
		TreeModel treeModel = getModel();

		Node root = treeModel.getNode();

		return getPathBetween(root, node);
	}

	private List<Node> getPathBetween(Node parent, Node child){
		PathFinder pathFinder = new PathFinder(){

			@Override
			public boolean test(Node node){
				return Objects.equals(child, node);
			}
		};
		pathFinder.applyTo(parent);

		return pathFinder.getPath();
	}

	private Node resolveNode(String id){
		BiMap<String, Node> entityRegistry = getEntityRegistry();

		Node node = entityRegistry.get(id);
		if(node == null){
			throw new IllegalArgumentException(id);
		}

		return node;
	}

	static
	private List<Node> collectNodes(TreeModel treeModel){
		List<Node> result = new ArrayList<>();

		Visitor visitor = new AbstractVisitor(){

			@Override
			public VisitorAction visit(Node node){
				result.add(node);

				return super.visit(node);
			}
		};
		visitor.applyTo(treeModel);

		return result;
	}

	static
	private class Trail {

		private Node lastPrediction = null;

		private Node result = null;

		private int missingLevels = 0;


		public Trail(){
		}

		public void push(Node node){
			setLastPrediction(node);
		}

		public Trail selectNull(){
			setResult(null);

			return this;
		}

		public Trail selectNode(Node node){
			setResult(node);

			return this;
		}

		public Trail selectLastPrediction(){
			setResult(getLastPrediction());

			return this;
		}

		public Node getResult(){
			return this.result;
		}

		private void setResult(Node result){
			this.result = result;
		}

		public Node getLastPrediction(){

			if(this.lastPrediction == null){
				throw new EvaluationException("Empty trail");
			}

			return this.lastPrediction;
		}

		private void setLastPrediction(Node lastPrediction){
			this.lastPrediction = lastPrediction;
		}

		public void addMissingLevel(){
			setMissingLevels(getMissingLevels() + 1);
		}

		public int getMissingLevels(){
			return this.missingLevels;
		}

		private void setMissingLevels(int missingLevels){
			this.missingLevels = missingLevels;
		}
	}
}