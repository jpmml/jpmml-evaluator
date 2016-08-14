/*
 * Copyright (c) 2016 Villu Ruusmann
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

import java.util.List;
import java.util.ListIterator;

import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.VisitorAction;
import org.jpmml.evaluator.MiningFieldUtil;
import org.jpmml.model.visitors.AbstractVisitor;

/**
 * <p>
 * A Visitor that interns {@link MiningField} elements.
 * </p>
 */
public class MiningFieldInterner extends AbstractVisitor {

	private ElementHashMap<MiningField> cache = new ElementHashMap<MiningField>(){

		@Override
		public ElementKey createKey(MiningField miningField){
			Object[] content = {miningField.getName()};

			return new ElementKey(content);
		}
	};


	@Override
	public VisitorAction visit(MiningSchema miningSchema){

		if(miningSchema.hasMiningFields()){
			List<MiningField> miningFields = miningSchema.getMiningFields();

			for(ListIterator<MiningField> it = miningFields.listIterator(); it.hasNext(); ){
				it.set(intern(it.next()));
			}
		}

		return super.visit(miningSchema);
	}

	private MiningField intern(MiningField miningField){

		if(miningField == null || miningField.hasExtensions()){
			return miningField;
		} // End if

		if(!MiningFieldUtil.isDefault(miningField)){
			return miningField;
		}

		return this.cache.intern(miningField);
	}
}