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
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Model;
import org.dmg.pmml.Output;
import org.dmg.pmml.PMML;
import org.dmg.pmml.ResultFeature;
import org.jpmml.model.InvalidElementException;
import org.jpmml.model.MissingElementException;

public class ModelEvaluatorBuilder implements EvaluatorBuilder, Serializable {

	private PMML pmml = null;

	private Model model = null;

	private ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

	private Set<ResultFeature> extraResultFeatures = EnumSet.noneOf(ResultFeature.class);

	private boolean checkSchema = true;


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
		Model model = (pmml != null ? PMMLUtil.findModel(pmml, modelName) : null);

		setPMML(pmml);
		setModel(model);
	}

	public ModelEvaluatorBuilder(PMML pmml, Model model){
		setPMML(pmml);
		setModel(model);
	}

	@Override
	public ModelEvaluatorBuilder clone(){
		ModelEvaluatorBuilder modelEvaluatorBuilder;

		try {
			modelEvaluatorBuilder = (ModelEvaluatorBuilder)super.clone();
		} catch(CloneNotSupportedException cnse){
			throw (InternalError)new InternalError()
				.initCause(cnse);
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

		Set<ResultFeature> extraResultFeatures = getExtraResultFeatures();

		ModelEvaluator<?> modelEvaluator = modelEvaluatorFactory.newModelEvaluator(pmml, model, extraResultFeatures);
		modelEvaluator.configure(configuration);

		boolean checkSchema = getCheckSchema();
		if(checkSchema){
			checkSchema(modelEvaluator);
		}

		return modelEvaluator;
	}

	protected void checkSchema(ModelEvaluator<?> modelEvaluator){
		Model model = modelEvaluator.getModel();

		MiningSchema miningSchema = model.requireMiningSchema();

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
		this.pmml = Objects.requireNonNull(pmml);

		return this;
	}

	public Model getModel(){
		return this.model;
	}

	protected ModelEvaluatorBuilder setModel(Model model){
		this.model = Objects.requireNonNull(model);

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

	/**
	 * <p>
	 * Sets the filter for cleaning the model schema and model evaluation results from redundant output fields.
	 * </p>
	 *
	 * @see OutputFilters#KEEP_ALL
	 * @see OutputFilters#KEEP_FINAL_RESULTS
	 */
	public ModelEvaluatorBuilder setOutputFilter(OutputFilter outputFilter){
		ConfigurationBuilder configurationBuilder = getConfigurationBuilder();

		configurationBuilder.setOutputFilter(outputFilter);

		return this;
	}

	public SymbolTable<String> getDerivedFieldGuard(){
		ConfigurationBuilder configurationBuilder = getConfigurationBuilder();

		return configurationBuilder.getDerivedFieldGuard();
	}

	/**
	 * <p>
	 * Sets a guard against recursive field declarations.
	 * </p>
	 *
	 * @see FieldNameSet
	 */
	public ModelEvaluatorBuilder setDerivedFieldGuard(SymbolTable<String> derivedFieldGuard){
		ConfigurationBuilder configurationBuilder = getConfigurationBuilder();

		configurationBuilder.setDerivedFieldGuard(derivedFieldGuard);

		return this;
	}

	public SymbolTable<String> getFunctionGuard(){
		ConfigurationBuilder configurationBuilder = getConfigurationBuilder();

		return configurationBuilder.getFunctionGuard();
	}

	/**
	 * <p>
	 * Sets a guard against recursive function declarations.
	 * </p>
	 *
	 * @see FunctionNameStack
	 */
	public ModelEvaluatorBuilder setFunctionGuard(SymbolTable<String> functionGuard){
		ConfigurationBuilder configurationBuilder = getConfigurationBuilder();

		configurationBuilder.setFunctionGuard(functionGuard);

		return this;
	}

	public Set<ResultFeature> getExtraResultFeatures(){
		return this.extraResultFeatures;
	}

	/**
	 * <p>
	 * Sets <em>extra</em> functional requirements.
	 * </p>
	 *
	 * The final set of functional requirements is obtained by combining
	 * default functional requirements (as declared by the {@link Output} element of the model)
	 * with extra functional requirements.
	 */
	public ModelEvaluatorBuilder setExtraResultFeatures(Set<ResultFeature> extraResultFeatures){
		this.extraResultFeatures = extraResultFeatures;

		return this;
	}

	public boolean getCheckSchema(){
		return this.checkSchema;
	}

	/**
	 * <p>
	 * Should the "data schema" of models be checked for the most common signs of insanity?
	 * </p>
	 */
	public ModelEvaluatorBuilder setCheckSchema(boolean checkSchema){
		this.checkSchema = checkSchema;

		return this;
	}
}