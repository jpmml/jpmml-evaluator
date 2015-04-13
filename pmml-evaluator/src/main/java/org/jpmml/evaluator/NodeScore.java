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

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import org.dmg.pmml.Node;

@Beta
public class NodeScore implements Computable, HasEntityId {

	private Node node = null;

	private Object result = null;


	protected NodeScore(Node node, Object result){
		setNode(node);
		setResult(result);
	}

	@Override
	public String getEntityId(){
		Node node = getNode();

		return node.getId();
	}

	@Override
	public String toString(){
		ToStringHelper helper = Objects.toStringHelper(this)
			.add("entityId", getEntityId());

		return helper.toString();
	}

	public Node getNode(){
		return this.node;
	}

	private void setNode(Node node){
		this.node = node;
	}

	@Override
	public Object getResult(){
		return this.result;
	}

	private void setResult(Object result){
		this.result = result;
	}
}