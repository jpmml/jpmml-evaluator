Features
========

# Summary #

The JPMML-Evaluator library is *de facto* the Reference Implementation (RI) of the Predictive Model Markup Language (PMML) for the Java platform.

The primary objective is to provide full compliance with 4.X versions of the PMML specification (released since 2009). The secondary objective is to provide maximum working compliance with 3.X versions of the PMML specification (released between 2004 and 2007). It means that some PMML features whose scope is limited to 3.X versions (eg. removed or deprecated in 4.X versions) are not supported.

### General structure ###

The JPMML-Evaluator library is hardwired to perform thorough "sanity" checking. Model evaluator classes will throw an exception when an invalid and/or unsupported PMML feature is encountered.

##### Data flow #####

1. Pre-processing of active fields (aka independent variables) according to [DataDictionary] (http://www.dmg.org/v4-2-1/DataDictionary.html) and [MiningSchema] (http://www.dmg.org/v4-2-1/MiningSchema.html) elements.
  * Strict data type system.
    * Except for `dateDaysSince[0]` and `dateTimeSecondsSince[0]` data types.
  * Strict operational type system.
  * Treatment of outlier, missing and/or invalid values.
2. Model evaluation.
3. Post-processing of target fields (aka dependent variables) according to the [Targets] (http://www.dmg.org/v4-2-1/Targets.html) element.
  * Rescaling and/or casting regression results.
  * Replacing a missing regression result with the default value.
  * Replacing a missing classification result with the map of prior probabilities.
4. Calculation of auxiliary output fields according to the [Output] (http://www.dmg.org/v4-2-1/Output.html) element.
  * Over 20 different result feature types.
    * Except for the `standardError` result feature.

##### Data manipulation #####

* [Transformations] (http://www.dmg.org/v4-2-1/Transformations.html).
  * Except for the `TextIndex` element.
* [PMML built-in functions] (http://www.dmg.org/v4-2-1/BuiltinFunctions.html).
* [PMML user-defined functions] (http://www.dmg.org/v4-2-1/Functions.html).
* Java user-defined functions.

### Model types ###

Supported model types:

* [Assocation rules] (http://www.dmg.org/v4-2-1/AssociationRules.html).
* [Cluster model] (http://www.dmg.org/v4-2-1/ClusteringModel.html).
  * Except for the `CenterFields` element (`ClusteringModel/CenterFields`). This element was removed in PMML schema version 3.2.
* [General regression] (http://www.dmg.org/v4-2-1/GeneralRegression.html) (regression, classification).
  * Except for the `cauchit`, `negbin` and `probit` values of the link function (`GeneralRegressionModel@linkFunction=cauchit|negbin|probit`).
  * Except for the `probit` value of the cumulative link function (`GeneralRegressionModel@cumulativeLinkFunction=probit`).
* [k-Nearest neighbors (k-NN)] (http://www.dmg.org/v4-2-1/KNN.html) (regression, classification, clustering).
  * Except for the `median` value of the continuous scoring method (`NearestNeighborModel@continuousScoringMethod=median`).
* [Naive Bayes] (http://www.dmg.org/v4-2-1/NaiveBayes.html).
  * Except for the `AnyDistribution`, `PoissonDistribution` and `UniformDistribution` child elements of the `TargetValueStat` element (`NaiveBayesModel/BayesInput/TargetValueStats/TargetValueStat/AnyDistribution|PoissonDistribution|UniformDistribution`).
* [Neural network] (http://www.dmg.org/v4-2-1/NeuralNetwork.html) (regression, classification).
  * Except for the `radialBasis` value of the activation function (`NeuralNetwork@activationFunction=radialBasis` and `NeuralLayer@activationFunction=radialBasis`).
* [Regression] (http://www.dmg.org/v4-2-1/Regression.html) (regression, classification).
  * Except for the `probit` value of the normalization method (`RegressionModel@normalizationMethod=probit`).
* [Rule set] (http://www.dmg.org/v4-2-1/RuleSet.html).
* [Scorecard] (http://www.dmg.org/v4-2-1/Scorecard.html).
* [Tree model] (http://www.dmg.org/v4-2-1/TreeModel.html).
  * Except for the `aggregateNodes` and `weightedConfidence` values of the missing value strategy (`TreeModel@missingValueStrategy=aggregateNodes|weightedConfidence`).
* [Support Vector Machine (SVM)] (http://www.dmg.org/v4-2-1/SupportVectorMachine.html) (regression, classification).
  * Except for the `Coefficients` value of the SVM model representation (`SupportVectorMachineModel@svmRepresentation=Coefficients`).
* [Ensemble model (ensembles of all of the above model types)] (http://www.dmg.org/v4-2-1/MultipleModels.html) (regression, classification, clustering).
  * Except for the `median` value of the multiple model method (`MiningModel/Segmentation@multipleModelMethod=median`).

Not yet supported model types:

* [Baseline model] (http://www.dmg.org/v4-2-1/BaselineModel.html).
* [Cox regression] (http://www.dmg.org/v4-2-1/GeneralRegression.html).
* [Sequence model] (http://www.dmg.org/v4-2-1/Sequence.html).
* [Text model] (http://www.dmg.org/v4-2-1/Text.html).
* [Time series model] (http://www.dmg.org/v4-2-1/TimeSeriesModel.html).

### Known limitations ###

* [Model composition] (http://www.dmg.org/v4-2-1/MultipleModels.html). Model composition specifies a mechanism for embedding defeatured regression and decision tree models (represented by the `Regression` and `DecisionTree` elements, respectively) into other models. This mechanism was deprecated in PMML schema version 4.1.
* The `MiningModel/Segmentation/LocalTransformations` element. This element was deprecated in PMML schema version 4.1. The definitions of `DerivedField` child elements should be simply moved over to the `MiningModel/LocalTransformations` element.
* [The `TableLocator` element] (http://www.dmg.org/v4-2-1/Taxonomy.html). This element specifies a mechanism for incorporating data from external data sources (eg. CSV files, databases). The PMML specification does not yet define its content.

# Inspection API #

Starting from JPMML-Evaluator version 1.1.7 it is possible to inspect the class model object for unsupported PMML features using a visitor class `org.jpmml.evaluator.UnsupportedFeatureInspector` [(source)] (https://github.com/jpmml/jpmml-evaluator/blob/master/pmml-evaluator/src/main/java/org/jpmml/evaluator/UnsupportedFeatureInspector.java). This visitor collects all unsupported features (viz. elements, attributes and enum-type attribute values) as instances of `org.jpmml.manager.UnsupportedFeatureException`.

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