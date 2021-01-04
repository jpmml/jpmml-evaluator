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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import org.dmg.pmml.Cell;
import org.dmg.pmml.Extension;
import org.dmg.pmml.HasTable;
import org.dmg.pmml.InlineTable;
import org.dmg.pmml.Namespaces;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.Row;
import org.dmg.pmml.TableLocator;
import org.w3c.dom.Element;

public class InlineTableUtil {

	private InlineTableUtil(){
	}

	static
	public <E extends PMMLObject & HasTable<E>> InlineTable getInlineTable(E object){
		InlineTable inlineTable = object.getInlineTable();

		TableLocator tableLocator = object.getTableLocator();
		if(tableLocator != null){
			throw new UnsupportedElementException(tableLocator);
		}

		return inlineTable;
	}

	static
	public Table<Integer, String, Object> getContent(InlineTable inlineTable){
		return CacheUtil.getValue(inlineTable, InlineTableUtil.contentCache);
	}

	static
	Table<Integer, String, Object> parse(InlineTable inlineTable){
		Table<Integer, String, Object> result = HashBasedTable.create();

		Integer rowKey = 1;

		List<Row> rows = inlineTable.getRows();
		for(Row row : rows){
			List<Object> cells = row.getContent();

			for(Object cell : cells){
				String column;
				Object value;

				if(cell instanceof Cell){
					Cell pmmlCell = (Cell)cell;

					column = parseColumn(pmmlCell.getName());
					value = pmmlCell.getValue();
				} else

				if(cell instanceof Extension){
					continue;
				} else

				if(cell instanceof PMMLObject){
					PMMLObject object = (PMMLObject)cell;

					throw new MisplacedElementException(object);
				} else

				if(cell instanceof JAXBElement){
					JAXBElement<?> jaxbElement = (JAXBElement<?>)cell;

					column = parseColumn(jaxbElement.getName());
					value = jaxbElement.getValue();
				} else

				if(cell instanceof Element){
					Element domElement = (Element)cell;

					column = domElement.getTagName();
					value = domElement.getTextContent();
				} else

				if(cell instanceof String){
					String string = (String)cell;

					if(("").equals(string.trim())){
						continue;
					}

					throw new InvalidElementException(row);
				} else

				{
					throw new InvalidElementException(row);
				}

				result.put(rowKey, column, value);
			}

			rowKey += 1;
		}

		return result;
	}

	static
	private String parseColumn(QName xmlName){
		String prefix = xmlName.getPrefix();
		String localPart = xmlName.getLocalPart();

		if(prefix != null && !("").equals(prefix)){
			return prefix + ":" + localPart;
		} else

		{
			return localPart;
		}
	}

	static
	InlineTable format(Table<Integer, String, String> table){
		InlineTable result = new InlineTable();

		Map<Integer, Map<String, String>> tableRows = table.rowMap();

		int minRow = Collections.min(tableRows.keySet());
		int maxRow = Collections.max(tableRows.keySet());

		if(minRow != 1 || maxRow != tableRows.size()){
			throw new IllegalArgumentException();
		}

		for(int i = minRow; i <= maxRow; i++){
			Map<String, String> tableRow = tableRows.get(i);

			Row row = new Row();

			Collection<String> columns = tableRow.keySet();
			for(String column : columns){
				String value = tableRow.get(column);

				if(value == null){
					continue;
				}

				QName xmlName;

				if(column.startsWith("data:")){
					xmlName = new QName(Namespaces.JPMML_INLINETABLE, column.substring("data:".length()), "data");
				} else

				{
					xmlName = new QName(Namespaces.PMML_LATEST, column);
				}

				JAXBElement<String> cell = new JAXBElement<>(xmlName, String.class, value);

				row.addContent(cell);
			}

			result.addRows(row);
		}

		return result;
	}

	private static final LoadingCache<InlineTable, Table<Integer, String, Object>> contentCache = CacheUtil.buildLoadingCache(new CacheLoader<InlineTable, Table<Integer, String, Object>>(){

		@Override
		public Table<Integer, String, Object> load(InlineTable inlineTable){
			return Tables.unmodifiableTable(parse(inlineTable));
		}
	});
}