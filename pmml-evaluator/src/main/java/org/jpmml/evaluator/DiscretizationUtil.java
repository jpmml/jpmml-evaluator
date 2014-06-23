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
package org.jpmml.evaluator;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Table;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Discretize;
import org.dmg.pmml.DiscretizeBin;
import org.dmg.pmml.InlineTable;
import org.dmg.pmml.Interval;
import org.dmg.pmml.MapValues;
import org.dmg.pmml.TableLocator;
import org.jpmml.manager.UnsupportedFeatureException;

public class DiscretizationUtil {

	private DiscretizationUtil(){
	}

	static
	public FieldValue discretize(Discretize discretize, FieldValue value){
		String result = discretize(discretize, (value.asNumber()).doubleValue());

		return FieldValueUtil.create(discretize.getDataType(), null, result);
	}

	static
	public String discretize(Discretize discretize, double value){
		List<DiscretizeBin> bins = discretize.getDiscretizeBins();

		for(DiscretizeBin bin : bins){
			Interval interval = bin.getInterval();

			if(contains(interval, value)){
				return bin.getBinValue();
			}
		}

		return discretize.getDefaultValue();
	}

	static
	public boolean contains(Interval interval, double value){
		Double left = interval.getLeftMargin();
		Double right = interval.getRightMargin();

		Interval.Closure closure = interval.getClosure();
		switch(closure){
			case OPEN_CLOSED:
				return greaterThan(value, left) && lessOrEqual(value, right);
			case OPEN_OPEN:
				return greaterThan(value, left) && lessThan(value, right);
			case CLOSED_OPEN:
				return greaterOrEqual(value, left) && lessThan(value, right);
			case CLOSED_CLOSED:
				return greaterOrEqual(value, left) && lessOrEqual(value, right);
			default:
				throw new UnsupportedFeatureException(interval, closure);
		}
	}

	static
	private boolean lessThan(double value, Double reference){
		return reference == null || Double.compare(value, reference) < 0;
	}

	static
	private boolean lessOrEqual(double value, Double reference){
		return reference == null || Double.compare(value, reference) <= 0;
	}

	static
	private boolean greaterThan(double value, Double reference){
		return reference == null || Double.compare(value, reference) > 0;
	}

	static
	private boolean greaterOrEqual(double value, Double reference){
		return reference == null || Double.compare(value, reference) >= 0;
	}

	static
	public FieldValue mapValue(MapValues mapValues, Map<String, FieldValue> values){
		DataType dataType = mapValues.getDataType();

		TableLocator tableLocator = mapValues.getTableLocator();
		if(tableLocator != null){
			throw new UnsupportedFeatureException(tableLocator);
		}

		InlineTable inlineTable = mapValues.getInlineTable();
		if(inlineTable != null){
			Table<Integer, String, String> table = InlineTableUtil.getContent(inlineTable);

			Map<String, String> row = InlineTableUtil.match(table, values);
			if(row != null){
				String result = row.get(mapValues.getOutputColumn());
				if(result == null){
					throw new EvaluationException(mapValues);
				}

				return FieldValueUtil.create(dataType, null, result);
			}
		}

		return FieldValueUtil.create(dataType, null, mapValues.getDefaultValue());
	}
}