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

import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Equivalence;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Visitor;
import org.jpmml.evaluator.visitors.InvalidFeatureInspector;
import org.jpmml.evaluator.visitors.UnsupportedFeatureInspector;
import org.jpmml.model.visitors.LocatorTransformer;

public class BatchUtil {

	private BatchUtil(){
	}

	static
	public List<Conflict> evaluate(Batch batch, Set<FieldName> ignoredFields, final double precision, final double zeroThreshold) throws Exception {
		ModelEvaluator<?> evaluator = (ModelEvaluator<?>)batch.getEvaluator();

		List<? extends Map<FieldName, ?>> input = batch.getInput();
		List<? extends Map<FieldName, ?>> output = batch.getOutput();

		PMML pmml = evaluator.getPMML();

		List<Visitor> visitors = Arrays.<Visitor>asList(new LocatorTransformer(), new InvalidFeatureInspector(), new UnsupportedFeatureInspector());
		for(Visitor visitor : visitors){
			visitor.applyTo(pmml);
		}

		ObjectOutputStream oos = new ObjectOutputStream(ByteStreams.nullOutputStream());

		try {
			oos.writeObject(evaluator);
		} finally {
			oos.close();
		}

		List<FieldName> groupFields = evaluator.getGroupFields();
		List<FieldName> targetFields = evaluator.getTargetFields();

		if(groupFields.size() == 1){
			FieldName groupField = groupFields.get(0);

			input = EvaluatorUtil.groupRows(groupField, input);
		} else

		if(groupFields.size() > 1){
			throw new EvaluationException();
		}

		Equivalence<Object> equivalence = new Equivalence<Object>(){

			@Override
			public boolean doEquivalent(Object expected, Object actual){
				actual = EvaluatorUtil.decode(actual);

				return VerificationUtil.acceptable(TypeUtil.parseOrCast(TypeUtil.getDataType(actual), expected), actual, precision, zeroThreshold);
			}

			@Override
			public int doHash(Object object){
				return object.hashCode();
			}
		};

		if(output.size() > 0){

			if(input.size() != output.size()){
				throw new EvaluationException();
			}

			List<Conflict> conflicts = new ArrayList<>();

			for(int i = 0; i < input.size(); i++){
				Map<FieldName, ?> arguments = input.get(i);

				Map<FieldName, ?> result = evaluator.evaluate(arguments);

				// Delete the default target field
				if(targetFields.size() == 0){
					result = new LinkedHashMap<>(result);

					result.remove(evaluator.getTargetField());
				} // End if

				if(ignoredFields != null && ignoredFields.size() > 0){
					result = new LinkedHashMap<>(result);

					Set<FieldName> fields = result.keySet();
					fields.removeAll(ignoredFields);
				}

				MapDifference<FieldName, ?> difference = Maps.<FieldName, Object>difference(output.get(i), result, equivalence);
				if(!difference.areEqual()){
					Conflict conflict = new Conflict(i, arguments, difference);

					conflicts.add(conflict);
				}
			}

			return conflicts;
		} else

		{
			for(int i = 0; i < input.size(); i++){
				Map<FieldName, ?> arguments = input.get(i);

				evaluator.evaluate(arguments);
			}

			return Collections.emptyList();
		}
	}

	/**
	 * <p>
	 * Evaluates the model using empty arguments.
	 * </p>
	 *
	 * @return The value of the target field.
	 */
	static
	public Object evaluateDefault(Batch batch) throws Exception {
		Evaluator evaluator = batch.getEvaluator();

		Map<FieldName, ?> arguments = Collections.emptyMap();

		Map<FieldName, ?> result = evaluator.evaluate(arguments);

		return result.get(evaluator.getTargetField());
	}

	static
	public class Conflict {

		private Integer id = null;

		private Map<FieldName, ?> arguments = null;

		private MapDifference<FieldName, ?> difference = null;


		public Conflict(Integer id, Map<FieldName, ?> arguments, MapDifference<FieldName, ?> difference){
			setId(id);
			setArguments(arguments);
			setDifference(difference);
		}

		@Override
		public String toString(){
			ToStringHelper helper = Objects.toStringHelper(this)
				.add("id", getId())
				.add("arguments", getArguments())
				.add("difference", getDifference());

			return helper.toString();
		}

		public Integer getId(){
			return this.id;
		}

		private void setId(Integer id){
			this.id = id;
		}

		public Map<FieldName, ?> getArguments(){
			return this.arguments;
		}

		private void setArguments(Map<FieldName, ?> arguments){
			this.arguments = arguments;
		}

		public MapDifference<FieldName, ?> getDifference(){
			return this.difference;
		}

		private void setDifference(MapDifference<FieldName, ?> difference){
			this.difference = difference;
		}
	}


}