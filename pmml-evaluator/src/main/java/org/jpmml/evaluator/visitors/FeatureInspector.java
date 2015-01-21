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
package org.jpmml.evaluator.visitors;

import java.util.List;

import com.google.common.collect.Lists;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.Visitable;
import org.dmg.pmml.VisitorAction;
import org.jpmml.manager.PMMLException;
import org.jpmml.model.visitors.AbstractSimpleVisitor;

abstract
class FeatureInspector<E extends PMMLException> extends AbstractSimpleVisitor {

	private List<E> exceptions = Lists.newArrayList();


	@Override
	public VisitorAction visit(PMMLObject object){
		return VisitorAction.CONTINUE;
	}

	@Override
	public void applyTo(Visitable visitable){
		super.applyTo(visitable);

		List<E> exceptions = getExceptions();
		if(exceptions.size() > 0){
			throw exceptions.get(0);
		}
	}

	void report(E exception){
		this.exceptions.add(exception);
	}

	public List<E> getExceptions(){
		return this.exceptions;
	}
}