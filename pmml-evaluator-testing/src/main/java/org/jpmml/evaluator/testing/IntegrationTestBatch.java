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
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import com.esotericsoftware.kryo.Kryo;
import com.google.common.base.Equivalence;
import org.dmg.pmml.Application;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Visitor;
import org.dmg.pmml.VisitorAction;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorBuilder;
import org.jpmml.evaluator.HasEntityRegistry;
import org.jpmml.evaluator.ModelEvaluatorBuilder;
import org.jpmml.evaluator.OutputFilters;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.kryo.KryoUtil;
import org.jpmml.evaluator.visitors.UnsupportedMarkupInspector;
import org.jpmml.model.SerializationUtil;
import org.jpmml.model.visitors.InvalidMarkupInspector;
import org.jpmml.model.visitors.LocatorTransformer;
import org.jpmml.model.visitors.MissingMarkupInspector;

abstract
public class IntegrationTestBatch extends ArchiveBatch {

	private String checkJavaSerializability = System.getProperty(IntegrationTestBatch.class.getName() + "." + "checkJavaSerializability", String.valueOf(true));

	private String checkKryoSerializability = System.getProperty(IntegrationTestBatch.class.getName() + "." + "checkKryoSerializability", String.valueOf(IntegrationTestBatch.KRYO_MODULE_PROVIDED));

	private Evaluator evaluator = null;


	public IntegrationTestBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		super(algorithm, dataset, columnFilter, equivalence);
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

		if(evaluator instanceof HasEntityRegistry){
			HasEntityRegistry<?> hasEntityRegistry = (HasEntityRegistry<?>)evaluator;

			hasEntityRegistry.getEntityRegistry();
		} // End if

		if(Boolean.parseBoolean(this.checkJavaSerializability)){
			SerializationUtil.clone(evaluator);
		} // End if

		if(Boolean.parseBoolean(this.checkKryoSerializability)){
			Kryo kryo = new Kryo();

			KryoUtil.init(kryo);
			KryoUtil.register(kryo);

			KryoUtil.clone(kryo, evaluator);
		}
	}

	protected void validatePMML(PMML pmml) throws Exception {
		List<Visitor> visitors = Arrays.<Visitor>asList(
			new MissingMarkupInspector(){

				@Override
				public VisitorAction visit(Application application){
					String name = application.getName();

					if(name == null){
						return VisitorAction.SKIP;
					}

					return super.visit(application);
				}
			},
			new InvalidMarkupInspector(),
			new UnsupportedMarkupInspector()
		);

		for(Visitor visitor : visitors){
			visitor.applyTo(pmml);
		}
	}

	static
	protected String truncate(String string){

		for(int i = 0; i < string.length(); i++){
			char c = string.charAt(i);

			if(!Character.isLetterOrDigit(c)){
				return string.substring(0, i);
			}
		}

		return string;
	}

	private static final boolean KRYO_MODULE_PROVIDED;

	static {
		Class<?> clazz;

		try {
			clazz = Class.forName("org.jpmml.evaluator.kryo.KryoUtil");
		} catch(ClassNotFoundException cnfe){
			clazz = null;
		}

		KRYO_MODULE_PROVIDED = (clazz != null);
	}
}
