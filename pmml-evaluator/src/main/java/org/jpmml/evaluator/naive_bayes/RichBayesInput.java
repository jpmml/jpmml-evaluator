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
package org.jpmml.evaluator.naive_bayes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.bind.annotation.XmlTransient;

import com.google.common.collect.ImmutableMap;
import org.dmg.pmml.DataType;
import org.dmg.pmml.naive_bayes.BayesInput;
import org.dmg.pmml.naive_bayes.PMMLAttributes;
import org.dmg.pmml.naive_bayes.PMMLElements;
import org.dmg.pmml.naive_bayes.PairCounts;
import org.dmg.pmml.naive_bayes.TargetValueCounts;
import org.jpmml.evaluator.MapHolder;
import org.jpmml.evaluator.MissingAttributeException;
import org.jpmml.evaluator.MissingElementException;
import org.jpmml.evaluator.TypeUtil;
import org.jpmml.model.ReflectionUtil;

public class RichBayesInput extends BayesInput implements MapHolder<TargetValueCounts> {

	@XmlTransient
	private DataType dataType = null;

	@XmlTransient
	private Map<?, TargetValueCounts> targetValueCountMap = null;


	private RichBayesInput(){
	}

	public RichBayesInput(DataType dataType){
		setDataType(dataType);
	}

	public RichBayesInput(DataType dataType, BayesInput bayesInput){
		setDataType(dataType);

		ReflectionUtil.copyState(bayesInput, this);
	}

	@Override
	public Map<?, TargetValueCounts> getMap(){

		if(this.targetValueCountMap == null){
			this.targetValueCountMap = ImmutableMap.copyOf(parsePairCounts());
		}

		return this.targetValueCountMap;
	}

	@Override
	public DataType getDataType(){
		return this.dataType;
	}

	private void setDataType(DataType dataType){
		this.dataType = Objects.requireNonNull(dataType);
	}

	private Map<?, TargetValueCounts> parsePairCounts(){
		DataType dataType = getDataType();

		Map<Object, TargetValueCounts> result = new LinkedHashMap<>();

		List<PairCounts> pairCounts = getPairCounts();
		for(PairCounts pairCount : pairCounts){
			Object category = pairCount.getValue();
			if(category == null){
				throw new MissingAttributeException(pairCount, PMMLAttributes.PAIRCOUNTS_VALUE);
			}

			Object value = TypeUtil.parseOrCast(dataType, category);

			TargetValueCounts targetValueCounts = pairCount.getTargetValueCounts();
			if(targetValueCounts == null){
				throw new MissingElementException(pairCount, PMMLElements.PAIRCOUNTS_TARGETVALUECOUNTS);
			}

			result.put(value, targetValueCounts);
		}

		return result;
	}
}