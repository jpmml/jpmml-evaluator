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
package org.jpmml.evaluator.clustering;

import java.util.List;
import java.util.Set;

import com.google.common.collect.BiMap;
import org.dmg.pmml.clustering.Cluster;
import org.jpmml.evaluator.AffinityDistribution;
import org.jpmml.evaluator.EntityClassification;
import org.jpmml.evaluator.HasAffinityRanking;
import org.jpmml.evaluator.HasDisplayValue;
import org.jpmml.evaluator.HasEntityAffinity;
import org.jpmml.evaluator.HasEntityIdRanking;

public class ClusterAffinityDistribution extends EntityClassification<Cluster> implements HasEntityIdRanking, HasDisplayValue, HasAffinityRanking, HasEntityAffinity {

	ClusterAffinityDistribution(Type type, BiMap<String, Cluster> entityRegistry){
		super(AffinityDistribution.validateType(type), entityRegistry);
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