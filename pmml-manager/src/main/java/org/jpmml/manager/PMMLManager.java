/*
 * Copyright (c) 2009 University of Tartu
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jpmml.manager;

import java.io.*;
import java.util.*;

import org.dmg.pmml.*;

import com.google.common.collect.*;

import static com.google.common.base.Preconditions.*;

public class PMMLManager implements Serializable {

	private PMML pmml = null;


	public PMMLManager(PMML pmml){
		setPMML(pmml);
	}

	public DataField getDataField(FieldName name){
		DataDictionary dataDictionary = getDataDictionary();

		List<DataField> dataFields = dataDictionary.getDataFields();

		return find(dataFields, name);
	}

	public DerivedField getDerivedField(FieldName name){
		TransformationDictionary transformationDictionary = getTransformationDictionary();
		if(transformationDictionary == null){
			return null;
		}

		List<DerivedField> derivedFields = transformationDictionary.getDerivedFields();

		return find(derivedFields, name);
	}

	public DefineFunction getFunction(String name){
		TransformationDictionary transformationDictionary = getTransformationDictionary();
		if(transformationDictionary == null){
			return null;
		}

		List<DefineFunction> defineFunctions = transformationDictionary.getDefineFunctions();
		for(DefineFunction defineFunction : defineFunctions){

			if((defineFunction.getName()).equals(name)){
				return defineFunction;
			}
		}

		return null;
	}

	public PMML getPMML(){
		return this.pmml;
	}

	private void setPMML(PMML pmml){
		this.pmml = checkNotNull(pmml);
	}

	public Header getHeader(){
		PMML pmml = getPMML();

		return checkNotNull(pmml.getHeader());
	}

	public DataDictionary getDataDictionary(){
		PMML pmml = getPMML();

		return checkNotNull(pmml.getDataDictionary());
	}

	public TransformationDictionary getTransformationDictionary(){
		PMML pmml = getPMML();

		return pmml.getTransformationDictionary();
	}

	public List<Model> getModels(){
		PMML pmml = getPMML();

		return pmml.getModels();
	}

	/**
	 * @param modelName The name of the Model to be selected. If <code>null</code>, the first model is selected.
	 *
	 * @see Model#getModelName()
	 */
	public Model getModel(String modelName){
		List<Model> models = getModels();

		if(modelName != null){

			for(Model model : models){

				if(modelName.equals(model.getModelName())){
					return model;
				}
			}

			return null;
		} // End if

		if(models.size() > 0){
			return models.get(0);
		}

		return null;
	}

	public ModelManager<? extends Model> getModelManager(String modelName, ModelManagerFactory modelManagerFactory){
		Model model = getModel(modelName);

		return modelManagerFactory.getModelManager(getPMML(), model);
	}

	@SuppressWarnings (
		value = {"unchecked"}
	)
	static
	public <E extends PMMLObject> E find(List<?> objects, Class<? extends E> clazz){

		for(Object object : objects){

			if(object.getClass().equals(clazz)){
				return (E)object;
			}
		}

		return null;
	}

	@SuppressWarnings (
		value = {"unchecked"}
	)
	static
	public <E extends PMMLObject> List<E> findAll(List<?> objects, Class<? extends E> clazz){
		List<E> result = Lists.newArrayList();

		for(Object object : objects){

			if(object.getClass().equals(clazz)){
				result.add((E)object);
			}
		}

		return result;
	}

	static
	public <E extends PMMLObject & HasName> E find(Collection<E> objects, FieldName name){

		for(E object : objects){

			if((object.getName()).equals(name)){
				return object;
			}
		}

		return null;
	}
}