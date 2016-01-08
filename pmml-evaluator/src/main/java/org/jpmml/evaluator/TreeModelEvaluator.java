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
package org.jpmml.evaluator;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Iterables;
import org.dmg.pmml.CompoundPredicate;
import org.dmg.pmml.DataType;
import org.dmg.pmml.EmbeddedModel;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningFunctionType;
import org.dmg.pmml.MissingValueStrategyType;
import org.dmg.pmml.NoTrueChildStrategyType;
import org.dmg.pmml.Node;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.ScoreDistribution;
import org.dmg.pmml.TreeModel;

public class TreeModelEvaluator extends ModelEvaluator<TreeModel> implements HasEntityRegistry<Node> {

	public TreeModelEvaluator(PMML pmml){
		super(pmml, TreeModel.class);
	}

	public TreeModelEvaluator(PMML pmml, TreeModel treeModel){
		super(pmml, treeModel);
	}

	@Override
	public String getSummary(){
		return "Tree model";
	}

	@Override
	public BiMap<String, Node> getEntityRegistry(){
		return getValue(TreeModelEvaluator.entityCache);
	}

	@Override
	public Map<FieldName, ?> evaluate(ModelEvaluationContext context){
		TreeModel treeModel = getModel();
		if(!treeModel.isScorable()){
			throw new InvalidResultException(treeModel);
		}

		Map<FieldName, ?> predictions;

		MiningFunctionType miningFunction = treeModel.getFunctionName();
		switch(miningFunction){
			case REGRESSION:
				predictions = evaluateRegression(context);
				break;
			case CLASSIFICATION:
				predictions = evaluateClassification(context);
				break;
			default:
				throw new UnsupportedFeatureException(treeModel, miningFunction);
		}

		return OutputUtil.evaluate(predictions, context);
	}

	private Map<FieldName, ?> evaluateRegression(ModelEvaluationContext context){
		Double score = null;

		Trail trail = new Trail();

		Node node = evaluateTree(trail, context);
		if(node != null){
			score = (Double)TypeUtil.parseOrCast(DataType.DOUBLE, node.getScore());
		}

		Map<FieldName, ?> result = TargetUtil.evaluateRegression(score, context);

		Map.Entry<FieldName, ?> resultEntry = Iterables.getOnlyElement(result.entrySet());

		return Collections.singletonMap(resultEntry.getKey(), createNodeScore(node, resultEntry.getValue()));
	}

	private Map<FieldName, ? extends Classification> evaluateClassification(ModelEvaluationContext context){
		TreeModel treeModel = getModel();

		NodeScoreDistribution result = null;

		Trail trail = new Trail();

		Node node = evaluateTree(trail, context);
		if(node != null){
			double missingValuePenalty = 1d;

			int missingLevels = trail.getMissingLevels();
			for(int i = 0; i < missingLevels; i++){
				missingValuePenalty *= treeModel.getMissingValuePenalty();
			}

			result = createNodeScoreDistribution(node, missingValuePenalty);
		}

		return TargetUtil.evaluateClassification(result, context);
	}

	private Node evaluateTree(Trail trail, EvaluationContext context){
		TreeModel treeModel = getModel();

		Node root = treeModel.getNode();
		if(root == null){
			throw new InvalidFeatureException(treeModel);
		}

		Boolean status = evaluateNode(root, trail, context);

		if(status != null && status.booleanValue()){
			NodeResult result = handleTrue(root, trail, context);

			Node node = result.getNode();
			if(node != null){
				String score = node.getScore();

				// "It is not possible that the scoring process ends in a Node which does not have a score attribute"
				if(score == null){
					throw new InvalidFeatureException(node);
				}
			}

			return node;
		}

		return null;
	}

	private Boolean evaluateNode(Node node, Trail trail, EvaluationContext context){
		EmbeddedModel embeddedModel = node.getEmbeddedModel();
		if(embeddedModel != null){
			throw new UnsupportedFeatureException(embeddedModel);
		}

		Predicate predicate = node.getPredicate();
		if(predicate == null){
			throw new InvalidFeatureException(node);
		} // End if

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

	private NodeResult handleTrue(Node node, Trail trail, EvaluationContext context){

		// A "true" leaf node
		if(!node.hasNodes()){
			return new NodeResult(node);
		}

		trail.push(node);

		List<Node> children = node.getNodes();
		for(Node child : children){
			Boolean status = evaluateNode(child, trail, context);

			if(status == null){
				NodeResult result = handleMissingValue(node, child, trail, context);

				if(result != null){
					return result;
				}
			} else

			if(status.booleanValue()){
				return handleTrue(child, trail, context);
			}
		}

		// A "true" non-leaf node
		return handleNoTrueChild(node, trail, context);
	}

	private NodeResult handleDefaultChild(Node node, Trail trail, EvaluationContext context){

		// "The defaultChild missing value strategy requires the presence of the defaultChild attribute in every non-leaf Node"
		String defaultChild = node.getDefaultChild();
		if(defaultChild == null){
			throw new InvalidFeatureException(node);
		}

		trail.addMissingLevel();

		List<Node> children = node.getNodes();
		for(Node child : children){
			String id = child.getId();

			if(id != null && (id).equals(defaultChild)){
				// The predicate of the referenced Node is not evaluated
				return handleTrue(child, trail, context);
			}
		}

		// "Only Nodes which are immediate children of the respective Node can be referenced"
		throw new InvalidFeatureException(node);
	}

	private NodeResult handleNoTrueChild(Node node, Trail trail, EvaluationContext context){
		TreeModel treeModel = getModel();

		NoTrueChildStrategyType noTrueChildStrategy = treeModel.getNoTrueChildStrategy();
		switch(noTrueChildStrategy){
			case RETURN_NULL_PREDICTION:
				return new NodeResult(null);
			case RETURN_LAST_PREDICTION:
				if(trail.size() > 0){
					Node parent = trail.getLastPrediction();

					// "Return the parent Node only if it specifies a score attribute"
					if(parent.getScore() != null){
						return new NodeResult(parent);
					}
				}
				return new NodeResult(null);
			default:
				throw new UnsupportedFeatureException(treeModel, noTrueChildStrategy);
		}
	}

	/**
	 * @param parent The parent Node of the Node that evaluated to the missing value.
	 * @param node The Node that evaluated to the missing value.
	 */
	private NodeResult handleMissingValue(Node parent, Node node, Trail trail, EvaluationContext context){
		TreeModel treeModel = getModel();

		MissingValueStrategyType missingValueStrategy = treeModel.getMissingValueStrategy();
		switch(missingValueStrategy){
			case NULL_PREDICTION:
				return new NodeResult(null);
			case LAST_PREDICTION:
				return new NodeResult(trail.getLastPrediction());
			case DEFAULT_CHILD:
				if(parent == null){
					throw new EvaluationException();
				}
				return handleDefaultChild(parent, trail, context);
			case NONE:
				return null;
			default:
				throw new UnsupportedFeatureException(treeModel, missingValueStrategy);
		}
	}

	private NodeScore createNodeScore(Node node, Object value){
		BiMap<String, Node> entityRegistry = getEntityRegistry();

		NodeScore result = new NodeScore(entityRegistry, node, value);

		return result;
	}

	private NodeScoreDistribution createNodeScoreDistribution(Node node, double missingValuePenalty){
		BiMap<String, Node> entityRegistry = getEntityRegistry();

		NodeScoreDistribution result = new NodeScoreDistribution(entityRegistry, node);

		List<ScoreDistribution> scoreDistributions = node.getScoreDistributions();

		double sum = 0;

		for(ScoreDistribution scoreDistribution : scoreDistributions){
			sum += scoreDistribution.getRecordCount();
		} // End for

		for(ScoreDistribution scoreDistribution : scoreDistributions){
			Double probability = scoreDistribution.getProbability();
			if(probability == null){
				probability = (scoreDistribution.getRecordCount() / sum);
			}

			result.put(scoreDistribution.getValue(), probability);

			Double confidence = scoreDistribution.getConfidence();
			if(confidence != null){
				result.putConfidence(scoreDistribution.getValue(), confidence * missingValuePenalty);
			}
		}

		return result;
	}

	static
	private class Trail extends ArrayDeque<Node> {

		private int missingLevels = 0;


		public Trail(){
		}

		public Node getLastPrediction(){
			return getFirst();
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

	static
	private class NodeResult {

		private Node node = null;


		public NodeResult(Node node){
			setNode(node);
		}

		public Node getNode(){
			return this.node;
		}

		private void setNode(Node node){
			this.node = node;
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