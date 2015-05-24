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

import java.io.InputStream;
import java.util.Map;

import com.google.common.collect.Maps;
import org.dmg.pmml.FieldName;

abstract
public class PMMLManagerTest {

	public PMMLManager createPMMLManager() throws Exception {
		return createPMMLManager(getClass());
	}

	static
	public PMMLManager createPMMLManager(Class<? extends PMMLManagerTest> clazz) throws Exception {
		InputStream is = getInputStream(clazz);

		try {
			return PMMLUtil.createPMMLManager(is);
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
	public InputStream getInputStream(Class<? extends PMMLManagerTest> clazz){
		return clazz.getResourceAsStream("/pmml/" + clazz.getSimpleName() + ".pmml");
	}
}