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
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;

public class ModelEvaluatorBuilder implements EvaluatorBuilder, Serializable {

	private PMML pmml = null;

	private Model model = null;

	private ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

	private InputMapper inputMapper = null;

	private ResultMapper resultMapper = null;


	protected ModelEvaluatorBuilder(){
	}

	/**
	 * <p>
	 * Selects the first scorable model.
	 * </p>
	 *
	 * @throws MissingElementException If the PMML does not contain any scorable models.
	 */
	public ModelEvaluatorBuilder(PMML pmml){
		this(pmml, (String)null);
	}

	/**
	 * <p>
	 * Selects the named model.
	 * </p>
	 *
	 * @throws MissingElementException If the PMML does not contain a model with the specified model name.
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
	public ModelEvaluatorBuilder clone(){
		ModelEvaluatorBuilder modelEvaluatorBuilder;

		try {
			modelEvaluatorBuilder = (ModelEvaluatorBuilder)super.clone();
		} catch(CloneNotSupportedException cnse){
			throw new InternalError(cnse);
		}

		modelEvaluatorBuilder.configurationBuilder = modelEvaluatorBuilder.configurationBuilder.clone();

		return modelEvaluatorBuilder;
	}

	@Override
	public ModelEvaluator<?> build(){
		PMML pmml = getPMML();
		Model model = getModel();

		if((pmml == null) || (model == null)){
			throw new IllegalStateException();
		}

		ConfigurationBuilder configurationBuilder = getConfigurationBuilder();

		Configuration configuration = configurationBuilder.build();

		ModelEvaluatorFactory modelEvaluatorFactory = configuration.getModelEvaluatorFactory();

		ModelEvaluator<?> modelEvaluator = modelEvaluatorFactory.newModelEvaluator(pmml, model);
		modelEvaluator.configure(configuration);

		InputMapper inputMapper = getInputMapper();
		ResultMapper resultMapper = getResultMapper();

		modelEvaluator.setInputMapper(inputMapper);
		modelEvaluator.setResultMapper(resultMapper);

		checkSchema(modelEvaluator);

		return modelEvaluator;
	}

	protected void checkSchema(ModelEvaluator<?> modelEvaluator){
		Model model = modelEvaluator.getModel();

		MiningSchema miningSchema = model.getMiningSchema();

		List<InputField> inputFields = modelEvaluator.getInputFields();
		List<InputField> groupFields = Collections.emptyList();

		if(modelEvaluator instanceof HasGroupFields){
			HasGroupFields hasGroupFields = (HasGroupFields)modelEvaluator;

			groupFields = hasGroupFields.getGroupFields();
		} // End if

		if((inputFields.size() + groupFields.size()) > 1000){
			throw new InvalidElementException("Model has too many input fields", miningSchema);
		}

		List<TargetField> targetFields = modelEvaluator.getTargetFields();
		List<OutputField> outputFields = modelEvaluator.getOutputFields();

		if((targetFields.size() + outputFields.size()) < 1){
			throw new InvalidElementException("Model does not have any target or output fields", miningSchema);
		}
	}

	public PMML getPMML(){
		return this.pmml;
	}

	protected ModelEvaluatorBuilder setPMML(PMML pmml){
		this.pmml = pmml;

		return this;
	}

	public Model getModel(){
		return this.model;
	}

	protected ModelEvaluatorBuilder setModel(Model model){
		this.model = model;

		return this;
	}

	public ConfigurationBuilder getConfigurationBuilder(){
		return this.configurationBuilder;
	}

	public ModelEvaluatorFactory getModelEvaluatorFactory(){
		ConfigurationBuilder configurationBuilder = getConfigurationBuilder();

		return configurationBuilder.getModelEvaluatorFactory();
	}

	public ModelEvaluatorBuilder setModelEvaluatorFactory(ModelEvaluatorFactory modelEvaluatorFactory){
		ConfigurationBuilder configurationBuilder = getConfigurationBuilder();

		configurationBuilder.setModelEvaluatorFactory(modelEvaluatorFactory);

		return this;
	}

	public ValueFactoryFactory getValueFactoryFactory(){
		ConfigurationBuilder configurationBuilder = getConfigurationBuilder();

		return configurationBuilder.getValueFactoryFactory();
	}

	public ModelEvaluatorBuilder setValueFactoryFactory(ValueFactoryFactory valueFactoryFactory){
		ConfigurationBuilder configurationBuilder = getConfigurationBuilder();

		configurationBuilder.setValueFactoryFactory(valueFactoryFactory);

		return this;
	}

	public OutputFilter getOutputFilter(){
		ConfigurationBuilder configurationBuilder = getConfigurationBuilder();

		return configurationBuilder.getOutputFilter();
	}

	public ModelEvaluatorBuilder setOutputFilter(OutputFilter outputFilter){
		ConfigurationBuilder configurationBuilder = getConfigurationBuilder();

		configurationBuilder.setOutputFilter(outputFilter);

		return this;
	}

	public SymbolTable<FieldName> getDerivedFieldGuard(){
		ConfigurationBuilder configurationBuilder = getConfigurationBuilder();

		return configurationBuilder.getDerivedFieldGuard();
	}

	public ModelEvaluatorBuilder setDerivedFieldGuard(SymbolTable<FieldName> derivedFieldGuard){
		ConfigurationBuilder configurationBuilder = getConfigurationBuilder();

		configurationBuilder.setDerivedFieldGuard(derivedFieldGuard);

		return this;
	}

	public SymbolTable<String> getFunctionGuard(){
		ConfigurationBuilder configurationBuilder = getConfigurationBuilder();

		return configurationBuilder.getFunctionGuard();
	}

	public ModelEvaluatorBuilder setFunctionGuard(SymbolTable<String> functionGuard){
		ConfigurationBuilder configurationBuilder = getConfigurationBuilder();

		configurationBuilder.setFunctionGuard(functionGuard);

		return this;
	}

	public InputMapper getInputMapper(){
		return this.inputMapper;
	}

	public ModelEvaluatorBuilder setInputMapper(InputMapper inputMapper){
		this.inputMapper = inputMapper;

		return this;
	}

	public ResultMapper getResultMapper(){
		return this.resultMapper;
	}

	public ModelEvaluatorBuilder setResultMapper(ResultMapper resultMapper){
		this.resultMapper = resultMapper;

		return this;
	}
}