/*
 * Copyright (c) 2016 Villu Ruusmann
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
package org.jpmml.evaluator;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.collect.Table;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Field;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.InlineTable;
import org.dmg.pmml.MathContext;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.Model;
import org.dmg.pmml.ModelVerification;
import org.dmg.pmml.PMML;
import org.dmg.pmml.PMMLElements;
import org.dmg.pmml.ResultFeature;
import org.dmg.pmml.VerificationField;
import org.dmg.pmml.VerificationFields;

/**
 * @see ModelEvaluatorBuilder
 */
@SuppressWarnings (
	value = {"unused"}
)
abstract
public class ModelEvaluator<M extends Model> extends ModelManager<M> implements Evaluator {

	private Configuration configuration = null;

	private InputMapper inputMapper = null;

	private ResultMapper resultMapper = null;

	private ValueFactory<?> valueFactory = null;

	private Boolean parentCompatible = null;

	private Boolean pure = null;

	private Integer numberOfVisibleFields = null;


	protected ModelEvaluator(){
	}

	protected ModelEvaluator(PMML pmml, M model){
		super(pmml, model);

		MathContext mathContext = model.getMathContext();
		switch(mathContext){
			case FLOAT:
			case DOUBLE:
				break;
			default:
				throw new UnsupportedAttributeException(model, mathContext);
		}
	}

	/**
	 * <p>
	 * Configures the runtime behaviour of this model evaluator.
	 * </p>
	 *
	 * <p>
	 * Must be called once before the first evaluation.
	 * May be called any number of times between subsequent evaluations.
	 * </p>
	 */
	public void configure(Configuration configuration){
		setConfiguration(Objects.requireNonNull(configuration));

		setValueFactory(null);

		resetInputFields();
		resetResultFields();
	}

	/**
	 * <p>
	 * Indicates if this model evaluator is compatible with its parent model evaluator.
	 * </p>
	 *
	 * <p>
	 * A parent compatible model evaluator inherits {@link DataField} declarations unchanged,
	 * which makes it possible to propagate {@link DataField} and global {@link DerivedField} values between evaluation contexts during evaluation.
	 * </p>
	 */
	public boolean isParentCompatible(){

		if(this.parentCompatible == null){
			this.parentCompatible = assessParentCompatibility();
		}

		return this.parentCompatible;
	}

	/**
	 * <p>
	 * Indicates if this model evaluator represents a pure function.
	 * </p>
	 *
	 * <p>
	 * A pure model evaluator does not tamper with the evaluation context during evaluation.
	 * </p>
	 */
	public boolean isPure(){

		if(this.pure == null){
			this.pure = assessPurity();
		}

		return this.pure;
	}

	protected int getNumberOfVisibleFields(){

		if(this.numberOfVisibleFields == null){
			ListMultimap<FieldName, Field<?>> visibleFields = getVisibleFields();

			this.numberOfVisibleFields = visibleFields.size();
		}

		return this.numberOfVisibleFields;
	}

	@Override
	public ModelEvaluator<M> verify(){
		M model = getModel();

		ModelVerification modelVerification = model.getModelVerification();
		if(modelVerification == null){
			return this;
		}

		VerificationBatch batch = parseModelVerification(modelVerification);

		List<? extends Map<FieldName, ?>> records = batch.getRecords();

		List<InputField> inputFields = getInputFields();

		if(this instanceof HasGroupFields){
			HasGroupFields hasGroupFields = (HasGroupFields)this;

			records = EvaluatorUtil.groupRows(hasGroupFields, records);
		}

		List<TargetField> targetFields = getTargetFields();
		List<OutputField> outputFields = getOutputFields();

		SetView<FieldName> intersection = Sets.intersection(batch.keySet(), new LinkedHashSet<>(Lists.transform(outputFields, OutputField::getFieldName)));

		boolean disjoint = intersection.isEmpty();

		for(Map<FieldName, ?> record : records){
			Map<FieldName, Object> arguments = new LinkedHashMap<>();

			for(InputField inputField : inputFields){
				FieldName name = inputField.getFieldName();

				FieldValue value = inputField.prepare(record.get(name));

				arguments.put(name, value);
			}

			ModelEvaluationContext context = createEvaluationContext();
			context.setArguments(arguments);

			Map<FieldName, ?> results = evaluateInternal(context);

			// "If there exist VerificationField elements that refer to OutputField elements,
			// then any VerificationField element that refers to a MiningField element whose "usageType=target" should be ignored,
			// because they are considered to represent a dependent variable from the training data set, not an expected output"
			if(!disjoint){

				for(OutputField outputField : outputFields){
					FieldName name = outputField.getFieldName();

					VerificationField verificationField = batch.get(name);
					if(verificationField == null){
						continue;
					}

					verify(record.get(name), results.get(name), verificationField.getPrecision(), verificationField.getZeroThreshold());
				}
			} else

			// "If there are no such VerificationField elements,
			// then any VerificationField element that refers to a MiningField element whose "usageType=target" should be considered to represent an expected output"
			{
				for(TargetField targetField : targetFields){
					FieldName name = targetField.getFieldName();

					VerificationField verificationField = batch.get(name);
					if(verificationField == null){
						continue;
					}

					Number precision = verificationField.getPrecision();
					Number zeroThreshold = verificationField.getZeroThreshold();

					verify(record.get(name), EvaluatorUtil.decode(results.get(name)), precision, zeroThreshold);
				}
			}
		}

		return this;
	}

	private void verify(Object expected, Object actual, Number precision, Number zeroThreshold){

		if(expected == null){
			return;
		} // End if

		if(actual instanceof Collection){
			// Ignored
		} else

		{
			DataType dataType = TypeUtil.getDataType(actual);

			expected = TypeUtil.parseOrCast(dataType, expected);
		}

		boolean acceptable = VerificationUtil.acceptable(expected, actual, precision.doubleValue(), zeroThreshold.doubleValue());
		if(!acceptable){
			throw new EvaluationException("Values " + PMMLException.formatValue(expected) + " and " + PMMLException.formatValue(actual) + " do not match");
		}
	}

	public ModelEvaluationContext createEvaluationContext(){
		return new ModelEvaluationContext(this);
	}

	@Override
	public Map<FieldName, ?> evaluate(Map<FieldName, ?> arguments){
		Configuration configuration = ensureConfiguration();

		SymbolTable<FieldName> prevDerivedFieldGuard = null;
		SymbolTable<FieldName> derivedFieldGuard = configuration.getDerivedFieldGuard();

		SymbolTable<String> prevFunctionGuard = null;
		SymbolTable<String> functionGuard = configuration.getFunctionGuard();

		arguments = processArguments(arguments);

		ModelEvaluationContext context = createEvaluationContext();
		context.setArguments(arguments);

		Map<FieldName, ?> results;

		try {
			if(derivedFieldGuard != null){
				prevDerivedFieldGuard = EvaluationContext.DERIVEDFIELD_GUARD_PROVIDER.get();

				EvaluationContext.DERIVEDFIELD_GUARD_PROVIDER.set(derivedFieldGuard.fork());
			} // End if

			if(functionGuard != null){
				prevFunctionGuard = EvaluationContext.FUNCTION_GUARD_PROVIDER.get();

				EvaluationContext.FUNCTION_GUARD_PROVIDER.set(functionGuard.fork());
			}

			results = evaluateInternal(context);
		} finally {

			if(derivedFieldGuard != null){
				EvaluationContext.DERIVEDFIELD_GUARD_PROVIDER.set(prevDerivedFieldGuard);
			} // End if

			if(functionGuard != null){
				EvaluationContext.FUNCTION_GUARD_PROVIDER.set(prevFunctionGuard);
			}
		}

		results = processResults(results);

		return results;
	}

	protected Map<FieldName, ?> processArguments(Map<FieldName, ?> arguments){
		InputMapper inputMapper = getInputMapper();

		if(inputMapper != null){
			Map<FieldName, Object> remappedArguments = new AbstractMap<FieldName, Object>(){

				@Override
				public Object get(Object key){
					return arguments.get(inputMapper.apply((FieldName)key));
				}

				@Override
				public Set<Map.Entry<FieldName, Object>> entrySet(){
					throw new UnsupportedOperationException();
				}
			};

			return remappedArguments;
		}

		return arguments;
	}

	protected Map<FieldName, ?> processResults(Map<FieldName, ?> results){
		ResultMapper resultMapper = getResultMapper();

		if(results instanceof OutputMap){
			OutputMap outputMap = (OutputMap)results;

			outputMap.clearPrivate();
		} // End if

		if(resultMapper != null){

			if(results.isEmpty()){
				return results;
			} else

			if(results.size() == 1){
				Map.Entry<FieldName, ?> entry = Iterables.getOnlyElement(results.entrySet());

				return Collections.singletonMap(resultMapper.apply(entry.getKey()), entry.getValue());
			}

			Map<FieldName, Object> remappedResults = new LinkedHashMap<>(2 * results.size());

			Collection<? extends Map.Entry<FieldName, ?>> entries = results.entrySet();
			for(Map.Entry<FieldName, ?> entry : entries){
				remappedResults.put(resultMapper.apply(entry.getKey()), entry.getValue());
			}

			return remappedResults;
		}

		return results;
	}

	@Override
	protected List<InputField> filterInputFields(List<InputField> inputFields){
		InputMapper inputMapper = getInputMapper();
		if(inputMapper != null){
			inputFields = updateNames(inputFields, inputMapper);
		}

		return inputFields;
	}

	@Override
	protected List<TargetField> filterTargetFields(List<TargetField> targetFields){
		ResultMapper resultMapper = getResultMapper();
		if(resultMapper != null){
			targetFields = updateNames(targetFields, resultMapper);
		}

		return targetFields;
	}

	@Override
	protected List<OutputField> filterOutputFields(List<OutputField> outputFields){
		ResultMapper resultMapper = getResultMapper();

		if(!outputFields.isEmpty()){
			OutputFilter outputFilter = ensureOutputFilter();

			Iterator<OutputField> it = outputFields.iterator();
			while(it.hasNext()){
				OutputField outputField = it.next();

				org.dmg.pmml.OutputField pmmlOutputField = outputField.getField();

				if(!outputFilter.test(pmmlOutputField)){
					it.remove();
				}
			}
		} // End if

		if(resultMapper != null){
			outputFields = updateNames(outputFields, resultMapper);
		}

		return outputFields;
	}

	public Map<FieldName, ?> evaluateInternal(ModelEvaluationContext context){
		M model = getModel();

		if(!model.isScorable()){
			throw new EvaluationException("Model is not scorable", model);
		}

		ValueFactory<?> valueFactory;

		MathContext mathContext = model.getMathContext();
		switch(mathContext){
			case FLOAT:
			case DOUBLE:
				valueFactory = ensureValueFactory();
				break;
			default:
				throw new UnsupportedAttributeException(model, mathContext);
		}

		Map<FieldName, ?> predictions;

		MiningFunction miningFunction = model.getMiningFunction();
		switch(miningFunction){
			case REGRESSION:
				predictions = evaluateRegression(valueFactory, context);
				break;
			case CLASSIFICATION:
				predictions = evaluateClassification(valueFactory, context);
				break;
			case CLUSTERING:
				predictions = evaluateClustering(valueFactory, context);
				break;
			case ASSOCIATION_RULES:
				predictions = evaluateAssociationRules(valueFactory, context);
				break;
			case SEQUENCES:
				predictions = evaluateSequences(valueFactory, context);
				break;
			case TIME_SERIES:
				predictions = evaluateTimeSeries(valueFactory, context);
				break;
			case MIXED:
				predictions = evaluateMixed(valueFactory, context);
				break;
			default:
				throw new UnsupportedAttributeException(model, miningFunction);
		}

		return OutputUtil.evaluate(predictions, context);
	}

	protected <V extends Number> Map<FieldName, ?> evaluateRegression(ValueFactory<V> valueFactory, EvaluationContext context){
		return evaluateDefault();
	}

	protected <V extends Number> Map<FieldName, ?> evaluateClassification(ValueFactory<V> valueFactory, EvaluationContext context){
		return evaluateDefault();
	}

	protected <V extends Number> Map<FieldName, ?> evaluateClustering(ValueFactory<V> valueFactory, EvaluationContext context){
		return evaluateDefault();
	}

	protected <V extends Number> Map<FieldName, ?> evaluateAssociationRules(ValueFactory<V> valueFactory, EvaluationContext context){
		return evaluateDefault();
	}

	protected <V extends Number> Map<FieldName, ?> evaluateSequences(ValueFactory<V> valueFactory, EvaluationContext context){
		return evaluateDefault();
	}

	protected <V extends Number> Map<FieldName, ?> evaluateTimeSeries(ValueFactory<V> valueFactory, EvaluationContext context){
		return evaluateDefault();
	}

	protected <V extends Number> Map<FieldName, ?> evaluateMixed(ValueFactory<V> valueFactory, EvaluationContext context){
		return evaluateDefault();
	}

	private <V extends Number> Map<FieldName, ?> evaluateDefault(){
		Model model = getModel();

		MiningFunction miningFunction = model.getMiningFunction();

		throw new InvalidAttributeException(model, miningFunction);
	}

	protected <V extends Number> Classification<Object, V> createClassification(ValueMap<Object, V> values){
		Set<ResultFeature> resultFeatures = getResultFeatures();

		if(resultFeatures.contains(ResultFeature.PROBABILITY) || resultFeatures.contains(ResultFeature.RESIDUAL)){
			return new ProbabilityDistribution<>(values);
		} else

		if(resultFeatures.contains(ResultFeature.CONFIDENCE)){
			return new ConfidenceDistribution<>(values);
		} else

		{
			return new VoteDistribution<>(values);
		}
	}

	protected boolean assessParentCompatibility(){
		List<InputField> inputFields = getInputFields();

		for(InputField inputField : inputFields){
			Field<?> field = inputField.getField();
			MiningField miningField = inputField.getMiningField();

			if(!(field instanceof DataField)){
				continue;
			} // End if

			if(!InputFieldUtil.isDefault(field, miningField)){
				return false;
			}
		}

		return true;
	}

	protected boolean assessPurity(){
		List<InputField> inputFields = getInputFields();

		for(InputField inputField : inputFields){
			Field<?> field = inputField.getField();
			MiningField miningField = inputField.getMiningField();

			if(!InputFieldUtil.isDefault(field, miningField)){
				return false;
			}
		}

		if(hasLocalDerivedFields() || hasOutputFields()){
			return false;
		}

		return true;
	}

	protected Configuration ensureConfiguration(){
		Configuration configuration = getConfiguration();

		if(this.configuration == null){
			throw new IllegalStateException();
		}

		return this.configuration;
	}

	protected ModelEvaluatorFactory ensureModelEvaluatorFactory(){
		Configuration configuration = ensureConfiguration();

		return configuration.getModelEvaluatorFactory();
	}

	protected ValueFactoryFactory ensureValueFactoryFactory(){
		Configuration configuration = ensureConfiguration();

		return configuration.getValueFactoryFactory();
	}

	protected OutputFilter ensureOutputFilter(){
		Configuration configuration = ensureConfiguration();

		return configuration.getOutputFilter();
	}

	protected ValueFactory<?> ensureValueFactory(){
		ValueFactory<?> valueFactory = getValueFactory();

		if(valueFactory == null){
			ValueFactoryFactory valueFactoryFactory = ensureValueFactoryFactory();

			MathContext mathContext = getMathContext();

			valueFactory = valueFactoryFactory.newValueFactory(mathContext);

			setValueFactory(valueFactory);
		}

		return valueFactory;
	}

	public Configuration getConfiguration(){
		return this.configuration;
	}

	private void setConfiguration(Configuration configuration){
		this.configuration = configuration;
	}

	public InputMapper getInputMapper(){
		return this.inputMapper;
	}

	void setInputMapper(InputMapper inputMapper){
		this.inputMapper = inputMapper;

		resetInputFields();
	}

	public ResultMapper getResultMapper(){
		return this.resultMapper;
	}

	void setResultMapper(ResultMapper resultMapper){
		this.resultMapper = resultMapper;

		resetResultFields();
	}

	private ValueFactory<?> getValueFactory(){
		return this.valueFactory;
	}

	private void setValueFactory(ValueFactory<?> valueFactory){
		this.valueFactory = valueFactory;
	}

	static
	private <F extends ModelField> List<F> updateNames(List<F> fields, com.google.common.base.Function<FieldName, FieldName> mapper){

		for(F field : fields){
			FieldName name = field.getFieldName();

			FieldName mappedName = mapper.apply(name);
			if(mappedName != null && !Objects.equals(mappedName, name)){
				field.setName(mappedName);
			}
		}

		return fields;
	}

	static
	private VerificationBatch parseModelVerification(ModelVerification modelVerification){
		VerificationBatch result = new VerificationBatch();

		VerificationFields verificationFields = modelVerification.getVerificationFields();
		if(verificationFields == null){
			throw new MissingElementException(modelVerification, PMMLElements.MODELVERIFICATION_VERIFICATIONFIELDS);
		}

		for(VerificationField verificationField : verificationFields){
			result.put(verificationField.getField(), verificationField);
		}

		InlineTable inlineTable = modelVerification.getInlineTable();
		if(inlineTable == null){
			throw new MissingElementException(modelVerification, PMMLElements.MODELVERIFICATION_INLINETABLE);
		}

		Table<Integer, String, Object> table = InlineTableUtil.getContent(inlineTable);

		List<Map<FieldName, Object>> records = new ArrayList<>();

		Set<Integer> rowKeys = table.rowKeySet();
		for(Integer rowKey : rowKeys){
			Map<String, Object> row = table.row(rowKey);

			Map<FieldName, Object> record = new LinkedHashMap<>();

			for(VerificationField verificationField : verificationFields){
				FieldName name = verificationField.getField();
				String column = verificationField.getColumn();

				if(column == null){
					column = name.getValue();
				} // End if

				if(!row.containsKey(column)){
					continue;
				}

				record.put(name, row.get(column));
			}

			records.add(record);
		}

		Integer recordCount = modelVerification.getRecordCount();
		if(recordCount != null && recordCount != records.size()){
			throw new InvalidElementException(modelVerification);
		}

		result.setRecords(records);

		return result;
	}

	static
	private class VerificationBatch extends LinkedHashMap<FieldName, VerificationField> {

		private List<Map<FieldName, Object>> records = null;


		public List<Map<FieldName, Object>> getRecords(){
			return this.records;
		}

		private void setRecords(List<Map<FieldName, Object>> records){
			this.records = records;
		}
	}
}
