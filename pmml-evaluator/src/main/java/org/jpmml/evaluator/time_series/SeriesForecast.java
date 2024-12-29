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

import java.util.List;
import java.util.Objects;

import org.jpmml.evaluator.AbstractComputable;
import org.jpmml.model.ToStringHelper;

public class SeriesForecast extends AbstractComputable {

	private List<Double> values = null;


	protected SeriesForecast(List<Double> values){
		setValues(values);
	}

	@Override
	public Double getResult(){
		List<Double> values = getValues();

		return values.get(0);
	}

	@Override
	protected ToStringHelper toStringHelper(){
		ToStringHelper helper = super.toStringHelper()
			.add("values", getValues());

		return helper;
	}

	public List<Double> getValues(){
		return this.values;
	}

	private void setValues(List<Double> values){
		this.values = Objects.requireNonNull(values);
	}
}