package org.jpmml.evaluator;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import org.dmg.pmml.FieldName;

public class SparseEvaluatorConfig {

  private Function<EvaluationContext, Map<FieldName, Boolean>> cacheGenerator = context -> Collections.EMPTY_MAP;
  private Boolean defaultInclusion = false;

  public SparseEvaluatorConfig(Function<EvaluationContext, Map<FieldName, Boolean>> cacheGenerator, Boolean defaultInclusion) {
    this.cacheGenerator = cacheGenerator;
    this.defaultInclusion = defaultInclusion;
  }

  public Function<EvaluationContext, Map<FieldName, Boolean>> getCacheGenerator() {
    return cacheGenerator;
  }

  public void setCacheGenerator(Function<EvaluationContext, Map<FieldName, Boolean>> cacheGenerator) {
    this.cacheGenerator = cacheGenerator;
  }

  public Boolean getDefaultInclusion() {
    return defaultInclusion;
  }

  public void setDefaultInclusion(boolean defaultInclusion) {
    this.defaultInclusion = defaultInclusion;
  }

}
