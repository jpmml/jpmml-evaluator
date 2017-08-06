/*
 * Copyright (c) 2011 University of Tartu
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jpmml.evaluator;

import java.util.List;

import org.dmg.pmml.CompoundPredicate;
import org.dmg.pmml.False;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.SimpleSetPredicate;
import org.dmg.pmml.True;

public class PredicateUtil {

	private PredicateUtil(){
	}

	/**
	 * @return The {@link Boolean} value of the predicate, or <code>null</code> if the value is unknown.
	 */
	static
	public Boolean evaluate(Predicate predicate, EvaluationContext context){

		try {
			return evaluatePredicate(predicate, context);
		} catch(PMMLException pe){
			pe.ensureContext(predicate);

			throw pe;
		}
	}

	static
	Boolean evaluatePredicate(Predicate predicate, EvaluationContext context){

		if(predicate instanceof SimplePredicate){
			return evaluateSimplePredicate((SimplePredicate)predicate, context);
		} else

		if(predicate instanceof SimpleSetPredicate){
			return evaluateSimpleSetPredicate((SimpleSetPredicate)predicate, context);
		} else

		if(predicate instanceof CompoundPredicate){
			return evaluateCompoundPredicate((CompoundPredicate)predicate, context);
		} else

		if(predicate instanceof True){
			return evaluateTrue((True)predicate);
		} else

		if(predicate instanceof False){
			return evaluateFalse((False)predicate);
		} // End if

		if(predicate instanceof JavaPredicate){
			return evaluateJavaPredicate((JavaPredicate)predicate, context);
		}

		throw new UnsupportedFeatureException(predicate);
	}

	static
	public Boolean evaluateSimplePredicate(SimplePredicate simplePredicate, EvaluationContext context){
		SimplePredicate.Operator operator = simplePredicate.getOperator();
		if(operator == null){
			throw new InvalidFeatureException(simplePredicate);
		}

		String stringValue = simplePredicate.getValue();

		switch(operator){
			case IS_MISSING:
			case IS_NOT_MISSING:
				// "If the operator is isMissing or isNotMissing, then the value attribute must not appear"
				if(stringValue != null){
					throw new InvalidFeatureException(simplePredicate);
				}
				break;
			default:
				// "With all other operators, however, the value attribute is required"
				if(stringValue == null){
					throw new InvalidFeatureException(simplePredicate);
				}
				break;
		}

		FieldValue value = context.evaluate(simplePredicate.getField());

		switch(operator){
			case IS_MISSING:
				return Boolean.valueOf(value == null);
			case IS_NOT_MISSING:
				return Boolean.valueOf(value != null);
			default:
				break;
		}

		// "A SimplePredicate evaluates to unknwon if the input value is missing"
		if(value == null){
			return null;
		}

		switch(operator){
			case EQUAL:
				return value.equals(simplePredicate);
			case NOT_EQUAL:
				return !value.equals(simplePredicate);
			default:
				break;
		}

		int order = value.compareTo(simplePredicate);

		switch(operator){
			case LESS_THAN:
				return Boolean.valueOf(order < 0);
			case LESS_OR_EQUAL:
				return Boolean.valueOf(order <= 0);
			case GREATER_OR_EQUAL:
				return Boolean.valueOf(order >= 0);
			case GREATER_THAN:
				return Boolean.valueOf(order > 0);
			default:
				throw new UnsupportedFeatureException(simplePredicate, operator);
		}
	}

	static
	public Boolean evaluateSimpleSetPredicate(SimpleSetPredicate simpleSetPredicate, EvaluationContext context){
		SimpleSetPredicate.BooleanOperator booleanOperator = simpleSetPredicate.getBooleanOperator();
		if(booleanOperator == null){
			throw new InvalidFeatureException(simpleSetPredicate);
		}

		FieldValue value = context.evaluate(simpleSetPredicate.getField());
		if(value == null){
			return null;
		}

		switch(booleanOperator){
			case IS_IN:
				return value.isIn(simpleSetPredicate);
			case IS_NOT_IN:
				return !value.isIn(simpleSetPredicate);
			default:
				throw new UnsupportedFeatureException(simpleSetPredicate, booleanOperator);
		}
	}

	static
	public Boolean evaluateCompoundPredicate(CompoundPredicate compoundPredicate, EvaluationContext context){
		CompoundPredicateResult result = evaluateCompoundPredicateInternal(compoundPredicate, context);

		return result.getResult();
	}

	static
	public CompoundPredicateResult evaluateCompoundPredicateInternal(CompoundPredicate compoundPredicate, EvaluationContext context){
		CompoundPredicate.BooleanOperator booleanOperator = compoundPredicate.getBooleanOperator();
		if(booleanOperator == null){
			throw new InvalidFeatureException(compoundPredicate);
		}

		List<Predicate> predicates = compoundPredicate.getPredicates();
		if(predicates.size() < 2){
			throw new InvalidFeatureException(compoundPredicate);
		}

		Predicate predicate = predicates.get(0);

		Boolean result = evaluate(predicate, context);

		switch(booleanOperator){
			case AND:
			case OR:
			case XOR:
				break;
			case SURROGATE:
				if(result != null){
					return new CompoundPredicateResult(result, false);
				}
				break;
			default:
				throw new UnsupportedFeatureException(compoundPredicate, booleanOperator);
		}

		for(int i = 1, max = predicates.size(); i < max; i++){
			predicate = predicates.get(i);

			Boolean value = evaluate(predicate, context);

			switch(booleanOperator){
				case AND:
					result = PredicateUtil.binaryAnd(result, value);
					break;
				case OR:
					result = PredicateUtil.binaryOr(result, value);
					break;
				case XOR:
					result = PredicateUtil.binaryXor(result, value);
					break;
				case SURROGATE:
					if(value != null){
						return new CompoundPredicateResult(value, true);
					}
					break;
				default:
					throw new UnsupportedFeatureException(compoundPredicate, booleanOperator);
			}
		}

		return new CompoundPredicateResult(result, false);
	}

	static
	public Boolean evaluateTrue(True truePredicate){
		return Boolean.TRUE;
	}

	static
	public Boolean evaluateFalse(False falsePredicate){
		return Boolean.FALSE;
	}

	static
	public Boolean evaluateJavaPredicate(JavaPredicate javaPredicate, EvaluationContext context){
		Boolean result = javaPredicate.evaluate(context);

		return result;
	}

	static
	public Boolean binaryAnd(Boolean left, Boolean right){

		if(left == null){

			if(right == null || right.booleanValue()){
				return null;
			}

			return Boolean.FALSE;
		} else

		if(right == null){

			if(left == null || left.booleanValue()){
				return null;
			}

			return Boolean.FALSE;
		} else

		{
			return Boolean.valueOf(left.booleanValue() & right.booleanValue());
		}
	}

	static
	public Boolean binaryOr(Boolean left, Boolean right){

		if(left != null && left.booleanValue()){
			return Boolean.TRUE;
		} else

		if(right != null && right.booleanValue()){
			return Boolean.TRUE;
		} else

		if(left == null || right == null){
			return null;
		} else

		{
			return Boolean.valueOf(left.booleanValue() | right.booleanValue());
		}
	}

	static
	public Boolean binaryXor(Boolean left, Boolean right){

		if(left == null || right == null){
			return null;
		} else

		{
			return Boolean.valueOf(left.booleanValue() ^ right.booleanValue());
		}
	}

	static
	public class CompoundPredicateResult {

		private Boolean result = null;

		private boolean alternative = false;


		private CompoundPredicateResult(Boolean result, boolean alternative){
			setResult(result);
			setAlternative(alternative);
		}

		public Boolean getResult(){
			return this.result;
		}

		private void setResult(Boolean result){
			this.result = result;
		}

		public boolean isAlternative(){
			return this.alternative;
		}

		private void setAlternative(boolean alternative){
			this.alternative = alternative;
		}
	}
}