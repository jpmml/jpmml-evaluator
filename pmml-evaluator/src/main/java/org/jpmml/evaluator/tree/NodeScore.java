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
package org.jpmml.evaluator.tree;

import com.google.common.base.Objects.ToStringHelper;
import com.google.common.collect.BiMap;
import org.dmg.pmml.tree.Node;
import org.jpmml.evaluator.EntityUtil;
import org.jpmml.evaluator.HasEntityId;
import org.jpmml.evaluator.HasEntityRegistry;
import org.jpmml.evaluator.Regression;
import org.jpmml.evaluator.Value;

public class NodeScore<V extends Number> extends Regression<V> implements HasEntityId, HasEntityRegistry<Node> {

	private BiMap<String, Node> entityRegistry = null;

	private Node node = null;


	NodeScore(Value<V> value, BiMap<String, Node> entityRegistry, Node node){
		super(value);

		setEntityRegistry(entityRegistry);
		setNode(node);
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
	public BiMap<String, Node> getEntityRegistry(){
		return this.entityRegistry;
	}

	private void setEntityRegistry(BiMap<String, Node> entityRegistry){
		this.entityRegistry = entityRegistry;
	}

	public Node getNode(){
		return this.node;
	}

	private void setNode(Node node){
		this.node = node;
	}
}