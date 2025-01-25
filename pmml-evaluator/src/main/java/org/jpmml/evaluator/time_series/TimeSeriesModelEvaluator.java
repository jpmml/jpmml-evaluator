/*
 * Copyright (c) 2024 Villu Ruusmann
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
package org.jpmml.evaluator.time_series;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.dmg.pmml.Array;
import org.dmg.pmml.DataType;
import org.dmg.pmml.HasRequiredArray;
import org.dmg.pmml.HasRequiredMatrix;
import org.dmg.pmml.Matrix;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.PMML;
import org.dmg.pmml.time_series.Algorithm;
import org.dmg.pmml.time_series.InterceptVector;
import org.dmg.pmml.time_series.MeasurementMatrix;
import org.dmg.pmml.time_series.StateSpaceModel;
import org.dmg.pmml.time_series.StateVector;
import org.dmg.pmml.time_series.TimeSeriesModel;
import org.dmg.pmml.time_series.TransitionMatrix;
import org.jpmml.evaluator.ArrayUtil;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.HasOrderFields;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.MatrixUtil;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.PMMLUtil;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.model.InvalidElementException;
import org.jpmml.model.UnsupportedAttributeException;
import org.jpmml.model.UnsupportedElementException;

public class TimeSeriesModelEvaluator extends ModelEvaluator<TimeSeriesModel> implements HasOrderFields {

	private List<InputField> orderInputFields = null;


	private TimeSeriesModelEvaluator(){
	}

	public TimeSeriesModelEvaluator(PMML pmml){
		this(pmml, PMMLUtil.findModel(pmml, TimeSeriesModel.class));
	}

	public TimeSeriesModelEvaluator(PMML pmml, TimeSeriesModel timeSeriesModel){
		super(pmml, timeSeriesModel);
	}

	@Override
	public String getSummary(){
		return "Timeseries model";
	}

	@Override
	public List<InputField> getOrderFields(){

		if(this.orderInputFields == null){
			List<InputField> orderInputFields = createInputFields(EnumSet.of(MiningField.UsageType.ORDER));

			this.orderInputFields = ImmutableList.copyOf(orderInputFields);
		}

		return this.orderInputFields;
	}

	public int getForecastHorizon(EvaluationContext context){
		List<InputField> supplementaryFields = getSupplementaryFields();

		if(supplementaryFields.isEmpty()){
			return 1;
		} else

		if(supplementaryFields.size() == 1){
			InputField supplementaryField = Iterables.getOnlyElement(supplementaryFields);

			if(supplementaryField.getDataType() != DataType.INTEGER){
				// XXX
			}

			FieldValue value = context.evaluate(supplementaryField.getName());
			if(FieldValueUtil.isMissing(value)){
				return 1;
			}

			return value.asInteger();
		} else

		{
			throw createMiningSchemaException("Expected 0 or 1 supplementary fields, got " + supplementaryFields.size() + " supplementary fields");
		}
	}

	@Override
	protected <V extends Number> Map<String, ?> evaluateTimeSeries(ValueFactory<V> valueFactory, EvaluationContext context){
		TimeSeriesModel timeSeriesModel = getModel();

		TargetField targetField = getTargetField();

		Algorithm algorithm = getAlgorithm(timeSeriesModel);

		int forecastHorizon = getForecastHorizon(context);

		Object forecast = evaluateAlgorithm(algorithm, forecastHorizon, context);

		return Collections.singletonMap(targetField.getName(), forecast);
	}

	private Object evaluateAlgorithm(Algorithm algorithm, int forecastHorizon, EvaluationContext context){

		if(algorithm instanceof StateSpaceModel){
			return evaluateStateSpaceModel((StateSpaceModel)algorithm, forecastHorizon, context);
		}

		throw new UnsupportedElementException(algorithm);
	}

	private Object evaluateStateSpaceModel(StateSpaceModel stateSpaceModel, int forecastHorizon, EvaluationContext context){
		StateVector stateVector = stateSpaceModel.requireStateVector();
		MeasurementMatrix measurementMatrix = stateSpaceModel.requireMeasurementMatrix();
		Number intercept = stateSpaceModel.getIntercept();
		InterceptVector interceptVector = stateSpaceModel.getInterceptVector();

		RealVector realStateVector = parseArray(stateVector);
		RealMatrix realMeasurementMatrix = parseMatrix(measurementMatrix);
		RealVector realInterceptVector;

		if(interceptVector != null){

			if(intercept != null && intercept.doubleValue() != 0d){
				throw new InvalidElementException(stateSpaceModel);
			}

			realInterceptVector = parseArray(interceptVector);
		} else

		{
			realInterceptVector = new ArrayRealVector(new double[]{intercept.doubleValue()}, false);
		} // End if

		if(forecastHorizon == 1){
			RealVector realObservableVector = (realMeasurementMatrix.operate(realStateVector)).add(realInterceptVector);

			return realObservableVector.getEntry(0);
		} else

		{
			TransitionMatrix transitionMatrix = stateSpaceModel.requireTransitionMatrix();

			RealMatrix realTransitionMatrix = parseMatrix(transitionMatrix);

			List<Double> values = new ArrayList<>();

			for(int i = 0; i < forecastHorizon; i++){
				RealVector realObservableVector = (realMeasurementMatrix.operate(realStateVector)).add(realInterceptVector);

				values.add(realObservableVector.getEntry(0));

				if(i < (forecastHorizon - 1)){
					realStateVector = realTransitionMatrix.operate(realStateVector);
				}
			}

			return new SeriesForecast(values);
		}
	}

	static
	private Algorithm getAlgorithm(TimeSeriesModel timeSeriesModel){
		TimeSeriesModel.Algorithm bestFit = timeSeriesModel.requireBestFit();

		switch(bestFit){
			case ARIMA:
				return timeSeriesModel.requireARIMA();
			case EXPONENTIAL_SMOOTHING:
				return timeSeriesModel.requireExponentialSmoothing();
			case SEASONAL_TREND_DECOMPOSITION:
				return timeSeriesModel.requireSeasonalTrendDecomposition();
			case SPECTRAL_ANALYSIS:
				return timeSeriesModel.requireSpectralAnalysis();
			case STATE_SPACE_MODEL:
				return timeSeriesModel.requireStateSpaceModel();
			case GARCH:
				return timeSeriesModel.requireGARCH();
			default:
				throw new UnsupportedAttributeException(timeSeriesModel, bestFit);
		}
	}

	static
	private RealVector parseArray(HasRequiredArray<?> hasRequiredArray){
		Array array = hasRequiredArray.requireArray();

		return ArrayUtil.asRealVector(array);
	}

	static
	private RealMatrix parseMatrix(HasRequiredMatrix<?> hasRequiredMatrix){
		Matrix matrix = hasRequiredMatrix.requireMatrix();

		return MatrixUtil.asRealMatrix(matrix);
	}
}