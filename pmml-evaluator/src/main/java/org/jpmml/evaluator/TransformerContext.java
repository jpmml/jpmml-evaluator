package org.jpmml.evaluator;

import org.dmg.pmml.DataField;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.FieldName;

import java.util.Collections;
import java.util.Map;

public class TransformerContext extends EvaluationContext {
    private final Transformer transformer;
    private Map<FieldName, ?> arguments = Collections.emptyMap();

    TransformerContext(Transformer transformer) {
        this.transformer = transformer;
    }

    @Override
    protected FieldValue createFieldValue(FieldName name, Object value) {
        throw new NotImplementedException();
    }

    public Map<FieldName, ?> getArguments() {
        return this.arguments;
    }

    public void setArguments(Map<FieldName, ?> arguments) {
        this.arguments = arguments;
    }

    @Override
    protected FieldValue resolve(FieldName name) {
        Transformer transformer = getTransformer();
        DataField dataField = transformer.getDataField(name);
        // Fields that either need not or must not be referenced in the MiningSchema element
        if (dataField == null) {
            DerivedField derivedField = transformer.getDerivedField(name);
            if (derivedField != null) {
                FieldValue value = ExpressionUtil.evaluateTypedExpressionContainer(derivedField, this);
                return declare(name, value);
            }
        } else

        // Fields that must be referenced in the DataDictionary element
        {
            Map<FieldName, ?> arguments = getArguments();
            Object value = arguments.get(name);
            if (value == null) {
                return declareMissing(name);
            }
            return declare(name, value);
        }

        throw new MissingFieldException(name);
    }

    /**
     * Declare a field as Missing one
     *
     * @param name The name of the field
     * @return The field value (would be 'null' also)
     */
    private FieldValue declareMissing(FieldName name) {
        // Casting should stay in place in order to avoid calling 'declare(FieldName,Object)'
        // which would result in exception
        return declare(name, (FieldValue) null);
    }

    @Override
    public void reset(boolean purge) {
        super.reset(purge);

        this.arguments = Collections.emptyMap();
    }

    private Transformer getTransformer() {
        return transformer;
    }
}
