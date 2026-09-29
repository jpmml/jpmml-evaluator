/*
 * Copyright (c) 2013 Villu Ruusmann
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
package org.jpmml.evaluator.regression;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import org.dmg.pmml.PMML;
import org.dmg.pmml.regression.PMMLAttributes;
import org.dmg.pmml.regression.RegressionModel;
import org.dmg.pmml.regression.RegressionTable;
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.PMMLUtil;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TargetUtil;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueMap;
import org.jpmml.model.InvalidAttributeException;
import org.jpmml.model.InvalidElementException;
import org.jpmml.model.InvalidElementListException;
import org.jpmml.model.MisplacedAttributeException;

public class RegressionModelEvaluator extends ModelEvaluator<RegressionModel> {

	private Map<String, List<RegressionTable>> regressionTables = Collections.emptyMap();


	private RegressionModelEvaluator(){
	}

	public RegressionModelEvaluator(PMML pmml){
		this(pmml, PMMLUtil.findModel(pmml, RegressionModel.class));
	}

	public RegressionModelEvaluator(PMML pmml, RegressionModel regressionModel){
		super(pmml, regressionModel);

		List<RegressionTable> regressionTables = regressionModel.requireRegressionTables();

		// Cannot use Guava's ImmutableMap, because it is null-hostile
		this.regressionTables = Collections.unmodifiableMap(new LinkedHashMap<>(toImmutableListMap(parseRegressionTables(regressionTables))));
	}

	@Override
	public String getSummary(){
		return "Regression";
	}

	@Override
	protected <V extends Number> Map<String, ?> evaluateRegression(ValueFactory<V> valueFactory, EvaluationContext context){
		RegressionModel regressionModel = getModel();

		if(hasSoleTargetField()){
			TargetField targetField = getSoleTargetField();

			RegressionTable regressionTable = getRegressionTable(targetField);

			Value<V> value = RegressionTableUtil.evaluateRegression(valueFactory, regressionModel, regressionTable, context);
			if(value == null){
				return TargetUtil.evaluateRegressionDefault(valueFactory, targetField);
			}

			return TargetUtil.evaluateRegression(targetField, value);
		} else

		{
			List<TargetField> targetFields = getMultipleTargetFields();

			Map<String, Object> results = new LinkedHashMap<>(2 * targetFields.size());

			for(int i = 0, max = targetFields.size(); i < max; i++){
				TargetField targetField = targetFields.get(i);

				RegressionTable regressionTable = getRegressionTable(targetField);

				Value<V> value = RegressionTableUtil.evaluateRegression(valueFactory, regressionModel, regressionTable, context);
				if(value == null){
					results.putAll(TargetUtil.evaluateRegressionDefault(valueFactory, targetField));

					continue;
				}

				results.putAll(TargetUtil.evaluateRegression(targetField, value));
			}

			return results;
		}
	}

	@Override
	protected <V extends Number> Map<String, ? extends Classification<?, V>> evaluateClassification(ValueFactory<V> valueFactory, EvaluationContext context){
		RegressionModel regressionModel = getModel();

		if(hasSoleTargetField()){
			TargetField targetField = getSoleTargetField();

			List<RegressionTable> regressionTables = getRegressionTables(targetField);

			ValueMap<Object, V> values = RegressionTableUtil.evaluateClassification(valueFactory, regressionModel, targetField, regressionTables, context);

			// "If one or more RegressionTable elements cannot be evaluated, then the predictions are defined by the priorProbability values of the Target element"
			if(values == null){
				return TargetUtil.evaluateClassificationDefault(valueFactory, targetField);
			}

			Classification<?, V> result = createClassification(values);

			return TargetUtil.evaluateClassification(targetField, result);
		} else

		{
			List<TargetField> targetFields = getMultipleTargetFields();

			Map<String, Classification<?, V>> results = new LinkedHashMap<>(2 * targetFields.size());

			for(int i = 0, max = targetFields.size(); i < max; i++){
				TargetField targetField = targetFields.get(i);

				List<RegressionTable> regressionTables = getRegressionTables(targetField);

				ValueMap<Object, V> values = RegressionTableUtil.evaluateClassification(valueFactory, regressionModel, targetField, regressionTables, context);
				if(values == null){
					results.putAll(TargetUtil.evaluateClassificationDefault(valueFactory, targetField));

					continue;
				}

				Classification<?, V> result = createClassification(values);

				results.putAll(TargetUtil.evaluateClassification(targetField, result));
			}

			return results;
		}
	}

	private RegressionTable getRegressionTable(TargetField targetField){
		List<RegressionTable> regressionTables = getRegressionTables(targetField);

		if(regressionTables.size() != 1){
			throw new InvalidElementListException(regressionTables);
		}

		return regressionTables.get(0);
	}

	private List<RegressionTable> getRegressionTables(TargetField targetField){
		RegressionModel regressionModel = getModel();

		Map<String, List<RegressionTable>> regressionTables = getRegressionTables();

		String targetFieldName = regressionModel.getTargetField();

		List<RegressionTable> result;

		if(hasSoleTargetField()){

			if(targetFieldName != null && !Objects.equals(targetField.getName(), targetFieldName)){
				throw new InvalidAttributeException(regressionModel, PMMLAttributes.REGRESSIONMODEL_TARGETFIELD, targetFieldName);
			} // End if

			if(regressionTables.size() != 1){
				throw new InvalidElementException(regressionModel);
			}

			result = regressionTables.get(targetField.getName());
			if(result == null){
				result = regressionTables.get(Evaluator.DEFAULT_TARGET_NAME);
			}
		} else

		{
			List<TargetField> targetFields = getMultipleTargetFields();

			if(targetFieldName != null){
				throw new MisplacedAttributeException(regressionModel, PMMLAttributes.REGRESSIONMODEL_TARGETFIELD, targetFieldName);
			} // End if

			if(regressionTables.size() != targetFields.size()){
				throw new InvalidElementException(regressionModel);
			}

			result = regressionTables.get(targetField.getName());
		} // End if

		if(result == null){
			throw new InvalidElementException(regressionModel);
		}

		return result;
	}

	private Map<String, List<RegressionTable>> getRegressionTables(){
		return this.regressionTables;
	}

	static
	private Map<String, List<RegressionTable>> parseRegressionTables(List<RegressionTable> regressionTables){
		ListMultimap<String, RegressionTable> result = ArrayListMultimap.create();

		for(RegressionTable regressionTable : regressionTables){
			result.put(regressionTable.getTargetField(), regressionTable);
		}

		return new LinkedHashMap<>(Multimaps.asMap(result));
	}
}