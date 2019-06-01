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
import java.util.Optional;
import java.util.function.Predicate;

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
			model = PMMLUtil.findModel(pmml, (Model object) -> Objects.equals(object.getModelName(), modelName), "<Model>@modelName=" + modelName);
		} else

		{
			model = PMMLUtil.findModel(pmml, (Model object) -> object.isScorable(), "<Model>@isScorable=true");
		}

		return model;
	}

	static
	public <M extends Model> M findModel(PMML pmml, Class<? extends M> clazz){
		Model model = findModel(pmml, (Model object) -> clazz.isInstance(object) && object.isScorable(), XPathUtil.formatElement(clazz) + "@isScorable=true");

		return clazz.cast(model);
	}

	static
	public Model findModel(PMML pmml, Predicate<Model> predicate, String predicateXPath){

		if(!pmml.hasModels()){
			throw new MissingElementException(MissingElementException.formatMessage(XPathUtil.formatElement(pmml.getClass()) + "/" + predicateXPath), pmml);
		}

		List<Model> models = pmml.getModels();

		Optional<Model> result = models.stream()
			.filter(predicate)
			.findAny();

		if(!result.isPresent()){
			throw new MissingElementException(MissingElementException.formatMessage(XPathUtil.formatElement(pmml.getClass()) + "/" + predicateXPath), pmml);
		}

		return result.get();
	}
}