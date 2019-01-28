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

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.google.common.collect.ImmutableMap;
import org.dmg.pmml.naive_bayes.BayesInput;
import org.dmg.pmml.naive_bayes.PairCounts;
import org.dmg.pmml.naive_bayes.TargetValueCounts;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.HasParsedValueMapping;
import org.jpmml.evaluator.MissingAttributeException;
import org.jpmml.evaluator.MissingElementException;
import org.jpmml.evaluator.PMMLAttributes;
import org.jpmml.evaluator.PMMLElements;
import org.jpmml.evaluator.TypeInfo;
import org.jpmml.model.ReflectionUtil;

@XmlRootElement (
	name = "BayesInput"
)
public class RichBayesInput extends BayesInput implements HasParsedValueMapping<TargetValueCounts> {

	@XmlTransient
	private Map<FieldValue, TargetValueCounts> parsedValueMappings = null;


	public RichBayesInput(){
	}

	public RichBayesInput(BayesInput bayesInput){
		ReflectionUtil.copyState(bayesInput, this);
	}

	@Override
	public Map<FieldValue, TargetValueCounts> getValueMapping(TypeInfo typeInfo){

		if(this.parsedValueMappings == null){
			this.parsedValueMappings = ImmutableMap.copyOf(parsePairCounts(typeInfo));
		}

		return this.parsedValueMappings;
	}

	private Map<FieldValue, TargetValueCounts> parsePairCounts(TypeInfo typeInfo){
		Map<FieldValue, TargetValueCounts> result = new LinkedHashMap<>();

		List<PairCounts> pairCounts = getPairCounts();
		for(PairCounts pairCount : pairCounts){
			String category = pairCount.getValue();
			if(category == null){
				throw new MissingAttributeException(pairCount, PMMLAttributes.PAIRCOUNTS_VALUE);
			}

			FieldValue value = parse(typeInfo, category);

			TargetValueCounts targetValueCounts = pairCount.getTargetValueCounts();
			if(targetValueCounts == null){
				throw new MissingElementException(pairCount, PMMLElements.PAIRCOUNTS_TARGETVALUECOUNTS);
			}

			result.put(value, targetValueCounts);
		}

		return result;
	}
}