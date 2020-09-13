/*
 * Copyright (c) 2017 Villu Ruusmann
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
package org.jpmml.evaluator.java;

import java.util.Map;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.PMMLUtil;
import org.jpmml.evaluator.ValueFactory;

public class JavaModelEvaluator extends ModelEvaluator<JavaModel> {

	private JavaModelEvaluator(){
	}

	public JavaModelEvaluator(PMML pmml){
		this(pmml, PMMLUtil.findModel(pmml, JavaModel.class));
	}

	public JavaModelEvaluator(PMML pmml, JavaModel javaModel){
		super(pmml, javaModel);
	}

	@Override
	public String getSummary(){
		return "Java";
	}

	@Override
	protected <V extends Number> Map<FieldName, ?> evaluateRegression(ValueFactory<V> valueFactory, EvaluationContext context){
		JavaModel javaModel = getModel();

		return javaModel.evaluateRegression(valueFactory, context);
	}

	@Override
	protected <V extends Number> Map<FieldName, ?> evaluateClassification(ValueFactory<V> valueFactory, EvaluationContext context){
		JavaModel javaModel = getModel();

		return javaModel.evaluateClassification(valueFactory, context);
	}
}