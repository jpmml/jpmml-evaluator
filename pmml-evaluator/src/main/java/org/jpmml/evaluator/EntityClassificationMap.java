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

import com.google.common.annotations.Beta;
import com.google.common.base.Objects.ToStringHelper;
import org.dmg.pmml.Entity;

@Beta
abstract
public class EntityClassificationMap<E extends Entity> extends ClassificationMap<String> implements HasEntityId {

	private E entity = null;

	private Double entityValue = null;


	protected EntityClassificationMap(Type type){
		super(type);
	}

	protected EntityClassificationMap(Type type, E entity){
		super(type);

		setEntity(entity);
	}

	@Override
	public String getEntityId(){
		E entity = getEntity();

		if(entity != null){
			return entity.getId();
		}

		return null;
	}

	Double put(E entity, String key, Double value){
		Type type = getType();

		if(this.entityValue == null || type.compare(value, this.entityValue) > 0){
			this.entityValue = value;

			setEntity(entity);
		}

		return super.put(key, value);
	}

	@Override
	protected ToStringHelper toStringHelper(){
		ToStringHelper helper = super.toStringHelper()
			.add("entityId", getEntityId());

		return helper;
	}

	public E getEntity(){
		return this.entity;
	}

	void setEntity(E entity){
		this.entity = entity;
	}
}