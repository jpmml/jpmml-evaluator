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
package org.jpmml.evaluator;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DefineFunction;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.HasName;
import org.dmg.pmml.PMML;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.TransformationDictionary;

import static com.google.common.base.Preconditions.checkNotNull;

public class PMMLManager implements Serializable {

	private PMML pmml = null;


	public PMMLManager(PMML pmml){
		setPMML(checkNotNull(pmml));
	}

	public DataField getDataField(FieldName name){
		DataDictionary dataDictionary = getDataDictionary();
		if(dataDictionary == null){
			return null;
		}

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

	public DataDictionary getDataDictionary(){
		PMML pmml = getPMML();

		return pmml.getDataDictionary();
	}

	public TransformationDictionary getTransformationDictionary(){
		PMML pmml = getPMML();

		return pmml.getTransformationDictionary();
	}

	public PMML getPMML(){
		return this.pmml;
	}

	private void setPMML(PMML pmml){
		this.pmml = pmml;
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