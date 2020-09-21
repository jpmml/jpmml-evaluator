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
import java.util.Objects;

import org.dmg.pmml.Indexable;
import org.dmg.pmml.PMMLObject;

public class IndexableUtil {

	private IndexableUtil(){
	}

	static
	public <K, E extends PMMLObject & Indexable<K>> E findIndexable(List<E> elements, K key){
		return findIndexable(elements, key, false);
	}

	static
	public <K, E extends PMMLObject & Indexable<K>> E findIndexable(List<E> elements, K key, boolean nullable){

		for(E element : elements){

			if(Objects.equals(ensureKey(element, nullable), key)){
				return element;
			}
		}

		return null;
	}

	static
	public <K, E extends PMMLObject & Indexable<K>> Map<K, E> buildMap(List<E> elements){
		return buildMap(elements, false);
	}

	static
	public <K, E extends PMMLObject & Indexable<K>> Map<K, E> buildMap(List<E> elements, boolean nullable){
		Map<K, E> result = new LinkedHashMap<>();

		for(E element : elements){
			K key = ensureKey(element, nullable);

			if(result.containsKey(key)){
				throw new InvalidElementException(element);
			}

			result.put(key, element);
		}

		return result;
	}

	static
	private <K, E extends PMMLObject & Indexable<K>> K ensureKey(E element, boolean nullable){
		K key = element.getKey();

		if(key == null && !nullable){
			throw new InvalidElementException(element);
		}

		return key;
	}
}