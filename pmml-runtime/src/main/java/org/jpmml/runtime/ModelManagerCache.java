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
package org.jpmml.runtime;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentMap;

import javax.xml.bind.JAXBException;
import javax.xml.transform.sax.SAXSource;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;
import org.jpmml.manager.ModelManager;
import org.jpmml.manager.ModelManagerFactory;
import org.jpmml.manager.PMMLManager;
import org.jpmml.model.ImportFilter;
import org.jpmml.model.JAXBUtil;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

abstract
public class ModelManagerCache {

	private ModelManagerFactory modelManagerFactory = null;

	private LoadingCache<Class<?>, ModelManager<? extends Model>> cache = null;


	public ModelManagerCache(ModelManagerFactory modelManagerFactory, CacheBuilder<Object, Object> cacheBuilder){
		setModelEvaluatorFactory(modelManagerFactory);

		CacheLoader<Class<?>, ModelManager<? extends Model>> cacheLoader = new CacheLoader<Class<?>, ModelManager<? extends Model>>(){

			@Override
			public ModelManager<? extends Model> load(Class<?> clazz) throws Exception {
				return ModelManagerCache.this.loadModelManager(clazz);
			}
		};

		this.cache = cacheBuilder.build(cacheLoader);
	}

	public ModelManager<? extends Model> get(Class<?> clazz) throws Exception {
		return this.cache.get(clazz);
	}

	public void remove(Class<?> clazz){
		this.cache.invalidate(clazz);
	}

	public ConcurrentMap<Class<?>, ModelManager<? extends Model>> asMap(){
		return this.cache.asMap();
	}

	protected PMML process(PMML pmml){
		return pmml;
	}

	private ModelManager<? extends Model> loadModelManager(Class<?> clazz) throws IOException, JAXBException, SAXException {
		String path = (clazz.getName()).replace('.', '/') + ".pmml";

		ClassLoader clazzLoader = clazz.getClassLoader();

		InputStream is = clazzLoader.getResourceAsStream(path);
		if(is == null){
			throw new FileNotFoundException(path);
		}

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

		PMMLManager pmmlManager = new PMMLManager(pmml);

		ModelManagerFactory modelManagerFactory = getModelManagerFactory();

		return pmmlManager.getModelManager(modelManagerFactory);
	}

	public ModelManagerFactory getModelManagerFactory(){
		return this.modelManagerFactory;
	}

	private void setModelEvaluatorFactory(ModelManagerFactory modelManagerFactory){
		this.modelManagerFactory = modelManagerFactory;
	}
}