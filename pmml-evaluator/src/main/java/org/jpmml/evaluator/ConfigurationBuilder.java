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

import java.io.Serializable;
import java.util.function.Predicate;

public class ConfigurationBuilder implements Cloneable, Serializable {

	private ModelEvaluatorFactory modelEvaluatorFactory = null;

	private ValueFactoryFactory valueFactoryFactory = null;

	private Predicate<org.dmg.pmml.OutputField> outputFilter = null;


	public ConfigurationBuilder(){
	}

	@Override
	public ConfigurationBuilder clone(){

		try {
			return (ConfigurationBuilder)super.clone();
		} catch(CloneNotSupportedException cnse){
			throw new InternalError(cnse);
		}
	}

	public Configuration build(){
		Configuration configuration = new Configuration();

		ModelEvaluatorFactory modelEvaluatorFactory = getModelEvaluatorFactory();
		if(modelEvaluatorFactory == null){
			modelEvaluatorFactory = ModelEvaluatorFactory.newInstance();
		}

		configuration.setModelEvaluatorFactory(modelEvaluatorFactory);

		ValueFactoryFactory valueFactoryFactory = getValueFactoryFactory();
		if(valueFactoryFactory == null){
			valueFactoryFactory = ValueFactoryFactory.newInstance();
		}

		configuration.setValueFactoryFactory(valueFactoryFactory);

		Predicate<org.dmg.pmml.OutputField> outputFilter = getOutputFilter();
		if(outputFilter == null){
			// Create a serializable lambda
			// See https://stackoverflow.com/a/22808112
			outputFilter = (Predicate<org.dmg.pmml.OutputField> & Serializable)(org.dmg.pmml.OutputField outputField) -> true;
		}

		configuration.setOutputFilter(outputFilter);

		return configuration;
	}

	public ModelEvaluatorFactory getModelEvaluatorFactory(){
		return this.modelEvaluatorFactory;
	}

	public ConfigurationBuilder setModelEvaluatorFactory(ModelEvaluatorFactory modelEvaluatorFactory){
		this.modelEvaluatorFactory = modelEvaluatorFactory;

		return this;
	}

	public ValueFactoryFactory getValueFactoryFactory(){
		return this.valueFactoryFactory;
	}

	public ConfigurationBuilder setValueFactoryFactory(ValueFactoryFactory valueFactoryFactory){
		this.valueFactoryFactory = valueFactoryFactory;

		return this;
	}

	public Predicate<org.dmg.pmml.OutputField> getOutputFilter(){
		return this.outputFilter;
	}

	public ConfigurationBuilder setOutputFilter(Predicate<org.dmg.pmml.OutputField> outputFilter){
		this.outputFilter = outputFilter;

		return this;
	}
}