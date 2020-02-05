package org.jpmml.evaluator;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;

abstract public class SparseModelEvaluator<M extends Model> extends ModelEvaluator<M> {

  protected SparseEvaluatorConfig sparseEvaluatorConfig;

  protected SparseModelEvaluator(PMML pmml, M model, SparseEvaluatorConfig sparseEvaluatorConfig) {
    super(pmml, model);
    this.sparseEvaluatorConfig = sparseEvaluatorConfig;
  }

}
