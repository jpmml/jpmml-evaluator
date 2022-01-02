/*
 * Copyright (c) 2020 Villu Ruusmann
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
package org.jpmml.evaluator.visitors;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.dmg.pmml.Array;
import org.dmg.pmml.Cell;
import org.dmg.pmml.Constant;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Discretize;
import org.dmg.pmml.DiscretizeBin;
import org.dmg.pmml.Expression;
import org.dmg.pmml.Field;
import org.dmg.pmml.FieldColumnPair;
import org.dmg.pmml.HasDataType;
import org.dmg.pmml.HasDefaultValue;
import org.dmg.pmml.HasFieldReference;
import org.dmg.pmml.HasMapMissingTo;
import org.dmg.pmml.HasTable;
import org.dmg.pmml.HasValue;
import org.dmg.pmml.InlineTable;
import org.dmg.pmml.MapValues;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.NormDiscrete;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.Row;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.SimpleSetPredicate;
import org.dmg.pmml.Value;
import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.baseline.FieldValue;
import org.dmg.pmml.baseline.FieldValueCount;
import org.dmg.pmml.general_regression.BaselineStratum;
import org.dmg.pmml.general_regression.Category;
import org.dmg.pmml.general_regression.PPCell;
import org.dmg.pmml.naive_bayes.PairCounts;
import org.dmg.pmml.regression.CategoricalPredictor;
import org.jpmml.evaluator.ArrayUtil;
import org.jpmml.evaluator.ExpressionUtil;
import org.jpmml.evaluator.InlineTableUtil;
import org.jpmml.evaluator.RichComplexArray;
import org.jpmml.evaluator.TypeCheckException;
import org.jpmml.evaluator.TypeUtil;

public class ValueParser extends AbstractParser {

	private Mode mode = null;


	public ValueParser(){
		this(ValueParser.MODE_PROVIDER.get());
	}

	public ValueParser(Mode mode){
		setMode(mode);
	}

	@Override
	public VisitorAction visit(BaselineStratum baselineStartum){
		return super.visit(baselineStartum);
	}

	@Override
	public VisitorAction visit(CategoricalPredictor categoricalPredictor){
		parseValue(categoricalPredictor);

		return super.visit(categoricalPredictor);
	}

	@Override
	public VisitorAction visit(Category category){
		return super.visit(category);
	}

	@Override
	public VisitorAction visit(Constant constant){
		boolean missing = constant.isMissing();

		if(!missing){
			Object value = constant.getValue();

			if(!ExpressionUtil.isEmptyContent(value)){
				DataType dataType = constant.getDataType();
				if(dataType == null){
					dataType = TypeUtil.getConstantDataType(value);
				}

				value = parseOrCast(dataType, value);

				constant.setValue(value);
			}
		}

		return super.visit(constant);
	}

	@Override
	public VisitorAction visit(Discretize discretize){
		parseExpressionValues(discretize);

		return super.visit(discretize);
	}

	@Override
	public VisitorAction visit(DiscretizeBin discretizeBin){
		Discretize discretize = (Discretize)getParent();

		DataType dataType = discretize.getDataType();
		if(dataType != null){
			Object binValue = discretizeBin.getBinValue();
			if(binValue != null){
				binValue = parseOrCast(dataType, binValue);

				discretizeBin.setBinValue(binValue);
			}
		}

		return super.visit(discretizeBin);
	}

	@Override
	public VisitorAction visit(FieldValue fieldValue){
		return super.visit(fieldValue);
	}

	@Override
	public VisitorAction visit(FieldValueCount fieldValueCount){
		return super.visit(fieldValueCount);
	}

	@Override
	public VisitorAction visit(MapValues mapValues){
		parseExpressionValues(mapValues);

		Map<String, DataType> dataTypes = new HashMap<>();

		String outputColumn = mapValues.requireOutputColumn();

		DataType outputDataType = mapValues.getDataType();
		if(outputDataType != null){
			dataTypes.put(outputColumn, outputDataType);
		} // End if

		if(mapValues.hasFieldColumnPairs()){
			List<FieldColumnPair> fieldColumnPairs = mapValues.getFieldColumnPairs();

			for(FieldColumnPair fieldColumnPair : fieldColumnPairs){
				DataType dataType = resolveDataType(fieldColumnPair.requireField());

				dataTypes.put(fieldColumnPair.requireColumn(), dataType);
			}
		}

		parseCellValues(mapValues, dataTypes);

		return super.visit(mapValues);
	}

	@Override
	public VisitorAction visit(MiningField miningField){
		DataType dataType = resolveDataType(miningField.requireName());

		if(dataType != null){
			Object missingValueReplacement = miningField.getMissingValueReplacement();
			if(missingValueReplacement != null){
				missingValueReplacement = safeParseOrCast(dataType, missingValueReplacement);

				miningField.setMissingValueReplacement(missingValueReplacement);
			}

			Object invalidValueReplacement = miningField.getInvalidValueReplacement();
			if(invalidValueReplacement != null){
				invalidValueReplacement = safeParseOrCast(dataType, invalidValueReplacement);

				miningField.setInvalidValueReplacement(invalidValueReplacement);
			}
		}

		return super.visit(miningField);
	}

	@Override
	public VisitorAction visit(NormDiscrete normDiscrete){
		parseValue(normDiscrete);

		return super.visit(normDiscrete);
	}

	@Override
	public VisitorAction visit(PairCounts pairCounts){
		return super.visit(pairCounts);
	}

	@Override
	public VisitorAction visit(PPCell ppCell){
		parseValue(ppCell);

		return super.visit(ppCell);
	}

	@Override
	public VisitorAction visit(SimplePredicate simplePredicate){

		if(simplePredicate.hasValue()){
			parseValue(simplePredicate);
		}

		return super.visit(simplePredicate);
	}

	@Override
	public VisitorAction visit(SimpleSetPredicate simpleSetPredicate){
		DataType dataType = resolveDataType(simpleSetPredicate.requireField());

		if(dataType != null){
			Array array = simpleSetPredicate.requireArray();

			Set<?> values;

			Object value = array.getValue();

			if(value instanceof List){
				values = new LinkedHashSet<>((List<?>)value);
			} else

			if(value instanceof Set){
				values = (Set<?>)value;
			} else

			{
				values = new LinkedHashSet<>(ArrayUtil.parse(array));
			}

			try {
				array = new RichComplexArray(dataType)
					.setType(array.requireType())
					.setValue(values);
			} catch(IllegalArgumentException | TypeCheckException e){
				Mode mode = getMode();

				if(mode == Mode.LOOSE){
					return super.visit(simpleSetPredicate);
				}

				throw e;
			}

			simpleSetPredicate.setArray(array);
		}

		return super.visit(simpleSetPredicate);
	}

	@Override
	public VisitorAction visit(Value value){
		PMMLObject parent = getParent();

		if(parent instanceof Field){
			Field<?> field = (Field<?>)parent;

			DataType dataType = field.getDataType();
			if(dataType != null){
				Object objectValue = value.requireValue();

				objectValue = safeParseOrCast(dataType, objectValue);

				value.setValue(objectValue);
			}
		}

		return super.visit(value);
	}

	private <E extends PMMLObject & HasFieldReference<E> & HasValue<E>> void parseValue(E hasValue){
		DataType dataType = resolveDataType(hasValue.requireField());

		if(dataType != null){
			Object value = hasValue.requireValue();

			value = parseOrCast(dataType, value);

			hasValue.setValue(value);
		}
	}

	private <E extends Expression & HasDataType<E> & HasDefaultValue<E, Object> & HasMapMissingTo<E, Object>> void parseExpressionValues(E expression){
		DataType dataType = expression.getDataType();

		if(dataType != null){
			Object defaultValue = expression.getDefaultValue();
			if(defaultValue != null){
				defaultValue = parseOrCast(dataType, defaultValue);

				expression.setDefaultValue(defaultValue);
			}

			Object mapMissingTo = expression.getMapMissingTo();
			if(mapMissingTo != null){
				mapMissingTo = parseOrCast(dataType, mapMissingTo);

				expression.setMapMissingTo(mapMissingTo);
			}
		}
	}

	private <E extends PMMLObject & HasTable<E>> void parseCellValues(E hasTable, Map<String, DataType> dataTypes){
		InlineTable inlineTable = InlineTableUtil.getInlineTable(hasTable);

		if(inlineTable != null && inlineTable.hasRows()){
			List<Row> rows = inlineTable.getRows();

			for(Row row : rows){
				List<Object> cells = row.getContent();

				for(Object cell : cells){

					if(cell instanceof Cell){
						Cell pmmlCell = (Cell)cell;

						String column = InlineTableUtil.parseColumn(pmmlCell.getName());

						DataType dataType = dataTypes.get(column);
						if(dataType != null){
							Object value = pmmlCell.getValue();

							value = parseOrCast(dataType, value);

							pmmlCell.setValue(value);
						}
					}
				}
			}
		}
	}

	private Object parseOrCast(DataType dataType, Object value){

		try {
			return TypeUtil.parseOrCast(dataType, value);
		} catch(IllegalArgumentException | TypeCheckException e){

			if(this.mode == Mode.LOOSE){
				return value;
			}

			throw e;
		}
	}

	private Object safeParseOrCast(DataType dataType, Object value){

		try {
			return TypeUtil.parseOrCast(dataType, value);
		} catch(IllegalArgumentException | TypeCheckException e){
			return value;
		}
	}

	public Mode getMode(){
		return this.mode;
	}

	public void setMode(Mode mode){
		this.mode = Objects.requireNonNull(mode);
	}

	static
	public enum Mode {
		LOOSE,
		STRICT,
	}

	public static final ThreadLocal<Mode> MODE_PROVIDER = new ThreadLocal<Mode>(){

		@Override
		public Mode initialValue(){
			return Mode.LOOSE;
		}
	};
}