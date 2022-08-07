/*
 * Copyright (c) 2022 Villu Ruusmann
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorBuilder;
import org.jpmml.evaluator.FieldNameSet;
import org.jpmml.evaluator.FunctionNameStack;
import org.jpmml.evaluator.LoadingModelEvaluatorBuilder;
import org.jpmml.evaluator.PMMLTransformer;
import org.jpmml.evaluator.ResultField;

abstract
public class SimpleArchiveBatch extends ArchiveBatch {

	public SimpleArchiveBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		super(algorithm, dataset, columnFilter, equivalence);
	}

	abstract
	public ArchiveBatchTest getArchiveBatchTest();

	@Override
	public InputStream open(String path) throws IOException {
		ArchiveBatchTest batchTest = getArchiveBatchTest();

		Class<? extends ArchiveBatchTest> clazz = batchTest.getClass();

		InputStream result = clazz.getResourceAsStream(path);
		if(result == null){
			throw new IOException(path);
		}

		return result;
	}

	@Override
	public Evaluator getEvaluator() throws Exception {
		EvaluatorBuilder evaluatorBuilder = getEvaluatorBuilder();

		Evaluator evaluator = evaluatorBuilder.build();

		evaluator.verify();

		return evaluator;
	}

	public EvaluatorBuilder getEvaluatorBuilder() throws Exception {
		LoadingModelEvaluatorBuilder evaluatorBuilder = createLoadingModelEvaluatorBuilder();

		try(InputStream is = open(getPmmlPath())){
			evaluatorBuilder.load(is);
		}

		List<PMMLTransformer<?>> transformers = getTransformers();
		for(PMMLTransformer<?> transformer : transformers){
			evaluatorBuilder.transform(transformer);
		}

		return evaluatorBuilder;
	}

	public String getPmmlPath(){
		return "/pmml/" + (getAlgorithm() + getDataset()) + ".pmml";
	}

	protected LoadingModelEvaluatorBuilder createLoadingModelEvaluatorBuilder(){
		LoadingModelEvaluatorBuilder evaluatorBuilder = new LoadingModelEvaluatorBuilder();

		// XXX
		evaluatorBuilder
			.setDerivedFieldGuard(new FieldNameSet(8))
			.setFunctionGuard(new FunctionNameStack(4));

		return evaluatorBuilder;
	}

	protected List<PMMLTransformer<?>> getTransformers(){
		return Collections.emptyList();
	}
}