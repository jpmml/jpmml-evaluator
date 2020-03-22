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
import org.dmg.pmml.DataType;
import org.dmg.pmml.clustering.Cluster;
import org.jpmml.evaluator.AffinityDistribution;
import org.jpmml.evaluator.EntityClassification;
import org.jpmml.evaluator.EntityUtil;
import org.jpmml.evaluator.HasAffinityRanking;
import org.jpmml.evaluator.HasDisplayValue;
import org.jpmml.evaluator.HasEntityAffinity;
import org.jpmml.evaluator.HasEntityIdRanking;
import org.jpmml.evaluator.Report;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueMap;

abstract
public class ClusterAffinityDistribution<V extends Number> extends EntityClassification<Cluster, String, V> implements HasEntityIdRanking, HasDisplayValue, HasAffinityRanking, HasEntityAffinity {

	ClusterAffinityDistribution(Type type, ValueMap<String, V> affinities){
		super(AffinityDistribution.validateType(type), affinities);
	}

	@Override
	protected void computeResult(DataType dataType){
		super.computeResult(dataType);
	}

	@Override
	public Set<String> getCategories(){
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
	public Double getAffinity(String category){
		return getValue(category);
	}

	@Override
	public Report getAffinityReport(String category){
		return getValueReport(category);
	}

	@Override
	public List<Double> getAffinityRanking(){
		return getWinnerValues();
	}

	@Override
	public Double getEntityAffinity(){
		return getAffinity(getEntityId());
	}

	public void put(Cluster entity, Value<V> value){
		BiMap<String, Cluster> entityRegistry = getEntityRegistry();

		String id = EntityUtil.getId(entity, entityRegistry);

		put(entity, id, value);
	}
}