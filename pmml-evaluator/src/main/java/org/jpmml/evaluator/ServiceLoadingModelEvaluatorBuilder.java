/*
 * Copyright (c) 2019 Villu Ruusmann
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

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;

public class ServiceLoadingModelEvaluatorBuilder extends ModelEvaluatorBuilder {

	public ServiceLoadingModelEvaluatorBuilder(){
	}

	public ServiceLoadingModelEvaluatorBuilder loadService(URL url) throws IOException {
		URLClassLoader clazzLoader = new URLClassLoader(new URL[]{url});

		try {
			return loadService(clazzLoader, (String)null);
		} finally {

			if(clazzLoader instanceof Closeable){
				Closeable closeable = (Closeable)clazzLoader;

				closeable.close();
			}
		}
	}

	public ServiceLoadingModelEvaluatorBuilder loadService(URL url, String modelName) throws IOException {
		URLClassLoader clazzLoader = new URLClassLoader(new URL[]{url});

		try {
			return loadService(clazzLoader, modelName);
		} finally {

			if(clazzLoader instanceof Closeable){
				Closeable closeable = (Closeable)clazzLoader;

				closeable.close();
			}
		}
	}

	public ServiceLoadingModelEvaluatorBuilder loadService(ClassLoader clazzLoader, String modelName){
		PMML pmml;

		Iterator<PMML> pmmlIt;

		try {
			ServiceLoader<PMML> serviceLoader = ServiceLoader.load(PMML.class, clazzLoader);

			pmmlIt = serviceLoader.iterator();
		} catch(ServiceConfigurationError sce){
			throw new IllegalArgumentException("PMML service provider configuration is not loadable", sce);
		}

		if(pmmlIt.hasNext()){

			try {
				pmml = pmmlIt.next();
			} catch(ServiceConfigurationError sce){
				throw new IllegalArgumentException("PMML service provider is not instantiable", sce);
			}

			if(pmmlIt.hasNext()){
				throw new IllegalArgumentException("Expected one PMML service provider, got more than one PMML service providers");
			}
		} else

		{
			throw new IllegalArgumentException("Expected one PMML service provider, got zero PMML service providers");
		}

		Model model = PMMLUtil.findModel(pmml, modelName);

		setPMML(pmml);
		setModel(model);

		return this;
	}
}