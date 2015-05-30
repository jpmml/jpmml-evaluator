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

import java.util.List;
import java.util.Set;

import com.google.common.annotations.Beta;
import com.google.common.collect.BiMap;
import org.dmg.pmml.Cluster;

import static com.google.common.base.Preconditions.checkArgument;

@Beta
public class ClusterAffinityDistribution extends EntityClassification<Cluster> implements HasEntityIdRanking, HasDisplayValue, HasAffinityRanking, HasEntityAffinity {

	protected ClusterAffinityDistribution(Type type, BiMap<String, Cluster> entityRegistry){
		super(type, entityRegistry);

		checkArgument((Type.DISTANCE).equals(type) || (Type.SIMILARITY).equals(type));
	}

	@Override
	public Set<String> getCategoryValues(){
		return keySet();
	}

	@Override
	public List<String> getEntityIdRanking(){
		return getWinnerKeys();
	}

	@Override
	public String getDisplayValue(){
		Cluster cluster = getEntity();

		return cluster.getName();
	}

	@Override
	public Double getAffinity(String value){
		return get(value);
	}

	@Override
	public List<Double> getAffinityRanking(){
		return getWinnerValues();
	}

	@Override
	public Double getEntityAffinity(){
		return getAffinity(getEntityId());
	}
}