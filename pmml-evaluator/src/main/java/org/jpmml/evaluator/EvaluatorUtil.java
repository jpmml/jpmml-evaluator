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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;

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
	 * Decouples a {@link Map} instance from the current runtime environment by decoding its values.
	 * </p>
	 */
	static
	public Map<String, ?> decodeAll(Map<String, ?> map){

		if(map.size() == 1){
			Collection<? extends Map.Entry<String, ?>> entries = map.entrySet();

			Map.Entry<String, ?> entry = Iterables.getOnlyElement(entries);

			String name = entry.getKey();
			Object value = entry.getValue();

			Object decodedValue;

			try {
				decodedValue = decode(value);
			} catch(UnsupportedOperationException uoe){
				return Collections.emptyMap();
			}

			return Collections.singletonMap(name, decodedValue);
		} else

		{
			@SuppressWarnings({"rawtypes", "unchecked"})
			Map<String, Object> result = (Map)MapUtil.ensureLinkedHashMap(map);

			Collection<? extends Map.Entry<String, Object>> entries = result.entrySet();

			for(Iterator<? extends Map.Entry<String, Object>> it = entries.iterator(); it.hasNext(); ){
				Map.Entry<String, Object> entry = it.next();

				String name = entry.getKey();
				Object value = entry.getValue();

				Object decodedValue;

				try {
					decodedValue = decode(value);
				} catch(UnsupportedOperationException uoe){
					it.remove();

					continue;
				}

				if(decodedValue != value){
					entry.setValue(decodedValue);
				}
			}

			return result;
		}
	}

	static
	public Table groupRows(HasGroupFields hasGroupFields, Table table){
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
	public Table groupRows(String groupColumn, Table table){
		Map<Object, ListMultimap<String, Object>> groupedRows = new LinkedHashMap<>();

		Table.Row row = table.createReaderRow(0);

		for(int i = 0, max = table.getNumberOfRows(); i < max; i++){
			Object groupValue = row.get(groupColumn);

			ListMultimap<String, Object> groupedRow = groupedRows.get(groupValue);
			if(groupedRow == null){
				groupedRow = ArrayListMultimap.create();

				groupedRows.put(groupValue, groupedRow);
			}

			Collection<? extends Map.Entry<String, ?>> entries = row.entrySet();
			for(Map.Entry<String, ?> entry : entries){
				String column = entry.getKey();
				Object value = entry.getValue();

				if(value != null){
					groupedRow.put(column, value);
				}
			}

			row.advance();
		}

		Table groupedTable = new Table(groupedRows.size());

		row = groupedTable.createWriterRow(0);

		Collection<Map.Entry<Object, ListMultimap<String, Object>>> entries = groupedRows.entrySet();
		for(Map.Entry<Object, ListMultimap<String, Object>> entry : entries){
			Object groupValue = entry.getKey();
			ListMultimap<String, Object> groupedRow = entry.getValue();

			// Inserts all values as Lists
			row.putAll(Multimaps.asMap(groupedRow));

			// Re-insert the value of the "group by" column as a scalar (instead of a singleton List of a scalar)
			row.put(groupColumn, groupValue);

			row.advance();
		}

		return groupedTable;
	}
}