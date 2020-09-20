/*
 * Copyright (c) 2015 Villu Ruusmann
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
package org.jpmml.evaluator.testing;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import com.esotericsoftware.kryo.Kryo;
import com.google.common.base.Equivalence;
import org.dmg.pmml.Application;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Visitor;
import org.dmg.pmml.VisitorAction;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorBuilder;
import org.jpmml.evaluator.ModelEvaluatorBuilder;
import org.jpmml.evaluator.OutputFilters;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.kryo.KryoUtil;
import org.jpmml.evaluator.visitors.InvalidMarkupInspector;
import org.jpmml.evaluator.visitors.UnsupportedMarkupInspector;
import org.jpmml.model.SerializationUtil;
import org.jpmml.model.visitors.LocatorTransformer;

abstract
public class IntegrationTestBatch extends ArchiveBatch {

	private String testJavaSerializability = System.getProperty(IntegrationTestBatch.class.getName() + ".testJavaSerializability", "true");

	private String testKryoSerializability = System.getProperty(IntegrationTestBatch.class.getName() + ".testKryoSerializability", "true");

	private Evaluator evaluator = null;


	public IntegrationTestBatch(String name, String dataset, Predicate<ResultField> predicate, Equivalence<Object> equivalence){
		super(name, dataset, predicate, equivalence);
	}

	abstract
	public IntegrationTest getIntegrationTest();

	@Override
	public InputStream open(String path){
		IntegrationTest integrationTest = getIntegrationTest();

		Class<? extends IntegrationTest> clazz = integrationTest.getClass();

		return clazz.getResourceAsStream(path);
	}

	@Override
	public EvaluatorBuilder getEvaluatorBuilder() throws Exception {
		ModelEvaluatorBuilder modelEvaluatorBuilder = (ModelEvaluatorBuilder)super.getEvaluatorBuilder();

		modelEvaluatorBuilder.setOutputFilter(OutputFilters.KEEP_FINAL_RESULTS);

		return modelEvaluatorBuilder;
	}

	/**
	 * @see #validateEvaluator(Evaluator)
	 */
	@Override
	public Evaluator getEvaluator() throws Exception {

		if(this.evaluator != null){
			throw new IllegalStateException();
		}

		Evaluator evaluator =  super.getEvaluator();

		validateEvaluator(evaluator);

		this.evaluator = evaluator;

		return evaluator;
	}

	/**
	 * @see #validatePMML(PMML)
	 */
	@Override
	public PMML getPMML() throws Exception {
		PMML pmml = super.getPMML();

		LocatorTransformer locatorTransformer = new LocatorTransformer();
		locatorTransformer.applyTo(pmml);

		validatePMML(pmml);

		return pmml;
	}

	@Override
	public void close() throws Exception {

		if(this.evaluator != null){
			Evaluator evaluator = this.evaluator;

			try {
				validateEvaluator(evaluator);
			} finally {
				this.evaluator = null;
			}
		}
	}

	protected void validateEvaluator(Evaluator evaluator) throws Exception {

		if(Boolean.parseBoolean(this.testJavaSerializability)){
			SerializationUtil.clone((Serializable)evaluator);
		} // End if

		if(Boolean.parseBoolean(this.testKryoSerializability)){
			Kryo kryo = new Kryo();

			KryoUtil.init(kryo);
			KryoUtil.register(kryo);

			KryoUtil.clone(kryo, (Serializable)evaluator);
		}
	}

	protected void validatePMML(PMML pmml) throws Exception {
		List<Visitor> visitors = Arrays.<Visitor>asList(
			new UnsupportedMarkupInspector(),
			new InvalidMarkupInspector(){

				@Override
				public VisitorAction visit(Application application){
					String name = application.getName();

					if(name == null){
						return VisitorAction.SKIP;
					}

					return super.visit(application);
				}

				@Override
				public VisitorAction visit(MiningSchema miningSchema){

					if(!miningSchema.hasMiningFields()){
						return VisitorAction.SKIP;
					}

					return super.visit(miningSchema);
				}
			}
		);

		for(Visitor visitor : visitors){
			visitor.applyTo(pmml);
		}
	}
}
