/*
 * Copyright (c) 2021 Villu Ruusmann
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

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DefineFunction;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.PMML;
import org.dmg.pmml.TransformationDictionary;

abstract
public class PMMLManager implements HasPMML, Serializable {

	private PMML pmml = null;

	private Map<String, DataField> dataFields = Collections.emptyMap();

	private Map<String, DerivedField> derivedFields = Collections.emptyMap();

	private Map<String, DefineFunction> defineFunctions = Collections.emptyMap();


	protected PMMLManager(){
	}

	protected PMMLManager(PMML pmml){
		setPMML(pmml);

		DataDictionary dataDictionary = pmml.requireDataDictionary();
		if(dataDictionary.hasDataFields()){
			this.dataFields = ImmutableMap.copyOf(IndexableUtil.buildMap(dataDictionary.getDataFields()));
		}

		TransformationDictionary transformationDictionary = pmml.getTransformationDictionary();
		if(transformationDictionary != null && transformationDictionary.hasDerivedFields()){
			this.derivedFields = ImmutableMap.copyOf(IndexableUtil.buildMap(transformationDictionary.getDerivedFields()));
		} // End if

		if(transformationDictionary != null && transformationDictionary.hasDefineFunctions()){
			this.defineFunctions = ImmutableMap.copyOf(IndexableUtil.buildMap(transformationDictionary.getDefineFunctions()));
		}
	}

	public DataField getDataField(String name){
		return this.dataFields.get(name);
	}

	public DerivedField getDerivedField(String name){
		return this.derivedFields.get(name);
	}

	public DefineFunction getDefineFunction(String name){
		return this.defineFunctions.get(name);
	}

	@Override
	public PMML getPMML(){
		return this.pmml;
	}

	private void setPMML(PMML pmml){
		this.pmml = Objects.requireNonNull(pmml);
	}

	static
	protected <K, V> Map<K, ? extends List<V>> toImmutableListMap(Map<K, List<V>> map){
		Function<List<V>, ImmutableList<V>> function = new Function<List<V>, ImmutableList<V>>(){

			@Override
			public ImmutableList<V> apply(List<V> list){
				return ImmutableList.copyOf(list);
			}
		};

		return Maps.transformValues(map, function);
	}

	static
	protected <K, V> Map<K, ? extends Set<V>> toImmutableSetMap(Map<K, Set<V>> map){
		Function<Set<V>, ImmutableSet<V>> function = new Function<Set<V>, ImmutableSet<V>>(){

			@Override
			public ImmutableSet<V> apply(Set<V> set){

				if(set instanceof EnumSet){
					EnumSet<?> enumSet = (EnumSet<?>)set;

					return (ImmutableSet)Sets.immutableEnumSet(enumSet);
				}

				return ImmutableSet.copyOf(set);
			}
		};

		return Maps.transformValues(map, function);
	}

	static
	protected <K1, K2, V2> Map<K1, ? extends Map<K2, V2>> toImmutableMapMap(Map<K1, Map<K2, V2>> map){
		Function<Map<K2, V2>, ImmutableMap<K2, V2>> function = new Function<Map<K2, V2>, ImmutableMap<K2, V2>>(){

			@Override
			public ImmutableMap<K2, V2> apply(Map<K2, V2> map){
				return ImmutableMap.copyOf(map);
			}
		};

		return Maps.transformValues(map, function);
	}
}