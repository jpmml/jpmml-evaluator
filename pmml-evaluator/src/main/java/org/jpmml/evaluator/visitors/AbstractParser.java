/*
 * Copyright (c) 2020 Villu Ruusmann
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
package org.jpmml.evaluator.visitors;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.dmg.pmml.DataType;
import org.dmg.pmml.Field;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.LocalTransformations;
import org.dmg.pmml.Model;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.TransformationDictionary;
import org.jpmml.evaluator.DuplicateFieldException;
import org.jpmml.evaluator.MissingFieldException;
import org.jpmml.model.visitors.FieldResolver;

abstract
class AbstractParser extends FieldResolver {

	private Map<FieldName, DataType> dataTypes = new HashMap<>();


	@Override
	public void reset(){
		super.reset();

		this.dataTypes.clear();
	}

	@Override
	public PMMLObject popParent(){
		PMMLObject parent = super.popParent();

		if(parent instanceof Model){
			this.dataTypes.clear();
		} else

		if(parent instanceof TransformationDictionary){
			this.dataTypes.clear();
		} else

		if(parent instanceof LocalTransformations){
			this.dataTypes.clear();
		}

		return parent;
	}

	protected DataType resolveDataType(FieldName name){
		DataType dataType = this.dataTypes.get(name);

		if(dataType == null && !this.dataTypes.containsKey(name)){
			Collection<Field<?>> fields = getFields();

			for(Field<?> field : fields){

				if((name).equals(field.getName())){

					if((dataType == null) || (dataType).equals(field.getDataType())){
						dataType = field.getDataType();
					} else

					// Two or more conflicting data types
					{
						dataType = null;

						break;
					}
				}
			}

			this.dataTypes.put(name, dataType);
		}

		return dataType;
	}

	protected DataType resolveTargetDataType(FieldName name){
		DataType dataType = this.dataTypes.get(name);

		if(dataType == null && !this.dataTypes.containsKey(name)){
			Collection<Field<?>> fields = getFields();

			for(Field<?> field : fields){

				if((name).equals(field.getName())){

					if(dataType != null){
						throw new DuplicateFieldException(name);
					}

					dataType = field.getDataType();
				}
			}

			if(dataType == null){
				throw new MissingFieldException(name);
			}

			this.dataTypes.put(name, dataType);
		}

		return dataType;
	}
}