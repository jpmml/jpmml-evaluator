/*
 * Copyright (c) 2014 Villu Ruusmann
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

import org.dmg.pmml.regression.RegressionModel;
import org.jpmml.evaluator.FloatValue;
import org.jpmml.evaluator.ValueMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RegressionModelUtilTest {

	@Test
	public void computeBinomialProbabilities(){
		ValueMap<String, Float> values = new ValueMap<>();
		values.put("yes", new FloatValue(0.3f));
		values.put("no", new FloatValue(Float.MAX_VALUE));

		RegressionModelUtil.computeBinomialProbabilities(values, RegressionModel.NormalizationMethod.NONE);

		assertEquals(new FloatValue(0.3f), values.get("yes"));
		assertEquals(new FloatValue(1f - 0.3f), values.get("no"));
	}

	@Test
	public void computeMultinomialProbabilities(){
		ValueMap<String, Float> values = new ValueMap<>();
		values.put("red", new FloatValue(0.3f));
		values.put("yellow", new FloatValue(0.5f));
		values.put("green", new FloatValue(Float.MAX_VALUE));

		RegressionModelUtil.computeMultinomialProbabilities(values, RegressionModel.NormalizationMethod.NONE);

		assertEquals(new FloatValue(0.3f), values.get("red"));
		assertEquals(new FloatValue(0.5f), values.get("yellow"));
		assertEquals(new FloatValue(1f - (0.3f + 0.5f)), values.get("green"));
	}

	@Test
	public void computeOrdinalProbabilities(){
		ValueMap<String, Float> values = new ValueMap<>();
		values.put("loud", new FloatValue(0.2f));
		values.put("louder", new FloatValue(0.7f));
		values.put("insane", new FloatValue(1f));

		RegressionModelUtil.computeOrdinalProbabilities(values, RegressionModel.NormalizationMethod.NONE);

		assertEquals(new FloatValue(0.2f - 0f), values.get("loud"));
		assertEquals(new FloatValue(0.7f - 0.2f), values.get("louder"));
		assertEquals(new FloatValue(1f - 0.7f), values.get("insane"));
	}
}