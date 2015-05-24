/*
 * Copyright (c) 2013 KNIME.com AG, Zurich, Switzerland
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

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import com.google.common.cache.Cache;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.collect.Table;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldUsageType;
import org.dmg.pmml.InlineTable;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningFunctionType;
import org.dmg.pmml.Model;
import org.dmg.pmml.ModelVerification;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.VerificationField;
import org.dmg.pmml.VerificationFields;

abstract
public class ModelEvaluator<M extends Model> extends ModelManager<M> implements Evaluator {

	public ModelEvaluator(PMML pmml, M model){
		super(pmml, model);
	}

	abstract
	public Map<FieldName, ?> evaluate(ModelEvaluationContext context);

	@Override
	public DataField getDataField(FieldName name){

		if(name == null){
			return getDataField();
		}

		return super.getDataField(name);
	}

	/**
	 * @return A synthetic {@link DataField} describing the default target field.
	 */
	protected DataField getDataField(){
		Model model = getModel();

		MiningFunctionType miningFunction = model.getFunctionName();
		switch(miningFunction){
			case REGRESSION:
				return new DataField(null, OpType.CONTINUOUS, DataType.DOUBLE);
			case CLASSIFICATION:
			case CLUSTERING:
				return new DataField(null, OpType.CATEGORICAL, DataType.STRING);
			default:
				break;
		}

		return null;
	}

	@Override
	public FieldValue prepare(FieldName name, Object value){
		DataField dataField = getDataField(name);
		MiningField miningField = getMiningField(name);

		if(dataField == null || miningField == null){
			throw new EvaluationException();
		}

		return ArgumentUtil.prepare(dataField, miningField, value);
	}

	@Override
	public void verify(){
		M model = getModel();

		ModelVerification modelVerification = model.getModelVerification();
		if(modelVerification == null){
			return;
		}

		VerificationFields verificationFields = modelVerification.getVerificationFields();
		if(verificationFields == null){
			throw new InvalidFeatureException(modelVerification);
		}

		Map<FieldName, VerificationField> fieldMap = Maps.newLinkedHashMap();

		for(VerificationField verificationField : verificationFields){
			fieldMap.put(FieldName.create(verificationField.getField()), verificationField);
		}

		InlineTable inlineTable = modelVerification.getInlineTable();
		if(inlineTable == null){
			throw new InvalidFeatureException(modelVerification);
		}

		Table<Integer, String, String> table = InlineTableUtil.getContent(inlineTable);

		List<Map<FieldName, Object>> records = Lists.newArrayList();

		Set<Integer> rowKeys = table.rowKeySet();
		for(Integer rowKey : rowKeys){
			Map<String, String> row = table.row(rowKey);

			Map<FieldName, Object> record = Maps.newLinkedHashMap();

			for(VerificationField verificationField : verificationFields){
				String field = verificationField.getField();
				String column = verificationField.getColumn();

				if(column == null){
					column = field;
				} // End if

				if(!row.containsKey(column)){
					continue;
				}

				record.put(FieldName.create(field), row.get(column));
			}

			records.add(record);
		}

		Integer recordCount = modelVerification.getRecordCount();
		if(recordCount != null && recordCount.intValue() != records.size()){
			throw new InvalidFeatureException(inlineTable);
		}

		List<FieldName> activeFields = getActiveFields();
		List<FieldName> groupFields = getGroupFields();

		if(groupFields.size() == 1){
			FieldName groupField = groupFields.get(0);

			records = EvaluatorUtil.groupRows(groupField, records);
		} else

		if(groupFields.size() > 1){
			throw new EvaluationException();
		}

		List<FieldName> targetFields = getTargetFields();
		List<FieldName> outputFields = getOutputFields();

		for(Map<FieldName, Object> record : records){
			Map<FieldName, Object> arguments = Maps.newLinkedHashMap();

			for(FieldName activeField : activeFields){
				arguments.put(activeField, EvaluatorUtil.prepare(this, activeField, record.get(activeField)));
			}

			Map<FieldName, ?> result = evaluate(arguments);

			SetView<FieldName> intersection = Sets.intersection(record.keySet(), ImmutableSet.copyOf(outputFields));

			// "If there exist VerificationField elements that refer to OutputField elements,
			// then any VerificationField element that refers to a MiningField element whose "usageType=target" should be ignored,
			// because they are considered to represent a dependent variable from the training data set, not an expected output"
			if(intersection.size() > 0){

				for(FieldName outputField : outputFields){
					VerificationField verificationField = fieldMap.get(outputField);

					if(verificationField == null){
						continue;
					}

					verify(record.get(outputField), result.get(outputField), verificationField.getPrecision(), verificationField.getZeroThreshold());
				}
			} else

			// "If there are no such VerificationField elements,
			// then any VerificationField element that refers to a MiningField element whose "usageType=target" should be considered to represent an expected output"
			{
				for(FieldName targetField : targetFields){
					VerificationField verificationField = fieldMap.get(targetField);

					if(verificationField == null){
						continue;
					}

					verify(record.get(targetField), EvaluatorUtil.decode(result.get(targetField)), verificationField.getPrecision(), verificationField.getZeroThreshold());
				}
			}
		}
	}

	/**
	 * @param expected A string or a collection of strings representing the expected value
	 * @param actual The actual value
	 */
	private void verify(Object expected, Object actual, double precision, double zeroThreshold){

		if(expected == null){
			return;
		} // End if

		if(!(actual instanceof Collection)){
			DataType dataType = TypeUtil.getDataType(actual);

			expected = TypeUtil.parseOrCast(dataType, expected);
		}

		boolean acceptable = VerificationUtil.acceptable(expected, actual, precision, zeroThreshold);
		if(!acceptable){
			throw new EvaluationException();
		}
	}

	public ModelEvaluationContext createContext(ModelEvaluationContext parent){
		return new ModelEvaluationContext(parent, this);
	}

	@Override
	public Map<FieldName, ?> evaluate(Map<FieldName, ?> arguments){
		List<FieldName> filterFields = getMiningFields(ModelEvaluator.FILTER_SET);

		ModelEvaluationContext context = createContext(null);
		context.declareAll(filterFields, arguments);

		return evaluate(context);
	}

	public <V> V getValue(LoadingCache<M, V> cache){
		M model = getModel();

		return CacheUtil.getValue(model, cache);
	}

	public <V> V getValue(Callable<? extends V> loader, Cache<M, V> cache){
		M model = getModel();

		return CacheUtil.getValue(model, loader, cache);
	}

	private static final EnumSet<FieldUsageType> FILTER_SET = EnumSet.of(FieldUsageType.ACTIVE, FieldUsageType.GROUP, FieldUsageType.ORDER);
}