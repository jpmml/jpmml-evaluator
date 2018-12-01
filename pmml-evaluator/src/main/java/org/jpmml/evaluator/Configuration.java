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
import java.util.Objects;

public class Configuration implements Serializable {

	private ModelEvaluatorFactory modelEvaluatorFactory = null;

	private ValueFactoryFactory valueFactoryFactory = null;


	public Configuration(){
		this(ModelEvaluatorFactory.newInstance());
	}

	public Configuration(ModelEvaluatorFactory modelEvaluatorFactory){
		this(modelEvaluatorFactory, ValueFactoryFactory.newInstance());
	}

	public Configuration(ModelEvaluatorFactory modelEvaluatorFactory, ValueFactoryFactory valueFactoryFactory){
		setModelEvaluatorFactory(Objects.requireNonNull(modelEvaluatorFactory));
		setValueFactoryFactory(Objects.requireNonNull(valueFactoryFactory));
	}

	public ModelEvaluatorFactory getModelEvaluatorFactory(){
		return this.modelEvaluatorFactory;
	}

	private void setModelEvaluatorFactory(ModelEvaluatorFactory modelEvaluatorFactory){
		this.modelEvaluatorFactory = modelEvaluatorFactory;
	}

	public ValueFactoryFactory getValueFactoryFactory(){
		return this.valueFactoryFactory;
	}

	private void setValueFactoryFactory(ValueFactoryFactory valueFactoryFactory){
		this.valueFactoryFactory = valueFactoryFactory;
	}

	static
	public Configuration getInstance(){
		return Configuration.INSTANCE;
	}

	private static final Configuration INSTANCE = new Configuration();
}