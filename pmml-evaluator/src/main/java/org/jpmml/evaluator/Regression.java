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
package org.jpmml.evaluator;

import org.dmg.pmml.DataType;
import org.dmg.pmml.MiningFunction;

/**
 * @see MiningFunction#REGRESSION
 */
public class Regression<V extends Number> extends AbstractComputable implements HasPrediction {

	private Value<V> value = null;

	private Object result = null;


	protected Regression(Value<V> value){
		setValue(value);
	}

	@Override
	public Object getResult(){

		if(this.result == null){
			throw new EvaluationException("Regression result has not been computed");
		}

		return this.result;
	}

	protected void setResult(Object result){
		this.result = result;
	}

	protected void computeResult(DataType dataType){
		Value<V> value = getValue();

		Object result = TypeUtil.cast(dataType, value.getValue());

		setResult(result);
	}

	@Override
	public Object getPrediction(){
		return getResult();
	}

	@Override
	public Report getPredictionReport(){
		Value<V> value = getValue();

		return ReportUtil.getReport(value);
	}

	public Value<V> getValue(){
		return this.value;
	}

	private void setValue(Value<V> value){

		if(value == null){
			throw new IllegalArgumentException();
		}

		this.value = value;
	}
}