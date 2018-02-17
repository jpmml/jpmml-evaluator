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

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.dmg.pmml.CompoundPredicate;
import org.dmg.pmml.EmbeddedModel;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MathContext;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.ScoreDistribution;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.evaluator.CacheUtil;
import org.jpmml.evaluator.EntityUtil;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.EvaluationException;
import org.jpmml.evaluator.HasEntityRegistry;
import org.jpmml.evaluator.InvalidAttributeException;
import org.jpmml.evaluator.MisplacedAttributeException;
import org.jpmml.evaluator.MissingAttributeException;
import org.jpmml.evaluator.MissingElementException;
import org.jpmml.evaluator.ModelEvaluationContext;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.OutputUtil;
import org.jpmml.evaluator.PMMLAttributes;
import org.jpmml.evaluator.PMMLElements;
import org.jpmml.evaluator.PredicateUtil;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TargetUtil;
import org.jpmml.evaluator.UndefinedResultException;
import org.jpmml.evaluator.UnsupportedAttributeException;
import org.jpmml.evaluator.UnsupportedElementException;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueMap;

public class TreeModelEvaluator extends ModelEvaluator<TreeModel> implements HasEntityRegistry<Node> {

	transient
	private BiMap<String, Node> entityRegistry = null;


	public TreeModelEvaluator(PMML pmml){
		this(pmml, selectModel(pmml, TreeModel.class));
	}

	public TreeModelEvaluator(PMML pmml, TreeModel treeModel){
		super(pmml, treeModel);

		Node root = treeModel.getNode();
		if(root == null){
			throw new MissingElementException(treeModel, PMMLElements.TREEMODEL_NODE);
		}
	}

	@Override
	public String getSummary(){
		return "Tree model";
	}

	@Override
	public BiMap<String, Node> getEntityRegistry(){

		if(this.entityRegistry == null){
			this.entityRegistry = getValue(TreeModelEvaluator.entityCache);
		}

		return this.entityRegistry;
	}

	@Override
	public Map<FieldName, ?> evaluate(ModelEvaluationContext context){
		TreeModel treeModel = ensureScorableModel();

		ValueFactory<?> valueFactory;

		MathContext mathContext = treeModel.getMathContext();
		switch(mathContext){
			case FLOAT:
			case DOUBLE:
				valueFactory = ensureValueFactory();
				break;
			default:
				throw new UnsupportedAttributeException(treeModel, mathContext);
		}

		Map<FieldName, ?> predictions;

		MiningFunction miningFunction = treeModel.getMiningFunction();
		switch(miningFunction){
			case REGRESSION:
				predictions = evaluateRegression(valueFactory, context);
				break;
			case CLASSIFICATION:
				predictions = evaluateClassification(valueFactory, context);
				break;
			case ASSOCIATION_RULES:
			case SEQUENCES:
			case CLUSTERING:
			case TIME_SERIES:
			case MIXED:
				throw new InvalidAttributeException(treeModel, miningFunction);
			default:
				throw new UnsupportedAttributeException(treeModel, miningFunction);
		}

		return OutputUtil.evaluate(predictions, context);
	}

	private <V extends Number> Map<FieldName, ?> evaluateRegression(ValueFactory<V> valueFactory, EvaluationContext context){
		TargetField targetField = getTargetField();

		Trail trail = new Trail();

		Node node = evaluateTree(trail, context);
		if(node == null){
			return TargetUtil.evaluateRegressionDefault(valueFactory, targetField);
		}

		NodeScore<V> result = createNodeScore(valueFactory, targetField, node);

		return TargetUtil.evaluateRegression(targetField, result);
	}

	private <V extends Number> Map<FieldName, ?> evaluateClassification(ValueFactory<V> valueFactory, EvaluationContext context){
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
			missingValuePenalty = Math.pow(treeModel.getMissingValuePenalty(), missingLevels);
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
				throw new MissingAttributeException(node, PMMLAttributes.NODE_SCORE);
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

		// "The defaultChild missing value strategy requires the presence of the defaultChild attribute in every non-leaf Node"
		String defaultChild = node.getDefaultChild();
		if(defaultChild == null){
			throw new MissingAttributeException(node, PMMLAttributes.NODE_DEFAULTCHILD);
		}

		trail.addMissingLevel();

		List<Node> children = node.getNodes();
		for(int i = 0, max = children.size(); i < max; i++){
			Node child = children.get(i);

			String id = child.getId();
			if(id != null && (id).equals(defaultChild)){
				// The predicate of the referenced Node is not evaluated
				return handleTrue(trail, child, context);
			}
		}

		// "Only Nodes which are immediate children of the respective Node can be referenced"
		throw new InvalidAttributeException(node, PMMLAttributes.NODE_DEFAULTCHILD, defaultChild);
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
		Value<V> value = valueFactory.newValue(node.getScore());

		value = TargetUtil.evaluateRegressionInternal(targetField, value);

		NodeScore<V> result = new NodeScore<V>(value, node){

			@Override
			public BiMap<String, Node> getEntityRegistry(){
				return TreeModelEvaluator.this.getEntityRegistry();
			}
		};

		return result;
	}

	private NodeVote createNodeVote(Node node){
		NodeVote result = new NodeVote(node){

			@Override
			public BiMap<String, Node> getEntityRegistry(){
				return TreeModelEvaluator.this.getEntityRegistry();
			}
		};

		return result;
	}

	private <V extends Number> NodeScoreDistribution<V> createNodeScoreDistribution(ValueFactory<V> valueFactory, Node node, double missingValuePenalty){
		List<ScoreDistribution> scoreDistributions = node.getScoreDistributions();

		NodeScoreDistribution<V> result = new NodeScoreDistribution<V>(new ValueMap<String, V>(2 * scoreDistributions.size()), node){

			@Override
			public BiMap<String, Node> getEntityRegistry(){
				return TreeModelEvaluator.this.getEntityRegistry();
			}
		};

		Value<V> sum = valueFactory.newValue();

		// "If the predicted probability is defined for any class label, then it must be defined for all"
		boolean hasProbabilities = false;

		for(int i = 0, max = scoreDistributions.size(); i < max; i++){
			ScoreDistribution scoreDistribution = scoreDistributions.get(i);

			Double probability = scoreDistribution.getProbability();

			if(i == 0){
				hasProbabilities = (probability != null);
			}

			Value<V> value;

			if(hasProbabilities){

				if(probability == null){
					throw new MissingAttributeException(scoreDistribution, PMMLAttributes.SCOREDISTRIBUTION_PROBABILITY);
				} // End if

				if(probability < 0d || probability > 1d){
					throw new InvalidAttributeException(scoreDistribution, PMMLAttributes.SCOREDISTRIBUTION_PROBABILITY, probability);
				}

				sum.add(probability);

				value = valueFactory.newValue(probability);
			} else

			{
				if(probability != null){
					throw new MisplacedAttributeException(scoreDistribution, PMMLAttributes.SCOREDISTRIBUTION_PROBABILITY, probability);
				}

				double recordCount = scoreDistribution.getRecordCount();
				if(recordCount != 0d){
					sum.add(recordCount);
				}

				value = valueFactory.newValue(recordCount);
			}

			result.put(scoreDistribution.getValue(), value);

			Double confidence = scoreDistribution.getConfidence();
			if(confidence != null){
				value = valueFactory.newValue(confidence);

				if(missingValuePenalty != 1d){
					value.multiply(missingValuePenalty);
				}

				result.putConfidence(scoreDistribution.getValue(), value);
			}
		}

		// "The predicted probabilities must sum to 1"
		if(!sum.equals(1d)){
			ValueMap<String, V> values = result.getValues();

			if(sum.equals(0d)){
				throw new UndefinedResultException();
			}

			for(Value<V> value : values){
				value.divide(sum);
			}
		}

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

	private static final LoadingCache<TreeModel, BiMap<String, Node>> entityCache = CacheUtil.buildLoadingCache(new CacheLoader<TreeModel, BiMap<String, Node>>(){

		@Override
		public BiMap<String, Node> load(TreeModel treeModel){
			ImmutableBiMap.Builder<String, Node> builder = new ImmutableBiMap.Builder<>();

			builder = collectNodes(treeModel.getNode(), new AtomicInteger(1), builder);

			return builder.build();
		}

		private ImmutableBiMap.Builder<String, Node> collectNodes(Node node, AtomicInteger index, ImmutableBiMap.Builder<String, Node> builder){
			builder = EntityUtil.put(node, index, builder);

			if(!node.hasNodes()){
				return builder;
			}

			List<Node> children = node.getNodes();
			for(Node child : children){
				builder = collectNodes(child, index, builder);
			}

			return builder;
		}
	});
}