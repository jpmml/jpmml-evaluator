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

import com.google.common.collect.Iterables;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;

public class ModelEvaluatorBuilder implements EvaluatorBuilder, Serializable {

	private PMML pmml = null;

	private Model model = null;

	private ModelEvaluatorFactory modelEvaluatorFactory = null;

	private ValueFactoryFactory valueFactoryFactory = null;

	private java.util.function.Function<FieldName, FieldName> inputMapper = null;

	private java.util.function.Function<FieldName, FieldName> resultMapper = null;


	ModelEvaluatorBuilder(){
	}

	/**
	 * <p>
	 * Selects the first scorable model.
	 * </p>
	 *
	 * @throw MissingElementException If the PMML does not contain any scorable models.
	 */
	public ModelEvaluatorBuilder(PMML pmml){
		this(pmml, (String)null);
	}

	/**
	 * <p>
	 * Selects the named model.
	 * </p>
	 *
	 * @throw MissingElementException If the PMML does not contain a model with the specified model name.
	 *
	 * @see Model#getModelName()
	 */
	public ModelEvaluatorBuilder(PMML pmml, String modelName){
		setPMML(Objects.requireNonNull(pmml));
		setModel(PMMLUtil.findModel(pmml, modelName));
	}

	public ModelEvaluatorBuilder(PMML pmml, Model model){
		setPMML(Objects.requireNonNull(pmml));
		setModel(Objects.requireNonNull(model));
	}

	@Override
	public ModelEvaluator<?> build(){
		PMML pmml = getPMML();
		Model model = getModel();

		if((pmml == null) || (model == null)){
			throw new IllegalStateException();
		}

		ModelEvaluatorFactory modelEvaluatorFactory = getModelEvaluatorFactory();
		if(modelEvaluatorFactory == null){
			modelEvaluatorFactory = ModelEvaluatorFactory.newInstance();
		}

		ValueFactoryFactory valueFactoryFactory = getValueFactoryFactory();
		if(valueFactoryFactory == null){
			valueFactoryFactory = ValueFactoryFactory.newInstance();
		}

		Configuration configuration = new Configuration(modelEvaluatorFactory, valueFactoryFactory);

		ModelEvaluator<?> modelEvaluator = modelEvaluatorFactory.newModelEvaluator(pmml, model);
		modelEvaluator.configure(configuration);

		java.util.function.Function<FieldName, FieldName> inputMapper = getInputMapper();
		java.util.function.Function<FieldName, FieldName> resultMapper = getResultMapper();

		if(inputMapper != null){
			Iterable<? extends InputField> inputFields = modelEvaluator.getInputFields();

			for(InputField inputField : inputFields){
				inputField.setName(inputMapper.apply(inputField.getName()));
			}
		} // End if

		if(resultMapper != null){
			Iterable<? extends ResultField> resultFields = Iterables.concat(modelEvaluator.getTargetFields(), modelEvaluator.getOutputFields());

			for(ResultField resultField : resultFields){
				resultField.setName(resultMapper.apply(resultField.getName()));
			}
		}

		return modelEvaluator;
	}

	public PMML getPMML(){
		return this.pmml;
	}

	ModelEvaluatorBuilder setPMML(PMML pmml){
		this.pmml = pmml;

		return this;
	}

	public Model getModel(){
		return this.model;
	}

	ModelEvaluatorBuilder setModel(Model model){
		this.model = model;

		return this;
	}

	public ModelEvaluatorFactory getModelEvaluatorFactory(){
		return this.modelEvaluatorFactory;
	}

	public ModelEvaluatorBuilder setModelEvaluatorFactory(ModelEvaluatorFactory modelEvaluatorFactory){
		this.modelEvaluatorFactory = modelEvaluatorFactory;

		return this;
	}

	public ValueFactoryFactory getValueFactoryFactory(){
		return this.valueFactoryFactory;
	}

	public ModelEvaluatorBuilder setValueFactoryFactory(ValueFactoryFactory valueFactoryFactory){
		this.valueFactoryFactory = valueFactoryFactory;

		return this;
	}

	public java.util.function.Function<FieldName, FieldName> getInputMapper(){
		return this.inputMapper;
	}

	public ModelEvaluatorBuilder setInputMapper(java.util.function.Function<FieldName, FieldName> inputMapper){
		this.inputMapper = inputMapper;

		return this;
	}

	public java.util.function.Function<FieldName, FieldName> getResultMapper(){
		return this.resultMapper;
	}

	public ModelEvaluatorBuilder setResultMapper(java.util.function.Function<FieldName, FieldName> resultMapper){
		this.resultMapper = resultMapper;

		return this;
	}
}