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
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.dmg.pmml.Model;
import org.dmg.pmml.Output;
import org.dmg.pmml.PMML;
import org.dmg.pmml.ResultFeature;
import org.jpmml.evaluator.annotations.Functionality;

abstract
public class ModelManagerFactory<S extends ModelManager<?>> implements Serializable {

	private Class<S> serviceClazz = null;

	transient
	private ListMultimap<Class<? extends Model>, Class<? extends S>> serviceProviderClazzes = null;


	protected ModelManagerFactory(Class<S> serviceClazz){
		setServiceClass(serviceClazz);
	}

	public S newModelManager(PMML pmml, Model model){
		return newModelManager(pmml, model, null);
	}

	public S newModelManager(PMML pmml, Model model, Set<ResultFeature> extraResultFeatures){
		Objects.requireNonNull(pmml);
		Objects.requireNonNull(model);

		Output output = model.getOutput();

		Set<ResultFeature> resultFeatures = ModelManager.collectResultFeatures(output);

		if(extraResultFeatures != null && !extraResultFeatures.isEmpty()){
			resultFeatures.addAll(extraResultFeatures);
		}

		try {
			List<Class<? extends S>> modelManagerClasses = getServiceProviderClasses(model.getClass());

			modelManagers:
			for(Class<? extends S> modelManagerClazz : modelManagerClasses){
				Functionality functionality = modelManagerClazz.getAnnotation(Functionality.class);

				if(functionality != null){
					Set<ResultFeature> providedResultFeatures = EnumSet.noneOf(ResultFeature.class);
					providedResultFeatures.addAll(Arrays.asList(functionality.value()));

					if(!providedResultFeatures.containsAll(resultFeatures)){
						continue modelManagers;
					}
				}

				Constructor<?> constructor = findConstructor(modelManagerClazz);

				try {
					S modelManager = (S)constructor.newInstance(pmml, model);

					if(extraResultFeatures != null && !extraResultFeatures.isEmpty()){
						modelManager.addResultFeatures(extraResultFeatures);
					}

					return modelManager;
				} catch(InvocationTargetException ite){
					Throwable cause = ite.getCause();

					if(cause instanceof PMMLException){

						// Invalid here, invalid everywhere
						if(cause instanceof InvalidMarkupException){
							// Ignored
						} else

						// Unsupported here, might be supported somewhere else
						if(cause instanceof UnsupportedMarkupException){
							continue;
						}

						throw (PMMLException)cause;
					}

					throw ite;
				}
			}
		} catch(ReflectiveOperationException | IOException e){
			throw new IllegalArgumentException(e);
		} catch(PMMLException pe){
			throw pe;
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
		this.serviceClazz = Objects.requireNonNull(serviceClazz);
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
			Class<?>[] parameterTypes = constructor.getParameterTypes();

			if(parameterTypes.length != 2){
				continue;
			}

			if((PMML.class).isAssignableFrom(parameterTypes[0]) && (Model.class).isAssignableFrom(parameterTypes[1])){
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