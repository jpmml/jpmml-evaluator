/*
 * Copyright (c) 2020 Villu Ruusmann
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;

abstract
public class ModelManagerFactory<S extends ModelManager<?>> {

	private Class<S> serviceClazz = null;

	private ListMultimap<Class<? extends Model>, Class<? extends S>> serviceProviderClazzes = null;


	protected ModelManagerFactory(){
	}

	protected ModelManagerFactory(Class<S> serviceClazz){
		setServiceClass(serviceClazz);
	}

	public S newModelManager(PMML pmml, Model model){
		Objects.requireNonNull(pmml);
		Objects.requireNonNull(model);

		try {
			List<Class<? extends S>> modelManagerClasses = getServiceProviderClasses(model.getClass());

			for(Class<? extends S> modelManagerClazz : modelManagerClasses){
				Constructor<?> constructor = findConstructor(modelManagerClazz);

				try {
					return (S)constructor.newInstance(pmml, model);
				} catch(InvocationTargetException ite){
					Throwable cause = ite.getCause();

					if(cause instanceof UnsupportedMarkupException){
						continue;
					}

					throw ite;
				}
			}
		} catch(ReflectiveOperationException | IOException e){
			throw new IllegalArgumentException(e);
		}

		throw new UnsupportedElementException(model);
	}

	public List<Class<? extends S>> getServiceProviderClasses(Class<? extends Model> modelClazz) throws ClassNotFoundException, IOException {
		ListMultimap<Class<? extends Model>, Class<? extends S>> serviceProviderClazzes = getServiceProviderClasses();

		while(modelClazz != null){
			List<Class<? extends S>> modelServiceProviderClazzes = serviceProviderClazzes.get(modelClazz);

			if(modelServiceProviderClazzes != null && !modelServiceProviderClazzes.isEmpty()){
				return modelServiceProviderClazzes;
			}

			Class<?> modelSuperClazz = modelClazz.getSuperclass();

			if(!(Model.class).isAssignableFrom(modelSuperClazz)){
				break;
			}

			modelClazz = modelSuperClazz.asSubclass(Model.class);
		}

		return Collections.emptyList();
	}

	public Class<S> getServiceClass(){
		return this.serviceClazz;
	}

	private void setServiceClass(Class<S> serviceClazz){
		this.serviceClazz = serviceClazz;
	}

	public ListMultimap<Class<? extends Model>, Class<? extends S>> getServiceProviderClasses() throws ClassNotFoundException, IOException {

		if(this.serviceProviderClazzes == null){
			Class<S> serviceClazz = getServiceClass();

			this.serviceProviderClazzes = loadServiceProviderClasses(serviceClazz);
		}

		return this.serviceProviderClazzes;
	}

	static
	private <S extends ModelManager<?>> ListMultimap<Class<? extends Model>, Class<? extends S>> loadServiceProviderClasses(Class<S> serviceClazz) throws ClassNotFoundException, IOException {
		Thread thread = Thread.currentThread();

		ClassLoader clazzLoader = thread.getContextClassLoader();
		if(clazzLoader == null){
			clazzLoader = ClassLoader.getSystemClassLoader();
		}

		ListMultimap<Class<? extends Model>, Class<? extends S>> result = ArrayListMultimap.create();

		Enumeration<URL> urls = clazzLoader.getResources("META-INF/services/" + serviceClazz.getName());

		while(urls.hasMoreElements()){
			URL url = urls.nextElement();

			try(InputStream is = url.openStream()){
				List<? extends Class<? extends S>> serviceProviderClazzes = loadServiceProviderClasses(is, clazzLoader, serviceClazz);

				for(Class<? extends S> serviceProviderClazz : serviceProviderClazzes){
					Class<? extends Model> modelClazz = findModelParameter(serviceClazz, serviceProviderClazz);

					result.put(modelClazz, serviceProviderClazz);
				}
			}
		}

		return result;
	}

	static
	private <S> List<Class<? extends S>> loadServiceProviderClasses(InputStream is, ClassLoader clazzLoader, Class<S> serviceClazz) throws ClassNotFoundException, IOException {
		List<Class<? extends S>> result = new ArrayList<>();

		BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 1024);

		while(true){
			String line = reader.readLine();

			if(line == null){
				break;
			}

			int hash = line.indexOf('#');
			if(hash > -1){
				line = line.substring(0, hash);
			}

			line = line.trim();

			if(line.isEmpty()){
				continue;
			}

			Class<?> serviceProviderClazz = Class.forName(line, false, clazzLoader);

			if(!(serviceClazz).isAssignableFrom(serviceProviderClazz)){
				throw new IllegalArgumentException(line);
			}

			result.add((Class)serviceProviderClazz);
		}

		reader.close();

		return result;
	}

	static
	private Constructor<?> findConstructor(Class<?> serviceProviderClass) throws NoSuchMethodException {
		Constructor<?>[] constructors = serviceProviderClass.getConstructors();

		for(Constructor<?> constructor : constructors){
			Parameter[] parameters = constructor.getParameters();

			if(parameters.length != 2){
				continue;
			}

			Parameter pmmlParameter = parameters[0];
			Parameter modelParameter = parameters[1];

			if((PMML.class).isAssignableFrom(pmmlParameter.getType()) && (Model.class).isAssignableFrom(modelParameter.getType())){
				return constructor;
			}
		}

		throw new NoSuchMethodException();
	}

	static
	private Class<? extends Model> findModelParameter(Class<?> serviceClazz, Class<?> serviceProviderClazz){
		Class<?> clazz = serviceProviderClazz;

		while(clazz != null){
			Class<?> superClazz = clazz.getSuperclass();

			if((serviceClazz).equals(superClazz)){
				ParameterizedType parameterizedType = (ParameterizedType)clazz.getGenericSuperclass();

				Type[] arguments = parameterizedType.getActualTypeArguments();
				if(arguments.length != 1){
					throw new IllegalArgumentException(clazz.getName());
				}

				Class<?> argumentClazz = (Class<?>)arguments[0];

				return argumentClazz.asSubclass(Model.class);
			}

			clazz = superClazz;
		}

		throw new IllegalArgumentException(serviceProviderClazz.getName());
	}
}