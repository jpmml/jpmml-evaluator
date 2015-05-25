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
import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;
import org.jpmml.model.ImportFilter;
import org.jpmml.model.JAXBUtil;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

abstract
public class ModelManagerCache {

	private ModelManagerFactory modelManagerFactory = null;

	private LoadingCache<URI, ModelManager<? extends Model>> cache = null;


	public ModelManagerCache(ModelManagerFactory modelManagerFactory, CacheBuilder<Object, Object> cacheBuilder){
		setModelEvaluatorFactory(modelManagerFactory);

		CacheLoader<URI, ModelManager<? extends Model>> cacheLoader = new CacheLoader<URI, ModelManager<? extends Model>>(){

			@Override
			public ModelManager<? extends Model> load(URI uri) throws Exception {
				return ModelManagerCache.this.loadModelManager(uri);
			}
		};

		this.cache = cacheBuilder.build(cacheLoader);
	}

	public ModelManager<? extends Model> get(Class<?> clazz) throws Exception {
		return get(toURL(clazz));
	}

	public ModelManager<? extends Model> get(URL url) throws Exception {
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

	public ConcurrentMap<URI, ModelManager<? extends Model>> asMap(){
		return this.cache.asMap();
	}

	protected ModelManager<? extends Model> loadModelManager(URI uri) throws IOException, JAXBException, SAXException {
		URL url = uri.toURL();

		InputStream is = url.openStream();

		PMML pmml;

		try {
			InputSource source = new InputSource(is);

			// Transform a PMML schema version 3.X or 4.X document to a PMML schema version 4.2 document
			SAXSource transformedSource = ImportFilter.apply(source);

			pmml = JAXBUtil.unmarshalPMML(transformedSource);
		} finally {
			is.close();
		}

		pmml = process(pmml);

		ModelManagerFactory modelManagerFactory = getModelManagerFactory();

		return modelManagerFactory.getModelManager(pmml);
	}

	protected PMML process(PMML pmml){
		return pmml;
	}

	public ModelManagerFactory getModelManagerFactory(){
		return this.modelManagerFactory;
	}

	private void setModelEvaluatorFactory(ModelManagerFactory modelManagerFactory){
		this.modelManagerFactory = modelManagerFactory;
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