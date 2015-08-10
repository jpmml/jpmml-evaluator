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

import java.util.AbstractList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSortedMap;
import org.dmg.pmml.SparseArray;

public class SparseArrayUtil {

	private SparseArrayUtil(){
	}

	@SuppressWarnings (
		value = {"unchecked"}
	)
	static
	public <E extends Number> SortedMap<Integer, E> getContent(SparseArray<E> sparseArray){
		return (SortedMap<Integer, E>)CacheUtil.getValue(sparseArray, SparseArrayUtil.contentCache);
	}

	static
	public <E extends Number> List<E> asNumberList(SparseArray<E> sparseArray){
		final
		SortedMap<Integer, E> content = getContent(sparseArray);

		final
		int size;

		Integer n = sparseArray.getN();
		if(n != null){
			size = n.intValue();
		} else

		{
			size = content.size();
		}

		final
		E defaultValue = sparseArray.getDefaultValue();

		List<E> result = new AbstractList<E>(){

			@Override
			public int size(){
				return size;
			}

			@Override
			public E get(int index){
				E value = content.get(Integer.valueOf(index + 1));

				if(value == null){
					value = defaultValue;
				}

				return value;
			}
		};

		return result;
	}

	static
	public <E extends Number> SortedMap<Integer, E> parse(SparseArray<E> sparseArray){
		SortedMap<Integer, E> result = new TreeMap<>();

		if(!sparseArray.hasIndices() && !sparseArray.hasEntries()){
			return result;
		}

		List<Integer> indices = sparseArray.getIndices();
		List<E> entries = sparseArray.getEntries();

		// "Both arrays must have the same length"
		if(indices.size() != entries.size()){
			throw new InvalidFeatureException(sparseArray);
		}

		Integer n = sparseArray.getN();

		for(int i = 0; i < indices.size(); i++){
			Integer index = indices.get(i);
			E entry = entries.get(i);

			if((index < 1) || (n != null && index > n.intValue())){
				throw new InvalidFeatureException(sparseArray);
			}

			result.put(index, entry);
		}

		if(n != null && n.intValue() < result.size()){
			throw new InvalidFeatureException(sparseArray);
		}

		return result;
	}

	private static final LoadingCache<SparseArray<? extends Number>, SortedMap<Integer, ? extends Number>> contentCache = CacheUtil.buildLoadingCache(new CacheLoader<SparseArray<? extends Number>, SortedMap<Integer, ? extends Number>>(){

		@Override
		public SortedMap<Integer, ? extends Number> load(SparseArray<?> sparseArray){
			return ImmutableSortedMap.copyOf(parse(sparseArray));
		}
	});
}