/*
 * Copyright (c) 2021 Villu Ruusmann
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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import org.dmg.pmml.PMMLObject;

abstract
public class ServiceFactory<K extends PMMLObject, S> implements Serializable {

	private Class<K> keyClazz = null;

	private Class<S> serviceClazz = null;

	transient
	private ListMultimap<Class<? extends K>, Class<? extends S>> serviceProviderClazzes = null;


	protected ServiceFactory(){
	}

	protected ServiceFactory(Class<K> keyClazz, Class<S> serviceClazz){
		setKeyClass(keyClazz);
		setServiceClass(serviceClazz);
	}

	public List<Class<? extends S>> getServiceProviderClasses(Class<? extends K> objectClazz) throws ClassNotFoundException, IOException {
		Class<K> keyClazz = getKeyClass();
		ListMultimap<Class<? extends K>, Class<? extends S>> serviceProviderClazzes = getServiceProviderClasses();

		Class<? extends K> clazz = objectClazz;

		while(clazz != null){

			if(serviceProviderClazzes.containsKey(clazz)){
				return serviceProviderClazzes.get(clazz);
			}

			Class<?> superClazz = clazz.getSuperclass();

			try {
				clazz = superClazz.asSubclass(keyClazz);
			} catch(ClassCastException cce){
				break;
			}
		}

		return Collections.emptyList();
	}

	public ListMultimap<Class<? extends K>, Class<? extends S>> getServiceProviderClasses() throws ClassNotFoundException, IOException {

		if(this.serviceProviderClazzes == null){
			ClassLoader clazzLoader = getClassLoader();

			Class<K> keyClazz = getKeyClass();
			Class<S> serviceClazz = getServiceClass();

			List<Class<? extends S>> serviceProviderClazzes = loadServiceProviderClasses(clazzLoader, serviceClazz);

			this.serviceProviderClazzes = Multimaps.index(serviceProviderClazzes, serviceProviderClazz -> getKey(keyClazz, serviceClazz, serviceProviderClazz));
		}

		return this.serviceProviderClazzes;
	}

	public ClassLoader getClassLoader(){
		Class<?> clazz = getClass();

		return clazz.getClassLoader();
	}

	public Class<K> getKeyClass(){
		return this.keyClazz;
	}

	private void setKeyClass(Class<K> keyClazz){
		this.keyClazz = Objects.requireNonNull(keyClazz);
	}

	public Class<S> getServiceClass(){
		return this.serviceClazz;
	}

	private void setServiceClass(Class<S> serviceClazz){
		this.serviceClazz = Objects.requireNonNull(serviceClazz);
	}

	static
	protected <S> List<Class<? extends S>> loadServiceProviderClasses(ClassLoader clazzLoader, Class<S> serviceClazz) throws ClassNotFoundException, IOException {
		List<Class<? extends S>> result = new ArrayList<>();

		Enumeration<URL> urls = clazzLoader.getResources("META-INF/services/" + serviceClazz.getName());

		while(urls.hasMoreElements()){
			URL url = urls.nextElement();

			try(InputStream is = url.openStream()){
				List<? extends Class<? extends S>> serviceProviderClazzes = loadServiceProviderClasses(is, clazzLoader, serviceClazz);

				result.addAll(serviceProviderClazzes);
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
	protected <K extends PMMLObject, S> Class<? extends K> getKey(Class<K> keyClazz, Class<S> serviceClazz, Class<? extends S> serviceProviderClazz){
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

				return argumentClazz.asSubclass(keyClazz);
			}

			clazz = superClazz;
		}

		throw new IllegalArgumentException(serviceProviderClazz.getName());
	}
}