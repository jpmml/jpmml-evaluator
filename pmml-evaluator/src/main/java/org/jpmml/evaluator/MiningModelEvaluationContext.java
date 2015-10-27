/*
 * Copyright (c) 2014 Villu Ruusmann
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

import java.util.HashMap;
import java.util.Map;

public class MiningModelEvaluationContext extends ModelEvaluationContext {

	private Map<String, SegmentResultMap> results = new HashMap<>();


	public MiningModelEvaluationContext(ModelEvaluationContext parent, MiningModelEvaluator modelEvaluator){
		super(parent, modelEvaluator);
	}

	@Override
	public MiningModelEvaluator getModelEvaluator(){
		return (MiningModelEvaluator)super.getModelEvaluator();
	}

	SegmentResultMap getResult(String id){
		return this.results.get(id);
	}

	void putResult(String id, SegmentResultMap result){
		this.results.put(id, result);
	}
}