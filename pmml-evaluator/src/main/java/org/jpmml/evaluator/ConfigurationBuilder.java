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

public class ConfigurationBuilder implements Cloneable, Serializable {

	private ModelEvaluatorFactory modelEvaluatorFactory = null;

	private ValueFactoryFactory valueFactoryFactory = null;

	private OutputFilter outputFilter = null;


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

		OutputFilter outputFilter = getOutputFilter();
		if(outputFilter == null){
			outputFilter = OutputFilters.KEEP_ALL;
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

	public OutputFilter getOutputFilter(){
		return this.outputFilter;
	}

	public ConfigurationBuilder setOutputFilter(OutputFilter outputFilter){
		this.outputFilter = outputFilter;

		return this;
	}
}