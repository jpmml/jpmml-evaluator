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

import java.io.*;
import java.util.*;

import javax.xml.transform.*;

import org.jpmml.model.*;

import org.dmg.pmml.*;

import com.google.common.collect.*;

import org.xml.sax.*;

abstract
public class PMMLTest {

	static
	public PMML loadPMML(Class<? extends PMMLTest> clazz) throws Exception {
		InputStream is = clazz.getResourceAsStream("/pmml/" + clazz.getSimpleName() + ".pmml");

		try {
			Source source = SchemaUtil.createImportSource(new InputSource(is));

			return JAXBUtil.unmarshalPMML(source);
		} finally {
			is.close();
		}
	}

	static
	public Map<FieldName, ?> createArguments(Object... objects){
		Map<FieldName, Object> result = Maps.newLinkedHashMap();

		if(objects.length % 2 != 0){
			throw new IllegalArgumentException();
		}

		for(int i = 0; i < objects.length / 2; i++){
			Object key = objects[i * 2];
			Object value = objects[i * 2 + 1];

			result.put(toFieldName(key), value);
		}

		return result;
	}

	static
	private FieldName toFieldName(Object object){

		if(object instanceof String){
			String string = (String)object;

			return FieldName.create(string);
		}

		return (FieldName)object;
	}

	static
	public String getEntityId(Object object){

		if(object instanceof HasEntityId){
			HasEntityId hasEntityId = (HasEntityId)object;

			return hasEntityId.getEntityId();
		}

		return null;
	}
}