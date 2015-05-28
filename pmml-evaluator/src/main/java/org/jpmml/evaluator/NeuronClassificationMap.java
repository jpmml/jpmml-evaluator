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

import java.util.Set;

import com.google.common.annotations.Beta;
import com.google.common.collect.BiMap;
import org.dmg.pmml.Entity;

@Beta
public class NeuronClassificationMap extends EntityClassificationMap<Entity> implements HasProbability {

	protected NeuronClassificationMap(BiMap<String, Entity> entityRegistry){
		super(Type.PROBABILITY, entityRegistry);
	}

	@Override
	public Set<String> getCategoryValues(){
		return keySet();
	}

	@Override
	public Double getProbability(String value){
		return get(value);
	}
}