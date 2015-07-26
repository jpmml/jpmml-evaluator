/*
 * Copyright (c) 2015 Villu Ruusmann
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.dmg.pmml.Indexable;
import org.dmg.pmml.PMMLObject;

public class IndexableUtil {

	private IndexableUtil(){
	}

	static
	public <K, E extends PMMLObject & Indexable<K>> E find(List<E> elements, K key){

		for(E element : elements){

			if((key).equals(ensureKey(element))){
				return element;
			}
		}

		return null;
	}

	static
	public <K, E extends PMMLObject & Indexable<K>> ImmutableMap<K, E> buildMap(List<E> elements){
		Map<K, E> result = new LinkedHashMap<>();

		for(E element : elements){
			K key = ensureKey(element);

			if(result.containsKey(key)){
				throw new InvalidFeatureException(element);
			}

			result.put(key, element);
		}

		return ImmutableMap.copyOf(result);
	}

	static
	<K, E extends PMMLObject & Indexable<K>> K ensureKey(E element){
		K key = element.getKey();

		if(key == null){
			throw new InvalidFeatureException(element);
		}

		return key;
	}
}