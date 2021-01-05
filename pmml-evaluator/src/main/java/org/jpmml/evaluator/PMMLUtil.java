/*
 * Copyright (c) 2018 Villu Ruusmann
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

import java.util.List;
import java.util.Objects;

import com.google.common.base.Predicate;
import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;
import org.jpmml.model.XPathUtil;

public class PMMLUtil {

	private PMMLUtil(){
	}

	static
	public Model findModel(PMML pmml, String modelName){
		Model model;

		if(modelName != null){
			Predicate<Model> predicate = new Predicate<Model>(){

				@Override
				public boolean apply(Model model){
					return Objects.equals(model.getModelName(), modelName);
				}
			};

			model = PMMLUtil.findModel(pmml, predicate, "<Model>@modelName=" + modelName);
		} else

		{
			Predicate<Model> predicate = new Predicate<Model>(){

				@Override
				public boolean apply(Model model){
					return model.isScorable();
				}
			};

			model = PMMLUtil.findModel(pmml, predicate, "<Model>@isScorable=true");
		}

		return model;
	}

	static
	public <M extends Model> M findModel(PMML pmml, Class<? extends M> clazz){
		Predicate<Model> predicate = new Predicate<Model>(){

			@Override
			public boolean apply(Model model){
				return clazz.isInstance(model) && model.isScorable();
			}
		};

		Model model = findModel(pmml, predicate, XPathUtil.formatElement(clazz) + "@isScorable=true");

		return clazz.cast(model);
	}

	static
	private Model findModel(PMML pmml, Predicate<Model> predicate, String predicateXPath){

		if(pmml.hasModels()){
			List<Model> models = pmml.getModels();

			for(Model model : models){

				if(predicate.apply(model)){
					return model;
				}
			}
		}

		throw new MissingElementException(MissingElementException.formatMessage(XPathUtil.formatElement(pmml.getClass()) + "/" + predicateXPath), pmml);
	}
}