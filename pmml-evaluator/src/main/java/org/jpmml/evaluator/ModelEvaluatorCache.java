/*
 * Copyright (c) 2014 Villu Ruusmann
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.ConcurrentMap;

import javax.xml.bind.JAXBException;
import javax.xml.transform.sax.SAXSource;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.dmg.pmml.PMML;
import org.jpmml.model.ImportFilter;
import org.jpmml.model.JAXBUtil;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ModelEvaluatorCache {

	private ModelEvaluatorFactory modelEvaluatorFactory = null;

	private LoadingCache<URI, ModelEvaluator<?>> cache = null;


	public ModelEvaluatorCache(CacheBuilder<Object, Object> cacheBuilder){
		this(ModelEvaluatorFactory.newInstance(), cacheBuilder);
	}

	public ModelEvaluatorCache(ModelEvaluatorFactory modelEvaluatorFactory, CacheBuilder<Object, Object> cacheBuilder){
		setModelEvaluatorFactory(modelEvaluatorFactory);

		CacheLoader<URI, ModelEvaluator<?>> cacheLoader = new CacheLoader<URI, ModelEvaluator<?>>(){

			@Override
			public ModelEvaluator<?> load(URI uri) throws Exception {
				return ModelEvaluatorCache.this.loadModelEvaluator(uri);
			}
		};

		this.cache = cacheBuilder.build(cacheLoader);
	}

	public ModelEvaluator<?> get(Class<?> clazz) throws Exception {
		return get(toURL(clazz));
	}

	public ModelEvaluator<?> get(URL url) throws Exception {
		URI uri = url.toURI();

		return this.cache.get(uri);
	}

	public void remove(Class<?> clazz) throws Exception {
		remove(toURL(clazz));
	}

	public void remove(URL url) throws Exception {
		URI uri = url.toURI();

		this.cache.invalidate(uri);
	}

	protected ModelEvaluator<?> loadModelEvaluator(URI uri) throws IOException, JAXBException, SAXException {
		URL url = uri.toURL();

		PMML pmml;

		try(InputStream is = url.openStream()){
			InputSource source = new InputSource(is);

			// Transform a PMML schema version 3.X or 4.X document to a PMML schema version 4.2 document
			SAXSource transformedSource = ImportFilter.apply(source);

			pmml = JAXBUtil.unmarshalPMML(transformedSource);
		}

		pmml = process(pmml);

		ModelEvaluatorFactory modelEvaluatorFactory = getModelEvaluatorFactory();

		ModelEvaluator<?> modelEvaluator = modelEvaluatorFactory.newModelManager(pmml);
		modelEvaluator.verify();

		return modelEvaluator;
	}

	protected PMML process(PMML pmml){
		return pmml;
	}

	public ModelEvaluatorFactory getModelEvaluatorFactory(){
		return this.modelEvaluatorFactory;
	}

	private void setModelEvaluatorFactory(ModelEvaluatorFactory modelEvaluatorFactory){
		this.modelEvaluatorFactory = modelEvaluatorFactory;
	}

	public ConcurrentMap<URI, ModelEvaluator<?>> asMap(){
		return this.cache.asMap();
	}

	static
	private URL toURL(Class<?> clazz) throws IOException {
		String path = (clazz.getName()).replace('.', '/') + ".pmml";

		ClassLoader clazzLoader = clazz.getClassLoader();

		URL url = clazzLoader.getResource(path);
		if(url == null){
			throw new FileNotFoundException(path);
		}

		return url;
	}
}