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

import org.dmg.pmml.DefineFunction;
import org.dmg.pmml.FieldName;

public class DefineFunctionEvaluationContext extends EvaluationContext {

	private EvaluationContext parent = null;


	DefineFunctionEvaluationContext(EvaluationContext parent){
		setParent(parent);
	}

	@Override
	public FieldValue createFieldValue(FieldName name, Object value){
		throw new EvaluationException();
	}

	@Override
	protected DefineFunction getDefineFunction(String name){
		EvaluationContext parent = getParent();

		return parent.getDefineFunction(name);
	}

	public EvaluationContext getParent(){
		return this.parent;
	}

	private void setParent(EvaluationContext parent){
		this.parent = parent;
	}
}