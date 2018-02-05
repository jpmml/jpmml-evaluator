package org.jpmml.evaluator;

import org.dmg.pmml.Apply;
import org.dmg.pmml.Constant;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.jpmml.model.PMMLUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TransformerTest {
    private Transformer transformer;

    static private PMML readPMML(File file) throws Exception {
        try (InputStream is = new FileInputStream(file)) {
            return PMMLUtil.unmarshal(is);
        }
    }

    @Before
    public void setUp() throws Exception {
        transformer = new Transformer(readPMML(new File("src/test/resources/pmml/basicTransformation.pmml")));
    }

    @Test
    public void getSummary() {
        assertEquals(transformer.getSummary(), "Transformer");
    }

    @Test
    public void getDataField() {
        DataField xField = transformer.getDataField(new FieldName("X"));
        assertEquals(DataType.DOUBLE, xField.getDataType());
        assertEquals("X", xField.getName().getValue());
        assertEquals(OpType.CONTINUOUS, xField.getOpType());
    }

    @Test
    public void getDerivedField() {
        DerivedField derivedField = transformer.getDerivedField(new FieldName("Y1"));
        assertEquals("Y1", derivedField.getName().getValue());
        assertEquals(DataType.DOUBLE, derivedField.getDataType());
        assertEquals(OpType.CONTINUOUS, derivedField.getOpType());
        Expression exp = derivedField.getExpression();
        assert exp instanceof Constant;
        Constant cons = (Constant) exp;
        assertEquals("1.0", cons.getValue());

        derivedField = transformer.getDerivedField(new FieldName("spec"));
        assertEquals("spec", derivedField.getName().getValue());
        assertEquals(DataType.STRING, derivedField.getDataType());
        assertEquals(OpType.CATEGORICAL, derivedField.getOpType());
        exp = derivedField.getExpression();
        assert exp instanceof Constant;
        cons = (Constant) exp;
        assertEquals("CAT", cons.getValue());

        derivedField = transformer.getDerivedField(new FieldName("Y3"));
        assertEquals("Y3", derivedField.getName().getValue());
        assertEquals(DataType.DOUBLE, derivedField.getDataType());
        assertEquals(OpType.CONTINUOUS, derivedField.getOpType());
        exp = derivedField.getExpression();
        assert exp instanceof Apply;
    }

    @Test
    public void getTransformFields() {
        List<DerivedField> transformFields = transformer.getTransformFields();
        assertEquals(4, transformFields.size());
        assertEquals("Y1", transformFields.get(0).getName().getValue());
        assertEquals("spec", transformFields.get(1).getName().getValue());
        assertEquals("Y2", transformFields.get(2).getName().getValue());
        assertEquals("Y3", transformFields.get(3).getName().getValue());
    }

    @Test
    public void getArgumentFields() {
        List<DataField> argumentFields = transformer.getArgumentFields();
        assertEquals(2, argumentFields.size());
        assertEquals("X", argumentFields.get(0).getName().getValue());
        assertEquals("Z", argumentFields.get(1).getName().getValue());
    }

    @Test
    public void evaluate() {
        Map<String, String> requestArguments = new HashMap<>();
        requestArguments.put("X", "2.0");
        requestArguments.put("Z", "2");

        Map<FieldName, FieldValue> arguments = getArgumentsFromRequest(requestArguments);
        Map<FieldName, ?> result = transformer.evaluate(arguments);

        assertEquals(4, result.size());

        Object y1 = result.get(new FieldName("Y1"));
        assert y1 instanceof ContinuousValue;
        assertEquals(1.0, ((ContinuousValue) y1).getValue());

        Object spec = result.get(new FieldName("spec"));
        assert spec instanceof CategoricalValue;
        assertEquals("CAT", ((CategoricalValue) spec).getValue());

        Object y2 = result.get(new FieldName("Y2"));
        assert y2 instanceof ContinuousValue;
        assertEquals(2.0, ((ContinuousValue) y2).getValue());

        Object y3 = result.get(new FieldName("Y3"));
        assert y3 instanceof ContinuousValue;
        assertEquals(4.0, ((ContinuousValue) y3).getValue());

        requestArguments.put("Z", "4");
        arguments = getArgumentsFromRequest(requestArguments);
        result = transformer.evaluate(arguments);
        y3 = result.get(new FieldName("Y3"));
        assert y3 instanceof ContinuousValue;
        assertEquals(16.0, ((ContinuousValue) y3).getValue());
    }

    @Test(expected = FunctionException.class)
    public void evaluateMissingArgument() {
        Map<String, String> requestArguments = new HashMap<>();
        requestArguments.put("X", "2.0");

        Map<FieldName, FieldValue> arguments = getArgumentsFromRequest(requestArguments);
        transformer.evaluate(arguments);
    }

    private Map<FieldName, FieldValue> getArgumentsFromRequest(Map<String, String> requestArguments) {
        Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();
        List<DataField> argumentFields = transformer.getArgumentFields();
        for (DataField argumentField : argumentFields) {
            FieldName activeName = argumentField.getName();
            String key = activeName.getValue();
            Object value = requestArguments.get(key);

            FieldValue fieldValue = FieldValueUtil
                .create(argumentField.getDataType(), argumentField.getOpType(), value);

            arguments.put(activeName, fieldValue);
        }
        return arguments;
    }
}