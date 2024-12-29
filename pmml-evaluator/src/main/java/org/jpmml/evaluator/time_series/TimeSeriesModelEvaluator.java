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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.PMML;
import org.dmg.pmml.time_series.Algorithm;
import org.dmg.pmml.time_series.InterceptVector;
import org.dmg.pmml.time_series.MeasurementMatrix;
import org.dmg.pmml.time_series.StateSpaceModel;
import org.dmg.pmml.time_series.StateVector;
import org.dmg.pmml.time_series.TimeSeriesModel;
import org.jpmml.evaluator.ArrayUtil;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.HasOrderFields;
import org.jpmml.evaluator.HasSupplementaryFields;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.MatrixUtil;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.PMMLUtil;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.model.InvalidElementException;
import org.jpmml.model.UnsupportedAttributeException;
import org.jpmml.model.UnsupportedElementException;

public class TimeSeriesModelEvaluator extends ModelEvaluator<TimeSeriesModel> implements HasSupplementaryFields, HasOrderFields {

	private List<InputField> supplementaryInputFields = null;

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
	public List<InputField> getSupplementaryFields(){

		if(this.supplementaryInputFields == null){
			List<InputField> supplementaryInputFields = filterInputFields(createInputFields(MiningField.UsageType.SUPPLEMENTARY));

			this.supplementaryInputFields = ImmutableList.copyOf(supplementaryInputFields);
		}

		return this.supplementaryInputFields;
	}

	@Override
	public List<InputField> getOrderFields(){

		if(this.orderInputFields == null){
			List<InputField> orderInputFields = filterInputFields(createInputFields(MiningField.UsageType.ORDER));

			this.orderInputFields = ImmutableList.copyOf(orderInputFields);
		}

		return this.orderInputFields;
	}

	@Override
	protected <V extends Number> Map<String, ?> evaluateTimeSeries(ValueFactory<V> valueFactory, EvaluationContext context){
		TimeSeriesModel timeSeriesModel = getModel();

		TargetField targetField = getTargetField();

		Algorithm algorithm = getAlgorithm(timeSeriesModel);
		if(algorithm == null){
			throw new InvalidElementException(timeSeriesModel);
		}

		Object forecast = evaluateAlgorithm(algorithm, context);

		return Collections.singletonMap(targetField.getFieldName(), forecast);
	}

	private Object evaluateAlgorithm(Algorithm algorithm, EvaluationContext context){

		if(algorithm instanceof StateSpaceModel){
			return evaluateStateSpaceModel((StateSpaceModel)algorithm, context);
		}

		throw new UnsupportedElementException(algorithm);
	}

	private Double evaluateStateSpaceModel(StateSpaceModel stateSpaceModel, EvaluationContext context){
		StateVector stateVector = stateSpaceModel.requireStateVector();
		MeasurementMatrix measurementMatrix = stateSpaceModel.requireMeasurementMatrix();
		Number intercept = stateSpaceModel.getIntercept();
		InterceptVector interceptVector = stateSpaceModel.getInterceptVector();

		RealVector realStateVector = ArrayUtil.asRealVector(stateVector.requireArray());
		RealMatrix realMeasurementMatrix = MatrixUtil.asRealMatrix(measurementMatrix.requireMatrix());
		RealVector realInterceptVector;

		if(interceptVector != null){

			if(intercept != null && intercept.doubleValue() != 0d){
				throw new InvalidElementException(stateSpaceModel);
			}

			realInterceptVector = ArrayUtil.asRealVector(interceptVector.requireArray());
		} else

		{
			realInterceptVector = new ArrayRealVector(new double[]{intercept.doubleValue()}, false);
		}

		RealVector realObservableVector = (realMeasurementMatrix.operate(realStateVector)).add(realInterceptVector);

		return realObservableVector.getEntry(0);
	}

	static
	private Algorithm getAlgorithm(TimeSeriesModel timeSeriesModel){
		TimeSeriesModel.Algorithm bestFit = timeSeriesModel.requireBestFit();

		switch(bestFit){
			case ARIMA:
				return timeSeriesModel.getARIMA();
			case EXPONENTIAL_SMOOTHING:
				return timeSeriesModel.getExponentialSmoothing();
			case SEASONAL_TREND_DECOMPOSITION:
				return timeSeriesModel.getSeasonalTrendDecomposition();
			case SPECTRAL_ANALYSIS:
				return timeSeriesModel.getSpectralAnalysis();
			case STATE_SPACE_MODEL:
				return timeSeriesModel.getStateSpaceModel();
			case GARCH:
				return timeSeriesModel.getGARCH();
			default:
				throw new UnsupportedAttributeException(timeSeriesModel, bestFit);
		}
	}
}