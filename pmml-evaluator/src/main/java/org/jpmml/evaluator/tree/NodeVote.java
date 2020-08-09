/*
 * Copyright (c) 2018 Villu Ruusmann
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

import org.dmg.pmml.DataType;
import org.dmg.pmml.tree.Node;
import org.jpmml.evaluator.EntityUtil;
import org.jpmml.evaluator.HasEntityRegistry;
import org.jpmml.evaluator.TypeUtil;
import org.jpmml.evaluator.Vote;
import org.jpmml.model.ToStringHelper;

abstract
public class NodeVote extends Vote implements HasEntityRegistry<Node>, HasDecisionPath {

	private Node node = null;


	NodeVote(Node node){
		setNode(node);
	}

	@Override
	protected void computeResult(DataType dataType){
		Node node = getNode();

		Object result = TypeUtil.parseOrCast(dataType, node.getScore());

		setResult(result);
	}

	@Override
	protected ToStringHelper toStringHelper(){
		ToStringHelper helper = super.toStringHelper()
			.add("entityId", getEntityId());

		return helper;
	}

	@Override
	public String getEntityId(){
		Node node = getNode();

		return EntityUtil.getId(node, this);
	}

	@Override
	public Node getNode(){
		return this.node;
	}

	private void setNode(Node node){

		if(node == null){
			throw new IllegalArgumentException();
		}

		this.node = node;
	}
}