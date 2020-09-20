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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import org.dmg.pmml.FieldName;

public class EvaluatorUtil {

	private EvaluatorUtil(){
	}

	static
	public Map<FieldName, ?> encodeKeys(Map<String, ?> map){
		Map<FieldName, Object> result = new LinkedHashMap<>(2 * map.size());

		Collection<? extends Map.Entry<String, ?>> entries = map.entrySet();
		for(Map.Entry<String, ?> entry : entries){
			String name = entry.getKey();
			Object value = entry.getValue();

			result.put(name != null ? FieldName.create(name) : null, value);
		}

		return result;
	}

	/**
	 * @see Computable
	 */
	static
	public Object decode(Object object){

		if(object instanceof Computable){
			Computable computable = (Computable)object;

			return computable.getResult();
		} // End if

		if(object instanceof Collection){
			Collection<?> rawValues = (Collection<?>)object;

			Collection<Object> decodedValues;

			// Try to preserve the original contract
			if(rawValues instanceof Set){
				decodedValues = new LinkedHashSet<>(rawValues.size());
			} else

			{
				decodedValues = new ArrayList<>(rawValues.size());
			}

			for(Object rawValue : rawValues){
				decodedValues.add(decode(rawValue));
			}

			return decodedValues;
		}

		return object;
	}

	/**
	 * <p>
	 * Decouples a {@link Map} instance from the current runtime environment by decoding both its keys and values.
	 * </p>
	 */
	static
	public Map<String, ?> decodeAll(Map<FieldName, ?> map){
		Map<String, Object> result = new LinkedHashMap<>(2 * map.size());

		Collection<? extends Map.Entry<FieldName, ?>> entries = map.entrySet();
		for(Map.Entry<FieldName, ?> entry : entries){
			FieldName name = entry.getKey();
			Object value = entry.getValue();

			try {
				value = decode(value);
			} catch(UnsupportedOperationException uoe){
				continue;
			}

			result.put(name != null ? name.getValue() : null, value);
		}

		return result;
	}

	static
	public List<? extends Map<FieldName, ?>> groupRows(HasGroupFields hasGroupFields, List<? extends Map<FieldName, ?>> table){
		List<InputField> groupFields = hasGroupFields.getGroupFields();

		if(groupFields.size() == 1){
			InputField groupField = groupFields.get(0);

			table = EvaluatorUtil.groupRows(groupField.getName(), table);
		} else

		if(groupFields.size() > 1){
			ModelEvaluator<?> modelEvaluator = (ModelEvaluator<?>)hasGroupFields; // XXX

			throw modelEvaluator.createMiningSchemaException("Expected 0 or 1 group field(s), got " + groupFields.size()  + " group fields");
		}

		return table;
	}

	static
	public <K> List<Map<K, Object>> groupRows(K groupKey, List<? extends Map<K, ?>> table){
		Map<Object, ListMultimap<K, Object>> groupedRows = new LinkedHashMap<>();

		for(int i = 0; i < table.size(); i++){
			Map<K, ?> row = table.get(i);

			Object groupValue = row.get(groupKey);

			ListMultimap<K, Object> groupedRow = groupedRows.get(groupValue);
			if(groupedRow == null){
				groupedRow = ArrayListMultimap.create();

				groupedRows.put(groupValue, groupedRow);
			}

			Collection<? extends Map.Entry<K, ?>> entries = row.entrySet();
			for(Map.Entry<K, ?> entry : entries){
				K key = entry.getKey();
				Object value = entry.getValue();

				groupedRow.put(key, value);
			}
		}

		List<Map<K, Object>> resultTable = new ArrayList<>(groupedRows.size());

		Collection<Map.Entry<Object, ListMultimap<K, Object>>> entries = groupedRows.entrySet();
		for(Map.Entry<Object, ListMultimap<K, Object>> entry : entries){
			Map<K, Object> resultRow = new LinkedHashMap<>();
			resultRow.putAll(Multimaps.asMap(entry.getValue()));

			// The value of the "group by" column is a single Object, not a Collection (ie. java.util.List) of Objects
			resultRow.put(groupKey, entry.getKey());

			resultTable.add(resultRow);
		}

		return resultTable;
	}
}