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

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;

import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Visitor;
import org.jpmml.model.JAXBUtil;
import org.jpmml.model.SAXUtil;
import org.jpmml.model.VisitorBattery;
import org.jpmml.model.filters.ImportFilter;
import org.jpmml.model.visitors.LocatorNullifier;
import org.jpmml.model.visitors.LocatorTransformer;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;

public class LoadingModelEvaluatorBuilder extends ModelEvaluatorBuilder {

	private Schema schema = null;

	private ValidationEventHandler validationEventHandler = null;

	private List<? extends XMLFilter> filters = null;

	private boolean locatable = false;

	private VisitorBattery visitors = null;


	public LoadingModelEvaluatorBuilder(){
	}

	public LoadingModelEvaluatorBuilder load(File file) throws IOException, SAXException, JAXBException {

		try(InputStream is = new FileInputStream(file)){
			return load(is);
		}
	}

	public LoadingModelEvaluatorBuilder load(File file, String modelName) throws IOException, SAXException, JAXBException {

		try(InputStream is = new FileInputStream(file)){
			return load(is, modelName);
		}
	}

	public LoadingModelEvaluatorBuilder load(InputStream is) throws SAXException, JAXBException {
		return load(is, (String)null);
	}

	public LoadingModelEvaluatorBuilder load(InputStream is, String modelName) throws SAXException, JAXBException {
		Schema schema = getSchema();
		ValidationEventHandler validationEventHandler = getValidationEventHandler();
		List<? extends XMLFilter> filters = getFilters();
		boolean locatable = getLocatable();
		VisitorBattery visitors = getVisitors();

		Unmarshaller unmarshaller = JAXBUtil.createUnmarshaller();
		unmarshaller.setSchema(schema);
		unmarshaller.setEventHandler(validationEventHandler);

		if(filters == null){
			filters = Collections.singletonList(new ImportFilter());
		}

		Source source = SAXUtil.createFilteredSource(is, filters.toArray(new XMLFilter[filters.size()]));

		PMML pmml = (PMML)unmarshaller.unmarshal(source);

		Visitor locatorHandler = (locatable ? new LocatorTransformer() : new LocatorNullifier());

		locatorHandler.applyTo(pmml);

		if(visitors != null && visitors.size() > 0){
			visitors.applyTo(pmml);
		}

		Model model = PMMLUtil.findModel(pmml, modelName);

		setPMML(pmml);
		setModel(model);

		return this;
	}

	@Override
	public LoadingModelEvaluatorBuilder clone(){
		return (LoadingModelEvaluatorBuilder)super.clone();
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

	public VisitorBattery getVisitors(){
		return this.visitors;
	}

	public LoadingModelEvaluatorBuilder setVisitors(VisitorBattery visitors){
		this.visitors = visitors;

		return this;
	}
}