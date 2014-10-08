/*
 * Copyright (c) 2013 KNIME.com AG, Zurich, Switzerland
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jpmml.evaluator;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import org.dmg.pmml.IntSparseArray;
import org.dmg.pmml.RealSparseArray;
import org.dmg.pmml.SparseArray;
import org.jpmml.manager.InvalidFeatureException;
import org.jpmml.manager.UnsupportedFeatureException;

public class SparseArrayUtil {

	private SparseArrayUtil(){
	}

	static
	public <E extends Number> int getSize(SparseArray<E> sparseArray){
		Integer n = sparseArray.getN();
		if(n != null){
			return n.intValue();
		}

		SortedMap<Integer, E> content = getContent(sparseArray);

		return content.size();
	}

	@SuppressWarnings (
		value = {"unchecked"}
	)
	static
	public <E extends Number> SortedMap<Integer, E> getContent(SparseArray<E> sparseArray){
		return (SortedMap<Integer, E>)CacheUtil.getValue(sparseArray, SparseArrayUtil.contentCache);
	}

	static
	public <E extends Number> double[] toArray(SparseArray<E> sparseArray){
		int size = getSize(sparseArray);

		double[] result = new double[size];

		for(int i = 0; i < size; i++){
			Number value = getValue(sparseArray, Integer.valueOf(i + 1));

			result[i] = value.doubleValue();
		}

		return result;
	}

	static
	public <E extends Number> SortedMap<Integer, E> parse(SparseArray<E> sparseArray){
		SortedMap<Integer, E> result = Maps.newTreeMap();

		List<Integer> indices = sparseArray.getIndices();
		List<E> entries = sparseArray.getEntries();

		// "Both arrays must have the same length"
		if(indices.size() != entries.size()){
			throw new InvalidFeatureException(sparseArray);
		}

		for(int i = 0; i < indices.size(); i++){
			Integer index = indices.get(i);
			E entry = entries.get(i);

			checkIndex(sparseArray, index);

			result.put(index, entry);
		}

		Integer n = sparseArray.getN();
		if(n != null && n.intValue() < result.size()){
			throw new InvalidFeatureException(sparseArray);
		}

		return result;
	}

	static
	public <E extends Number> E getValue(SparseArray<E> sparseArray, Integer index){

		if(sparseArray instanceof IntSparseArray){
			return (E)getIntValue((IntSparseArray)sparseArray, index);
		} else

		if(sparseArray instanceof RealSparseArray){
			return (E)getRealValue((RealSparseArray)sparseArray, index);
		}

		throw new UnsupportedFeatureException(sparseArray);
	}

	static
	public Integer getIntValue(IntSparseArray sparseArray, Integer index){
		Map<Integer, Integer> content = getContent(sparseArray);

		Integer result = content.get(index);
		if(result == null){
			checkIndex(sparseArray, index);

			return sparseArray.getDefaultValue();
		}

		return result;
	}

	static
	public Double getRealValue(RealSparseArray sparseArray, Integer index){
		Map<Integer, Double> content = getContent(sparseArray);

		Double result = content.get(index);
		if(result == null){
			checkIndex(sparseArray, index);

			return sparseArray.getDefaultValue();
		}

		return result;
	}

	static
	private <E extends Number> void checkIndex(SparseArray<E> sparseArray, Integer index){
		Integer n = sparseArray.getN();

		if(index < 1 || (n != null && index > n)){
			throw new EvaluationException();
		}
	}

	private static final LoadingCache<SparseArray<?>, SortedMap<Integer, ? extends Number>> contentCache = CacheBuilder.newBuilder()
		.weakKeys()
		.build(new CacheLoader<SparseArray<?>, SortedMap<Integer, ? extends Number>>(){

			@Override
			public SortedMap<Integer, ? extends Number> load(SparseArray<?> sparseArray){
				return ImmutableSortedMap.copyOf(parse(sparseArray));
			}
		});
}