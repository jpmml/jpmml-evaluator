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
package org.jpmml.sas;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.Batch;
import org.jpmml.evaluator.FilterBatch;
import org.jpmml.evaluator.IntegrationTest;
import org.jpmml.evaluator.IntegrationTestBatch;
import org.jpmml.evaluator.PMMLEquivalence;
import org.jpmml.sas.visitors.ExpressionCorrector;
import org.junit.Test;

public class ClassificationTest extends IntegrationTest {

	public ClassificationTest(){
		super(new PMMLEquivalence(1e-5, 1e-5));
	}

	@Test
	public void evaluateLogisticRegressionAudit() throws Exception {
		evaluateAudit("LogisticRegression", "Audit");
	}

	@Test
	public void evaluateProbitRegressionAudit() throws Exception {
		evaluateAudit("ProbitRegression", "Audit");
	}

	private void evaluateAudit(String name, String dataset) throws Exception {

		try(Batch batch = createFilterBatch(name, dataset, ClassificationTest.AUDIT_COLUMNS)){
			evaluate(batch, getEquivalence());
		}
	}

	@Test
	public void evaluateLogisticRegressionIris() throws Exception {
		evaluateIris("LogisticRegression", "Iris");
	}

	private void evaluateIris(String name, String dataset) throws Exception {

		try(Batch batch = createFilterBatch(name, dataset, ClassificationTest.IRIS_COLUMNS)){
			evaluate(batch, getEquivalence());
		}
	}

	protected Batch createFilterBatch(String name, String dataset, final Map<FieldName, FieldName> columns){
		Batch result = new FilterBatch(createBatch(name, dataset, Predicates.in(columns.values()))){

			@Override
			public List<? extends Map<FieldName, ?>> getOutput() throws Exception {
				Function<Map<FieldName, ?>, Map<FieldName, Object>> function = new Function<Map<FieldName, ?>, Map<FieldName, Object>>(){

					@Override
					public Map<FieldName, Object> apply(Map<FieldName, ?> map){
						Map<FieldName, Object> result = new LinkedHashMap<>();

						Collection<Map.Entry<FieldName, FieldName>> entries = columns.entrySet();
						for(Map.Entry<FieldName, FieldName> entry : entries){
							result.put(entry.getValue(), map.get(entry.getKey()));
						}

						return result;
					}
				};

				return Lists.transform(super.getOutput(), function);
			}
		};

		return result;
	}

	@Override
	protected Batch createBatch(String name, String dataset, Predicate<FieldName> predicate){
		Batch result = new IntegrationTestBatch(name, dataset, predicate){

			@Override
			public ClassificationTest getIntegrationTest(){
				return ClassificationTest.this;
			}

			@Override
			public PMML getPMML() throws Exception {
				PMML pmml = super.getPMML();

				ExpressionCorrector expressionCorrector = new ExpressionCorrector();
				expressionCorrector.applyTo(pmml);

				return pmml;
			}
		};

		return result;
	}

	private static final Map<FieldName, FieldName> AUDIT_COLUMNS = ImmutableMap.of(FieldName.create("EM_CLASSIFICATION"), FieldName.create("Adjusted"), FieldName.create("EM_EVENTPROBABILITY"), FieldName.create("P_Adjusted1"));
	private static final Map<FieldName, FieldName> IRIS_COLUMNS = ImmutableMap.of(FieldName.create("EM_CLASSIFICATION"), FieldName.create("Species"), FieldName.create("U_Species"), FieldName.create("U_Species"));
}