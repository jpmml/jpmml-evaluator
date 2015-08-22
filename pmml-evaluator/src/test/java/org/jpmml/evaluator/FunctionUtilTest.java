package org.jpmml.evaluator;

import java.util.Arrays;
import java.util.List;

import org.dmg.pmml.Apply;
import org.jpmml.evaluator.functions.EchoFunction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FunctionUtilTest {

	@Test
	public void evaluate(){
		Apply apply = new Apply(EchoFunction.class.getName());

		try {
			evaluate(apply);

			fail();
		} catch(FunctionException fe){
			assertEquals(apply, fe.getContext());
		}

		assertEquals("Hello World!", evaluate(apply, "Hello World!"));

		try {
			evaluate(apply, "Hello World!", "Hello World!");

			fail();
		} catch(FunctionException fe){
			assertEquals(apply, fe.getContext());
		}
	}

	static
	private Object evaluate(Apply apply, Object... arguments){
		EvaluationContext context = new VirtualEvaluationContext();

		List<FieldValue> values = FieldValueUtil.createAll(Arrays.asList(arguments));

		FieldValue result = FunctionUtil.evaluate(apply, values, context);

		return FieldValueUtil.getValue(result);
	}
}