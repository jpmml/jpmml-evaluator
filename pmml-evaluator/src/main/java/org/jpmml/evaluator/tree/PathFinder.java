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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;

import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.tree.Node;
import org.jpmml.model.visitors.AbstractVisitor;

abstract
class PathFinder extends AbstractVisitor implements Predicate<Node> {

	private List<Node> path = null;


	@Override
	public VisitorAction visit(Node node){

		if(test(node)){
			List<Node> path = new ArrayList<>();
			path.add(node);

			Deque<PMMLObject> parents = getParents();
			for(PMMLObject parent : parents){

				if(parent instanceof Node){
					path.add((Node)parent);

					continue;
				}

				break;
			}

			Collections.reverse(path);

			setPath(path);

			return VisitorAction.TERMINATE;
		}

		return super.visit(node);
	}

	public List<Node> getPath(){
		return this.path;
	}

	private void setPath(List<Node> path){
		this.path = path;
	}
}