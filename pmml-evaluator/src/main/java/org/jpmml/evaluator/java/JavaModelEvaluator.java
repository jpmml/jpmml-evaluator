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
import org.jpmml.evaluator.JavaModel;
import org.jpmml.evaluator.ModelEvaluationContext;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueFactoryFactory;

public class JavaModelEvaluator extends ModelEvaluator<JavaModel> {

	public JavaModelEvaluator(PMML pmml){
		this(pmml, selectModel(pmml, JavaModel.class));
	}

	public JavaModelEvaluator(PMML pmml, JavaModel javaModel){
		super(pmml, javaModel);
	}

	@Override
	public String getSummary(){
		return "Java";
	}

	@Override
	public Map<FieldName, ?> evaluate(Map<FieldName, ?> arguments){
		JavaModelEvaluationContext context = new JavaModelEvaluationContext(this);
		context.setArguments(arguments);

		return evaluate(context);
	}

	@Override
	public Map<FieldName, ?> evaluate(ModelEvaluationContext context){
		return evaluate((JavaModelEvaluationContext)context);
	}

	public Map<FieldName, ?> evaluate(JavaModelEvaluationContext context){
		JavaModel javaModel = ensureScorableModel();

		return javaModel.evaluate(context);
	}

	@Override
	public ModelEvaluatorFactory ensureModelEvaluatorFactory(){
		return super.ensureModelEvaluatorFactory();
	}

	@Override
	public ValueFactoryFactory ensureValueFactoryFactory(){
		return super.ensureValueFactoryFactory();
	}

	@Override
	public ValueFactory<?> ensureValueFactory(){
		return super.ensureValueFactory();
	}
}