/*
 * Copyright (c) 2017 Villu Ruusmann
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
package org.jpmml.evaluator.java;

import java.util.ArrayList;
import java.util.List;

import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.ModelEvaluationContext;
import org.jpmml.evaluator.mining.MiningModelEvaluationContext;

public class JavaModelEvaluationContext extends ModelEvaluationContext {

	private List<FieldValue> values = null;


	public JavaModelEvaluationContext(JavaModelEvaluator javaModelEvaluator){
		super(javaModelEvaluator);
	}

	public JavaModelEvaluationContext(MiningModelEvaluationContext parent, JavaModelEvaluator javaModelEvaluator){
		super(parent, javaModelEvaluator);
	}

	@Override
	public JavaModelEvaluator getModelEvaluator(){
		return (JavaModelEvaluator)super.getModelEvaluator();
	}

	public void cache(List<FieldName> names){

		if(names == null){
			this.values = null;

			return;
		}

		List<FieldValue> values = new ArrayList<>(names.size());

		for(FieldName name : names){
			FieldValue value = evaluate(name);

			values.add(value);
		}

		this.values = values;
	}

	public FieldValue evaluate(int index){
		List<FieldValue> values = this.values;

		if(values == null){
			throw new IllegalStateException();
		}

		return values.get(index);
	}
}