package org.jpmml.evaluator;

import java.util.*;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.dmg.pmml.*;

public class Transformer {

    private final PMML pmml;

    private final Map<FieldName, DataField> dataFields;

    private final Map<FieldName, DerivedField> derivedFields;


    public Transformer(PMML pmml) {
        Objects.requireNonNull(pmml);
        this.pmml = pmml;

        DataDictionary dataDictionary = pmml.getDataDictionary();
        if (dataDictionary == null) {
            throw new MissingElementException(pmml, PMMLElements.PMML_DATADICTIONARY);
        } // End if

        if (dataDictionary.hasDataFields()) {
            this.dataFields = CacheUtil.getValue(dataDictionary, Transformer.dataFieldCache);
        } else {
            this.dataFields = Collections.emptyMap();
        }

        TransformationDictionary transformationDictionary = pmml.getTransformationDictionary();
        if (transformationDictionary != null && transformationDictionary.hasDerivedFields()) {
            this.derivedFields = CacheUtil.getValue(transformationDictionary, Transformer.derivedFieldCache);
        } else {
            this.derivedFields = Collections.emptyMap();
        }
    }

    /**
     * <p>
     * Gets a short description of the {@link Transformer}.
     * </p>
     */
    public String getSummary(){
        return "Transformer";
    }

    public DataField getDataField(FieldName name) {
        return this.dataFields.get(name);
    }

    public DerivedField getDerivedField(FieldName name) {
        return this.derivedFields.get(name);
    }

    private static final LoadingCache<DataDictionary, Map<FieldName, DataField>> dataFieldCache = CacheUtil.buildLoadingCache(new CacheLoader<DataDictionary, Map<FieldName, DataField>>() {

        @Override
        public Map<FieldName, DataField> load(DataDictionary dataDictionary) {
            return IndexableUtil.buildMap(dataDictionary.getDataFields());
        }
    });

    private static final LoadingCache<TransformationDictionary, Map<FieldName, DerivedField>> derivedFieldCache = CacheUtil.buildLoadingCache(new CacheLoader<TransformationDictionary, Map<FieldName, DerivedField>>() {

        @Override
        public Map<FieldName, DerivedField> load(TransformationDictionary transformationDictionary) {
            return IndexableUtil.buildMap(transformationDictionary.getDerivedFields());
        }
    });

    /**
     * <p>
     * Gets the transformed output fields.
     * </p>
     */
    public List<DerivedField> getTransformFields() {
        return new LinkedList<>(this.derivedFields.values());
    }

    public List<DataField> getArgumentFields() {
        return new LinkedList<>(this.dataFields.values());
    }

    public Map<FieldName, ?> evaluate(Map<FieldName, ?> arguments) {
        TransformerContext context = new TransformerContext(this);
        context.setArguments(arguments);

        return evaluate(context);
    }

    public Map<FieldName, ?> evaluate(TransformerContext context) {
        Map<FieldName, Object> result = new LinkedHashMap<>();
        List<DerivedField> derivedFields = new LinkedList<>(getTransformFields());
        for (DerivedField derivedField : derivedFields) {
            FieldValue value = context.evaluate(derivedField.getName());
            result.put(derivedField.getName(), value);
        }
        return result;
    }

    public PMML getPMML() {
        return pmml;
    }
}
