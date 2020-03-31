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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.dmg.pmml.Array;
import org.dmg.pmml.Constant;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Discretize;
import org.dmg.pmml.DiscretizeBin;
import org.dmg.pmml.Expression;
import org.dmg.pmml.Field;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.HasDataType;
import org.dmg.pmml.HasDefaultValue;
import org.dmg.pmml.HasMapMissingTo;
import org.dmg.pmml.HasValue;
import org.dmg.pmml.MapValues;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.NormDiscrete;
import org.dmg.pmml.PMMLAttributes;
import org.dmg.pmml.PMMLElements;
import org.dmg.pmml.PMMLObject;
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
import org.jpmml.evaluator.MissingAttributeException;
import org.jpmml.evaluator.MissingElementException;
import org.jpmml.evaluator.RichComplexArray;
import org.jpmml.evaluator.TypeCheckException;
import org.jpmml.evaluator.TypeUtil;
import org.jpmml.model.XPathUtil;

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
		FieldName name = categoricalPredictor.getField();
		if(name == null){
			throw new MissingAttributeException(categoricalPredictor, org.dmg.pmml.regression.PMMLAttributes.CATEGORICALPREDICTOR_FIELD);
		}

		parseValue(name, categoricalPredictor);

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

		return super.visit(mapValues);
	}

	@Override
	public VisitorAction visit(MiningField miningField){
		FieldName name = miningField.getName();
		if(name == null){
			throw new MissingAttributeException(miningField, PMMLAttributes.MININGFIELD_NAME);
		}

		DataType dataType = resolveDataType(name);
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
		FieldName name = normDiscrete.getField();
		if(name == null){
			throw new MissingAttributeException(normDiscrete, PMMLAttributes.NORMDISCRETE_FIELD);
		}

		parseValue(name, normDiscrete);

		return super.visit(normDiscrete);
	}

	@Override
	public VisitorAction visit(PairCounts pairCounts){
		return super.visit(pairCounts);
	}

	@Override
	public VisitorAction visit(PPCell ppCell){
		FieldName name = ppCell.getField();
		if(name == null){
			throw new MissingAttributeException(ppCell, org.dmg.pmml.general_regression.PMMLAttributes.PPCELL_FIELD);
		}

		parseValue(name, ppCell);

		return super.visit(ppCell);
	}

	@Override
	public VisitorAction visit(SimplePredicate simplePredicate){
		FieldName name = simplePredicate.getField();
		if(name == null){
			throw new MissingAttributeException(simplePredicate, PMMLAttributes.SIMPLEPREDICATE_FIELD);
		} // End if

		if(simplePredicate.hasValue()){
			parseValue(name, simplePredicate);
		}

		return super.visit(simplePredicate);
	}

	@Override
	public VisitorAction visit(SimpleSetPredicate simpleSetPredicate){
		FieldName name = simpleSetPredicate.getField();
		if(name == null){
			throw new MissingAttributeException(simpleSetPredicate, PMMLAttributes.SIMPLESETPREDICATE_FIELD);
		}

		Array array = simpleSetPredicate.getArray();
		if(array == null){
			throw new MissingElementException(simpleSetPredicate, PMMLElements.SIMPLESETPREDICATE_ARRAY);
		}

		DataType dataType = resolveDataType(name);
		if(dataType != null){
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
					.setType(array.getType())
					.setValue(values);
			} catch(IllegalArgumentException | TypeCheckException e){
				Mode mode = getMode();

				if((Mode.LOOSE).equals(mode)){
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

		Object simpleValue = value.getValue();
		if(simpleValue == null){
			throw new MissingAttributeException(value, PMMLAttributes.VALUE_VALUE);
		} // End if

		if(parent instanceof Field){
			Field<?> field = (Field<?>)parent;

			DataType dataType = field.getDataType();
			if(dataType != null){
				simpleValue = safeParseOrCast(dataType, simpleValue);

				value.setValue(simpleValue);
			}
		}

		return super.visit(value);
	}

	private <E extends PMMLObject & HasValue<E>> void parseValue(FieldName name, E hasValue){
		Object value = hasValue.getValue();
		if(value == null){
			throw new MissingAttributeException(MissingAttributeException.formatMessage(XPathUtil.formatElement(hasValue.getClass()) + "@value"), hasValue);
		}

		DataType dataType = resolveDataType(name);
		if(dataType != null){
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

	private Object parseOrCast(DataType dataType, Object value){

		try {
			return TypeUtil.parseOrCast(dataType, value);
		} catch(IllegalArgumentException | TypeCheckException e){

			if((Mode.LOOSE).equals(this.mode)){
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