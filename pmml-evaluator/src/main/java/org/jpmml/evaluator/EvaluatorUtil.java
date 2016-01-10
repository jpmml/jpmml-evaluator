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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OpType;

public class EvaluatorUtil {

	private EvaluatorUtil(){
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

			Collection<Object> decodedValues = createCollection(rawValues);

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
	public Map<String, ?> decode(Map<FieldName, ?> map){
		Map<String, Object> result = new LinkedHashMap<>();

		Collection<? extends Map.Entry<FieldName, ?>> entries = map.entrySet();
		for(Map.Entry<FieldName, ?> entry : entries){
			FieldName name = entry.getKey();
			Object value = entry.getValue();

			try {
				result.put(name != null ? name.getValue() : null, decode(value));
			} catch(UnsupportedOperationException uoe){
				// Ignored
			}
		}

		return result;
	}

	static
	public FieldValue prepare(Evaluator evaluator, FieldName name, Object value){

		if(value instanceof Collection){
			Collection<?> rawValues = (Collection<?>)value;

			Collection<Object> preparedValues = createCollection(rawValues);

			DataType dataType = null;

			OpType opType = null;

			for(Object rawValue : rawValues){
				FieldValue preparedValue = evaluator.prepare(name, rawValue);

				if(preparedValue != null){

					if(dataType == null){
						dataType = preparedValue.getDataType();
					} // End if

					if(opType == null){
						opType = preparedValue.getOpType();
					}
				}

				preparedValues.add(FieldValueUtil.getValue(preparedValue));
			}

			return FieldValueUtil.create(dataType, opType, preparedValues);
		}

		return evaluator.prepare(name, value);
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

		List<Map<K, Object>> resultTable = new ArrayList<>();

		Collection<Map.Entry<Object, ListMultimap<K, Object>>> entries = groupedRows.entrySet();
		for(Map.Entry<Object, ListMultimap<K, Object>> entry : entries){
			Map<K, Object> resultRow = new LinkedHashMap<>();
			resultRow.putAll((entry.getValue()).asMap());

			// The value of the "group by" column is a single Object, not a Collection (ie. java.util.List) of Objects
			resultRow.put(groupKey, entry.getKey());

			resultTable.add(resultRow);
		}

		return resultTable;
	}

	static
	public List<FieldName> getTargetFields(Evaluator evaluator){
		List<FieldName> targetFields = evaluator.getTargetFields();

		if(targetFields.isEmpty()){
			FieldName targetField = evaluator.getTargetField();

			DataField dataField = evaluator.getDataField(targetField);
			if(dataField != null){
				return Collections.singletonList(targetField);
			}
		}

		return targetFields;
	}

	static
	private Collection<Object> createCollection(Collection<?> template){

		// Try to preserve the original contract
		if(template instanceof Set){
			return new LinkedHashSet<>(template.size());
		}

		return new ArrayList<>(template.size());
	}
}