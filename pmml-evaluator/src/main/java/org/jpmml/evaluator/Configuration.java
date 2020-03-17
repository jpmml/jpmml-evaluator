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

import org.dmg.pmml.FieldName;

/**
 * @see ConfigurationBuilder
 */
public class Configuration implements Serializable {

	private ModelEvaluatorFactory modelEvaluatorFactory = null;

	private ValueFactoryFactory valueFactoryFactory = null;

	private OutputFilter outputFilter = null;

	private SymbolTable<FieldName> derivedFieldGuard = null;

	private SymbolTable<String> functionGuard = null;


	Configuration(){
	}

	public ModelEvaluatorFactory getModelEvaluatorFactory(){
		return this.modelEvaluatorFactory;
	}

	void setModelEvaluatorFactory(ModelEvaluatorFactory modelEvaluatorFactory){
		this.modelEvaluatorFactory = modelEvaluatorFactory;
	}

	public ValueFactoryFactory getValueFactoryFactory(){
		return this.valueFactoryFactory;
	}

	void setValueFactoryFactory(ValueFactoryFactory valueFactoryFactory){
		this.valueFactoryFactory = valueFactoryFactory;
	}

	public OutputFilter getOutputFilter(){
		return this.outputFilter;
	}

	void setOutputFilter(OutputFilter outputFilter){
		this.outputFilter = outputFilter;
	}

	public SymbolTable<FieldName> getDerivedFieldGuard(){
		return this.derivedFieldGuard;
	}

	void setDerivedFieldGuard(SymbolTable<FieldName> derivedFieldGuard){
		this.derivedFieldGuard = derivedFieldGuard;
	}

	public SymbolTable<String> getFunctionGuard(){
		return this.functionGuard;
	}

	void setFunctionGuard(SymbolTable<String> functionGuard){
		this.functionGuard = functionGuard;
	}
}