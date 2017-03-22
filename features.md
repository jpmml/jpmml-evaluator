Features
========

# Overview #

The JPMML-Evaluator library is *de facto* the reference implementation of the PMML specification for the Java platform.

The primary objective is to provide full compliance with 4.X versions of the PMML specification (released since 2009). The secondary objective is to provide maximum working compliance with 3.X versions of the PMML specification (released between 2004 and 2007). It means that some PMML features whose scope is limited to 3.X versions (eg. removed or deprecated in 4.X versions) may not be supported.

### General structure ###

The JPMML-Evaluator library is hardwired to perform thorough "sanity" checking. Model evaluator classes will throw an exception when an invalid and/or unsupported PMML feature is encountered.

##### Data flow #####

1. Pre-processing of active fields (aka independent variables) according to the [DataDictionary](http://www.dmg.org/pmml/v4-3/DataDictionary.html) and [MiningSchema](http://www.dmg.org/pmml/v4-3/MiningSchema.html) elements:
  * Strict data type system:
    * Except for `dateDaysSince[0]` and `dateTimeSecondsSince[0]` data types.
  * Strict operational type system.
  * Treatment of outlier, missing and/or invalid values.
2. Model evaluation.
3. Post-processing of target fields (aka dependent variables) according to the [Targets](http://www.dmg.org/pmml/v4-3/Targets.html) element:
  * Rescaling and/or casting regression results.
  * Replacing a missing regression result with the default value.
  * Replacing a missing classification result with the map of prior probabilities.
4. Calculation of auxiliary output fields according to the [Output](http://www.dmg.org/pmml/v4-3/Output.html) element:
  * Over 20 different result feature types:
    * Except for the `standardError` result feature.

##### Data manipulation #####

* [Transformations](http://www.dmg.org/pmml/v4-3/Transformations.html):
  * Except for the `Lag` element.
* [PMML built-in functions](http://www.dmg.org/pmml/v4-3/BuiltinFunctions.html):
  * Except for `erf`, `normalCDF`, `normalIDF`, `normalPDF`, `stdNormalCDF`, `stdNormalIDF` and `stdNormalPDF` functions.
* [PMML user-defined functions](http://www.dmg.org/pmml/v4-3/Functions.html).
* Java user-defined functions.

### Model types ###

Supported model types:

* [Assocation rules](http://www.dmg.org/pmml/v4-3/AssociationRules.html) (association).
* [Cluster model](http://www.dmg.org/pmml/v4-3/ClusteringModel.html) (clustering):
  * Except for the `distributionBased` value of the `modelClass` attribute.
* [General regression](http://www.dmg.org/pmml/v4-3/GeneralRegression.html) (regression, Cox regression, classification).
* [k-Nearest neighbors (k-NN)](http://www.dmg.org/pmml/v4-3/KNN.html) (regression, classification, clustering).
* [Naive Bayes](http://www.dmg.org/pmml/v4-3/NaiveBayes.html) (classification).
* [Neural network](http://www.dmg.org/pmml/v4-3/NeuralNetwork.html) (regression, classification):
  * Except for the `radialBasis` value of the `activationFunction` attribute.
* [Regression](http://www.dmg.org/pmml/v4-3/Regression.html) (regression, classification).
* [Rule set](http://www.dmg.org/pmml/v4-3/RuleSet.html) (classification).
* [Scorecard](http://www.dmg.org/pmml/v4-3/Scorecard.html) (regression).
* [Tree model](http://www.dmg.org/pmml/v4-3/TreeModel.html) (regression, classification):
  * Except for `aggregateNodes` and `weightedConfidence` values of the `missingValueStrategy` attribute.
* [Support Vector Machine (SVM)](http://www.dmg.org/pmml/v4-3/SupportVectorMachine.html) (regression, classification):
  * Except for the `Coefficients` value of the `svmRepresentation` attribute.
* [Ensemble model (ensembles of all of the above model types)](http://www.dmg.org/pmml/v4-3/MultipleModels.html) (regression, classification, clustering).

Not yet supported model types:

* [Baseline model](http://www.dmg.org/pmml/v4-3/BaselineModel.html)
* [Bayesian network](http://dmg.org/pmml/v4-3/BayesianNetwork.html)
* [Gaussian process](http://dmg.org/pmml/v4-3/GaussianProcess.html)
* [Sequence rules](http://www.dmg.org/pmml/v4-3/Sequence.html)
* [Text model](http://www.dmg.org/pmml/v4-3/Text.html)
* [Time series model](http://www.dmg.org/pmml/v4-3/TimeSeriesModel.html)

### Known limitations ###

* [Model composition](http://www.dmg.org/pmml/v4-3/MultipleModels.html). Model composition specifies a mechanism for embedding defeatured regression and decision tree models (represented by the `Regression` and `DecisionTree` elements, respectively) into other models. Model composition was deprecated in PMML schema version 4.1.
* The `ClusteringModel/CenterFields` element. This element was removed in PMML schema version 3.2. PMML producers should move the list of `DerivedField` child elements to the `ClusteringModel/LocalTransformations` element, and reference them using a list of `ClusteringField` elements instead.
* The `MiningModel/Segmentation/LocalTransformations` element. This element was deprecated in PMML schema version 4.1. PMML producers should move the list of `DerivedField` child elements to the `MiningModel/LocalTransformations` element instead.
* The `TableLocator` element. The `TableLocator` element specifies a mechanism for incorporating data from external data sources (eg. CSV files, databases). The `TableLocator` element is simply a placeholder in PMML schema version 4.3.

# Inspection API #

Starting from JPMML-Evaluator version 1.1.7, it is possible to inspect the class model object for unsupported PMML features using a visitor class `org.jpmml.evaluator.visitors.UnsupportedFeatureInspector` [(source)](https://github.com/jpmml/jpmml-evaluator/blob/master/pmml-evaluator/src/main/java/org/jpmml/evaluator/visitors/UnsupportedFeatureInspector.java). This visitor collects all unsupported features (viz. elements, attributes and enum-type attribute values) as instances of `org.jpmml.manager.UnsupportedFeatureException`.

The class model object is safe for evaluation using the JPMML-Evaluator library if the collection of exceptions is empty:
```java
public boolean isFullySupported(PMML pmml){
  UnsupportedFeatureInspector inspector = new UnsupportedFeatureInspector();

  // Traverse the specified class model object
  pmml.accept(inspector);

  List<UnsupportedFeatureException> exceptions = inspector.getExceptions();
  if(exceptions.isEmpty()){
    return true;
  }

  return false;
}
```

The visitor class traverses the class model object completely. In contrast, actual model evaluator classes traverse the class model object more or less partially, whereas every "evaluation path" is a function of the specified input data record. It follows that the collection of exceptions represents the worst case scenario. The evaluation using the JPMML-Evaluator library may succed even if this collection is not empty.
