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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.dmg.pmml.Model;
import org.dmg.pmml.Output;
import org.dmg.pmml.PMML;
import org.dmg.pmml.ResultFeature;
import org.jpmml.evaluator.annotations.Functionality;
import org.jpmml.model.PMMLException;
import org.jpmml.model.UnsupportedElementException;
import org.jpmml.model.UnsupportedMarkupException;

abstract
public class ModelManagerFactory<S extends ModelManager<?>> extends ServiceFactory<Model, S> {

	protected ModelManagerFactory(){
	}

	protected ModelManagerFactory(Class<S> serviceClazz){
		super(Model.class, serviceClazz);
	}

	public S newModelManager(PMML pmml, Model model){
		return newModelManager(pmml, model, null);
	}

	public S newModelManager(PMML pmml, Model model, Set<ResultFeature> extraResultFeatures){
		pmml = Objects.requireNonNull(pmml);
		model = Objects.requireNonNull(model);

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

						// Unsupported by this one, might be supported by next ones
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
}