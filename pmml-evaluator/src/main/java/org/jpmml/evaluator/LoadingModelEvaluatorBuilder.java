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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.ValidationEventHandler;
import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;
import org.dmg.pmml.ResultFeature;
import org.dmg.pmml.ScoreDistributionTransformer;
import org.dmg.pmml.SimplifyingScoreDistributionTransformer;
import org.dmg.pmml.Visitor;
import org.dmg.pmml.adapters.NodeAdapter;
import org.dmg.pmml.adapters.ScoreDistributionAdapter;
import org.dmg.pmml.tree.NodeTransformer;
import org.dmg.pmml.tree.SimplifyingNodeTransformer;
import org.jpmml.evaluator.visitors.ModelEvaluatorVisitorBattery;
import org.jpmml.model.JAXBUtil;
import org.jpmml.model.SAXUtil;
import org.jpmml.model.filters.ImportFilter;
import org.jpmml.model.visitors.LocatorNullifier;
import org.jpmml.model.visitors.LocatorTransformer;
import org.jpmml.model.visitors.VisitorBattery;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;

/**
 * <p>
 * Builds a {@link ModelEvaluator} based on a PMML XML input stream.
 * </p>
 *
 * <strong>Init sequence</strong>
 * <ol>
 *   <li>Create a new, or clone an existing object.</li>
 *   <li>Perform {@link ModelEvaluatorBuilder}-level (ie. parent level) configuration work.
 *   For example, specify the mapping between model and application schemas.</li>
 *   <li>Perform {@link LoadingModelEvaluatorBuilder}-level configuration work.
 *   For example, specify if XML meta-information should be collected and exposed.</li>
 *   <li>Load the PMML XML input stream into an in-memory class model object.</li>
 *   <li>Transform the in-memory class model object.
 *   For example, customize model business logic.</li>
 *   <li>Build.</li>
 * </ol>
 *
 * <p>
 * Configuration changes typically have no effect after the class model object has been loaded.
 * </p>
 *
 * <strong>Simple example</strong>
 * <pre>{@code
 * Evaluator evaluator = new LoadingModelEvaluatorBuilder()
 *   .load(new File("model.pmml"))
 *   .build();
 * }</pre>
 *
 * <strong>Complex example</strong>
 * <pre>{@code
 * LoadingModelEvaluatorBuilder evaluatorBuilder = new LoadingModelEvaluatorBuilder()
 *   .setOutputFilter(OutputFilters.KEEP_FINAL_RESULTS)
 *   .setLocatable(false);
 *
 * Evaluator firstEvaluator = evaluatorBuilder
 *   .load(new File("model-001.pmml"))
 *   .build();
 *
 * Evaluator secondEvaluator = evaluatorBuilder
 *   .setMutable(true)
 *   .load(new File("model-002.pmml"))
 *   .transform(pmml -> transpile(pmml))
 *   .build();
 * }</pre>
 */
public class LoadingModelEvaluatorBuilder extends ModelEvaluatorBuilder {

	private JAXBContext jaxbContext = null;

	private Schema schema = null;

	private ValidationEventHandler validationEventHandler = null;

	private List<? extends XMLFilter> filters = null;

	private boolean locatable = false;

	private boolean mutable = false;

	private VisitorBattery visitors = new ModelEvaluatorVisitorBattery();


	public LoadingModelEvaluatorBuilder(){
	}

	public LoadingModelEvaluatorBuilder load(File file) throws IOException, ParserConfigurationException, SAXException, JAXBException {

		try(InputStream is = new FileInputStream(file)){
			return load(is);
		}
	}

	public LoadingModelEvaluatorBuilder load(File file, String modelName) throws IOException, ParserConfigurationException, SAXException, JAXBException {

		try(InputStream is = new FileInputStream(file)){
			return load(is, modelName);
		}
	}

	public LoadingModelEvaluatorBuilder load(InputStream is) throws ParserConfigurationException, SAXException, JAXBException {
		return load(is, (String)null);
	}

	public LoadingModelEvaluatorBuilder load(InputStream is, String modelName) throws ParserConfigurationException, SAXException, JAXBException {
		JAXBContext jaxbContext = getJAXBContext();
		Schema schema = getSchema();
		ValidationEventHandler validationEventHandler = getValidationEventHandler();
		List<? extends XMLFilter> filters = getFilters();
		boolean locatable = getLocatable();
		boolean mutable = getMutable();
		VisitorBattery visitors = getVisitors();

		if(jaxbContext == null){
			jaxbContext = JAXBUtil.getContext();
		}

		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		unmarshaller.setSchema(schema);
		unmarshaller.setEventHandler(validationEventHandler);

		if(filters == null){
			filters = Collections.singletonList(new ImportFilter());
		}

		Source source = SAXUtil.createFilteredSource(is, filters.toArray(new XMLFilter[filters.size()]));

		PMML pmml;

		NodeTransformer defaultNodeTransformer = NodeAdapter.NODE_TRANSFORMER_PROVIDER.get();
		ScoreDistributionTransformer defaultScoreDistributionTransformer = ScoreDistributionAdapter.SCOREDISTRIBUTION_TRANSFORMER_PROVIDER.get();

		try {
			NodeAdapter.NODE_TRANSFORMER_PROVIDER.set(mutable ? null : SimplifyingNodeTransformer.INSTANCE);
			ScoreDistributionAdapter.SCOREDISTRIBUTION_TRANSFORMER_PROVIDER.set(mutable ? null : SimplifyingScoreDistributionTransformer.INSTANCE);

			pmml = (PMML)unmarshaller.unmarshal(source);
		} finally {
			NodeAdapter.NODE_TRANSFORMER_PROVIDER.set(defaultNodeTransformer);
			ScoreDistributionAdapter.SCOREDISTRIBUTION_TRANSFORMER_PROVIDER.set(defaultScoreDistributionTransformer);
		}

		Visitor locatorHandler = (locatable ? new LocatorTransformer() : new LocatorNullifier());

		locatorHandler.applyTo(pmml);

		if(visitors != null && !visitors.isEmpty()){
			visitors.applyTo(pmml);
		}

		Model model = PMMLUtil.findModel(pmml, modelName);

		setPMML(pmml);
		setModel(model);

		return this;
	}

	public <E extends Exception> LoadingModelEvaluatorBuilder transform(PMMLTransformer<E> transformer) throws E {
		PMML pmml = getPMML();
		Model model = getModel();

		if((pmml == null) || (model == null)){
			throw new IllegalStateException();
		}

		String modelName = model.getModelName();

		PMML transformedPMML = transformer.apply(pmml);
		if(transformedPMML == null){
			return this;
		}

		Model transformedModel = PMMLUtil.findModel(transformedPMML, modelName);

		setPMML(transformedPMML);
		setModel(transformedModel);

		return this;
	}

	@Override
	public LoadingModelEvaluatorBuilder clone(){
		return (LoadingModelEvaluatorBuilder)super.clone();
	}

	@Override
	protected LoadingModelEvaluatorBuilder setPMML(PMML pmml){
		return (LoadingModelEvaluatorBuilder)super.setPMML(pmml);
	}

	@Override
	protected LoadingModelEvaluatorBuilder setModel(Model model){
		return (LoadingModelEvaluatorBuilder)super.setModel(model);
	}

	@Override
	public LoadingModelEvaluatorBuilder setModelEvaluatorFactory(ModelEvaluatorFactory modelEvaluatorFactory){
		return (LoadingModelEvaluatorBuilder)super.setModelEvaluatorFactory(modelEvaluatorFactory);
	}

	@Override
	public LoadingModelEvaluatorBuilder setValueFactoryFactory(ValueFactoryFactory valueFactoryFactory){
		return (LoadingModelEvaluatorBuilder)super.setValueFactoryFactory(valueFactoryFactory);
	}

	@Override
	public LoadingModelEvaluatorBuilder setOutputFilter(OutputFilter outputFilter){
		return (LoadingModelEvaluatorBuilder)super.setOutputFilter(outputFilter);
	}

	@Override
	public LoadingModelEvaluatorBuilder setDerivedFieldGuard(SymbolTable<String> derivedFieldGuard){
		return (LoadingModelEvaluatorBuilder)super.setDerivedFieldGuard(derivedFieldGuard);
	}

	@Override
	public LoadingModelEvaluatorBuilder setFunctionGuard(SymbolTable<String> functionGuard){
		return (LoadingModelEvaluatorBuilder)super.setFunctionGuard(functionGuard);
	}

	@Override
	public LoadingModelEvaluatorBuilder setExtraResultFeatures(Set<ResultFeature> extraResultFeatures){
		return (LoadingModelEvaluatorBuilder)super.setExtraResultFeatures(extraResultFeatures);
	}

	@Override
	public LoadingModelEvaluatorBuilder setInputMapper(InputMapper inputMapper){
		return (LoadingModelEvaluatorBuilder)super.setInputMapper(inputMapper);
	}

	@Override
	public LoadingModelEvaluatorBuilder setResultMapper(ResultMapper resultMapper){
		return (LoadingModelEvaluatorBuilder)super.setResultMapper(resultMapper);
	}

	@Override
	public LoadingModelEvaluatorBuilder setCheckSchema(boolean checkSchema){
		return (LoadingModelEvaluatorBuilder)super.setCheckSchema(checkSchema);
	}

	public JAXBContext getJAXBContext(){
		return this.jaxbContext;
	}

	/**
	 * @see JAXBUtil#getObjectFactoryClasses()
	 */
	public LoadingModelEvaluatorBuilder setJAXBContext(JAXBContext jaxbContext){
		this.jaxbContext = jaxbContext;

		return this;
	}

	public Schema getSchema(){
		return this.schema;
	}

	/**
	 * @see Unmarshaller#setSchema(Schema)
	 */
	public LoadingModelEvaluatorBuilder setSchema(Schema schema){
		this.schema = schema;

		return this;
	}

	public ValidationEventHandler getValidationEventHandler(){
		return this.validationEventHandler;
	}

	/**
	 * @see Unmarshaller#setEventHandler(ValidationEventHandler)
	 */
	public LoadingModelEvaluatorBuilder setValidationEventHandler(ValidationEventHandler validationEventHandler){
		this.validationEventHandler = validationEventHandler;

		return this;
	}

	public List<? extends XMLFilter> getFilters(){
		return this.filters;
	}

	public LoadingModelEvaluatorBuilder setFilters(List<? extends XMLFilter> filters){
		this.filters = filters;

		return this;
	}

	public boolean getLocatable(){
		return this.locatable;
	}

	/**
	 * <p>
	 * Should PMML class model objects collect and keep SAX Locator (meta-)information?
	 * </p>
	 */
	public LoadingModelEvaluatorBuilder setLocatable(boolean locatable){
		this.locatable = locatable;

		return this;
	}

	public boolean getMutable(){
		return this.mutable;
	}

	/**
	 * <p>
	 * Should polymorphic PMML class model objects use types that favour mutability over memory efficiency?
	 * </p>
	 */
	public LoadingModelEvaluatorBuilder setMutable(boolean mutable){
		this.mutable = mutable;

		return this;
	}

	public VisitorBattery getVisitors(){
		return this.visitors;
	}

	public LoadingModelEvaluatorBuilder setVisitors(VisitorBattery visitors){
		this.visitors = visitors;

		return this;
	}
}