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
package org.jpmml.evaluator.visitors;

import java.util.List;
import java.util.ListIterator;

import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.HasDerivedFields;
import org.dmg.pmml.LocalTransformations;
import org.dmg.pmml.Output;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.PMMLAttributes;
import org.dmg.pmml.TransformationDictionary;
import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.general_regression.BaseCumHazardTables;
import org.dmg.pmml.general_regression.GeneralRegressionModel;
import org.dmg.pmml.naive_bayes.BayesInput;
import org.dmg.pmml.naive_bayes.BayesInputs;
import org.jpmml.evaluator.MissingAttributeException;
import org.jpmml.evaluator.RichDataField;
import org.jpmml.evaluator.RichDerivedField;
import org.jpmml.evaluator.RichOutputField;
import org.jpmml.evaluator.general_regression.RichBaseCumHazardTables;
import org.jpmml.evaluator.naive_bayes.RichBayesInput;

public class MapHolderParser extends AbstractParser {

	@Override
	public VisitorAction visit(BayesInputs bayesInputs){

		if(bayesInputs.hasBayesInputs()){
			List<BayesInput> content = bayesInputs.getBayesInputs();

			for(ListIterator<BayesInput> it = content.listIterator(); it.hasNext(); ){
				BayesInput bayesInput = it.next();

				FieldName name = bayesInput.getField();
				if(name == null){
					throw new MissingAttributeException(bayesInput, org.dmg.pmml.naive_bayes.PMMLAttributes.BAYESINPUT_FIELD);
				}

				DataType dataType;

				DerivedField derivedField = bayesInput.getDerivedField();
				if(derivedField != null){
					dataType = derivedField.getDataType();

					if(dataType == null){
						throw new MissingAttributeException(derivedField, PMMLAttributes.DERIVEDFIELD_DATATYPE);
					}
				} else

				{
					dataType = resolveDataType(name);
				} // End if

				if(dataType != null){
					it.set(new RichBayesInput(dataType, bayesInput));
				}
			}
		}

		return super.visit(bayesInputs);
	}

	@Override
	public VisitorAction visit(DataDictionary dataDictionary){

		if(dataDictionary.hasDataFields()){
			List<DataField> dataFields = dataDictionary.getDataFields();

			for(ListIterator<DataField> it = dataFields.listIterator(); it.hasNext(); ){
				DataField dataField = it.next();

				if(dataField.hasValues()){
					it.set(new RichDataField(dataField));
				}
			}
		}

		return super.visit(dataDictionary);
	}

	@Override
	public VisitorAction visit(GeneralRegressionModel generalRegressionModel){
		BaseCumHazardTables baseCumHazardTables = generalRegressionModel.getBaseCumHazardTables();

		if(baseCumHazardTables != null){
			FieldName baselineStrataVariable = generalRegressionModel.getBaselineStrataVariable();

			if(baselineStrataVariable != null){
				DataType dataType = resolveDataType(baselineStrataVariable);

				if(dataType != null){
					generalRegressionModel.setBaseCumHazardTables(new RichBaseCumHazardTables(dataType, baseCumHazardTables));
				}
			}
		}

		return super.visit(generalRegressionModel);
	}

	@Override
	public VisitorAction visit(LocalTransformations localTransformations){
		processDerivedFields(localTransformations);

		return super.visit(localTransformations);
	}

	@Override
	public VisitorAction visit(Output output){

		if(output.hasOutputFields()){
			List<OutputField> outputFields = output.getOutputFields();

			for(ListIterator<OutputField> it = outputFields.listIterator(); it.hasNext(); ){
				OutputField outputField = it.next();

				if(outputField.hasValues()){
					it.set(new RichOutputField(outputField));
				}
			}
		}

		return super.visit(output);
	}

	@Override
	public VisitorAction visit(TransformationDictionary transformationDictionary){
		processDerivedFields(transformationDictionary);

		return super.visit(transformationDictionary);
	}

	private void processDerivedFields(HasDerivedFields<?> hasDerivedFields){

		if(hasDerivedFields.hasDerivedFields()){
			List<DerivedField> derivedFields = hasDerivedFields.getDerivedFields();

			for(ListIterator<DerivedField> it = derivedFields.listIterator(); it.hasNext(); ){
				DerivedField derivedField = it.next();

				if(derivedField.hasValues()){
					it.set(new RichDerivedField(derivedField));
				}
			}
		}
	}
}