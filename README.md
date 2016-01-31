JPMML-Evaluator [![Build Status](https://travis-ci.org/jpmml/jpmml-evaluator.png?branch=master)](https://travis-ci.org/jpmml/jpmml-evaluator)
===============

Java Evaluator API for Predictive Model Markup Language (PMML).

# Features #

JPMML-Evaluator is *de facto* the reference implementation of the PMML specification for the Java platform:

1. Pre-processing of active fields according to the [DataDictionary] (http://www.dmg.org/v4-2-1/DataDictionary.html) and [MiningSchema] (http://www.dmg.org/v4-2-1/MiningSchema.html) elements:
  * Complete data type system
  * Complete operational type system.
  * Treatment of outlier, missing and/or invalid values.
2. Model evaluation:
  * [Association rules] (http://www.dmg.org/v4-2-1/AssociationRules.html)
  * [Cluster model] (http://www.dmg.org/v4-2-1/ClusteringModel.html)
  * [General regression] (http://www.dmg.org/v4-2-1/GeneralRegression.html)
  * [Naive Bayes] (http://www.dmg.org/v4-2-1/NaiveBayes.html)
  * [k-Nearest neighbors] (http://www.dmg.org/v4-2-1/KNN.html)
  * [Neural network] (http://www.dmg.org/v4-2-1/NeuralNetwork.html)
  * [Regression] (http://www.dmg.org/v4-2-1/Regression.html)
  * [Rule set] (http://www.dmg.org/v4-2-1/RuleSet.html)
  * [Scorecard] (http://www.dmg.org/v4-2-1/Scorecard.html)
  * [Support Vector Machine] (http://www.dmg.org/v4-2-1/SupportVectorMachine.html)
  * [Tree model] (http://www.dmg.org/v4-2-1/TreeModel.html)
  * [Ensemble model] (http://www.dmg.org/v4-2-1/MultipleModels.html)
3. Post-processing of target fields according to the [Targets] (http://www.dmg.org/v4-2-1/Targets.html) element:
  * Rescaling and/or casting regression results.
  * Replacing a missing regression result with the default value.
  * Replacing a missing classification result with the map of prior probabilities.
4. Calculation of auxiliary output fields according to the [Output] (http://www.dmg.org/v4-2-1/Output.html) element:
  * Over 20 different result feature types.
5. Model verification according to the [ModelVerification] (http://www.dmg.org/v4-2-1/ModelVerification.html) element.

For more information please see the [features.md] (https://github.com/jpmml/jpmml-evaluator/blob/master/features.md) file.

JPMML-Evaluator has been tested with popular open-source PMML producer software:
  * [R] (http://www.r-project.org/) and [Rattle] (http://rattle.togaware.com/)
  * [KNIME] (http://www.knime.com/)
  * [RapidMiner] (http://rapidminer.com/products/rapidminer-studio/)

JPMML-Evaluator is thread safe and can easily deliver over one million scorings per second (on a single quad-core CPU) when working with simpler models.

# Prerequisites #

* Java 1.7 or newer

# Installation #

JPMML-Evaluator library JAR files (together with accompanying Java source and Javadocs JAR files) are released via [Maven Central Repository] (http://repo1.maven.org/maven2/org/jpmml/). Please join the [JPMML mailing list] (https://groups.google.com/forum/#!forum/jpmml) for release announcements.

The current version is **1.2.11** (1 February, 2016).

```xml
<dependency>
	<groupId>org.jpmml</groupId>
	<artifactId>pmml-evaluator</artifactId>
	<version>1.2.11</version>
</dependency>
```

# Usage #

JPMML-Evaluator depends on the [JPMML-Model] (https://github.com/jpmml/jpmml-model) library for PMML class model.

Loading a PMML schema version 3.X or 4.X document into an `org.dmg.pmml.PMML` instance:
```
PMML pmml;

try(InputStream is = ...){
	Source transformedSource = ImportFilter.apply(new InputSource(is));

	pmml = JAXBUtil.unmarshalPMML(transformedSource);
}
```

If the model type is known, then it is possible to instantiate the corresponding subclass of `org.jpmml.evaluator.ModelEvaluator` directly:
```java
PMML pmml = ...;

ModelEvaluator<TreeModel> modelEvaluator = new TreeModelEvaluator(pmml);
```

Otherwise, if the model type is unknown, then the model evaluator instantiation work should be delegated to an instance of class `org.jpmml.evaluator.ModelEvaluatorFactory`:
```java
PMML pmml = ...;

ModelEvaluatorFactory modelEvaluatorFactory = ModelEvaluatorFactory.newInstance();
 
ModelEvaluator<?> modelEvaluator = modelEvaluatorFactory.newModelManager(pmml);
```

Model evaluator classes follow functional programming principles and are completely thread safe.

Model evaluator instances are fairly lightweight, which makes them cheap to create and destroy. Nevertheless, long-running applications should maintain a one-to-one mapping between `PMML` and `ModelEvaluator` instances for better performance.

It is advisable for application code to work against the `org.jpmml.evaluator.Evaluator` interface:
```java
Evaluator evaluator = (Evaluator)modelEvaluator;
```

An evaluator instance can be queried for the definition of active (ie. independent), target (ie. primary dependent) and output (ie. secondary dependent) fields:
```java
List<FieldName> activeFields = evaluator.getActiveFields();
List<FieldName> targetFields = evaluator.getTargetFields();
List<FieldName> outputFields = evaluator.getOutputFields();
``` 

The PMML scoring operation must be invoked with valid arguments. Otherwise, the behaviour of the model evaluator class is unspecified.

The preparation of field values:
```java
Map<FieldName, FieldValue> arguments = new LinkedHashMap<FieldName, FieldValue>();

List<FieldName> activeFields = evaluator.getActiveFields();
for(FieldName activeField : activeFields){
	// The raw (ie. user-supplied) value could be any Java primitive value
	Object rawValue = ...;

	// The raw value is passed through: 1) outlier treatment, 2) missing value treatment, 3) invalid value treatment and 4) type conversion
	FieldValue activeValue = evaluator.prepare(activeField, rawValue);

	arguments.put(activeField, activeValue);
}
```

The scoring:
```java
Map<FieldName, ?> results = evaluator.evaluate(arguments);
```

Typically, a model has exactly one target field:
```java
FieldName targetName = evaluator.getTargetField();

Object targetValue = results.get(targetName);
```

The target value is either a Java primitive value (as a wrapper object) or an instance of `org.jpmml.evaluator.Computable`:
```java
if(targetValue instanceof Computable){
	Computable computable = (Computable)targetValue;

	Object primitiveValue = computable.getResult();
}
```

The target value may implement interfaces that descend from interface `org.jpmml.evaluator.ResultFeature`:
```java
// Test for "entityId" result feature
if(targetValue instanceof HasEntityId){
	HasEntityId hasEntityId = (HasEntityId)targetValue;
	HasEntityRegistry<?> hasEntityRegistry = (HasEntityRegistry<?>)evaluator;
	BiMap<String, ? extends Entity> entities = hasEntityRegistry.getEntityRegistry();
	Entity winner = entities.get(hasEntityId.getEntityId());

	// Test for "probability" result feature
	if(targetValue instanceof HasProbability){
		HasProbability hasProbability = (HasProbability)targetValue;
		Double winnerProbability = hasProbability.getProbability(winner.getId());
	}
}
```

# Example applications #

Module `pmml-evaluator-example` exemplifies the use of the JPMML-Evaluator library.

This module can be built using [Apache Maven] (http://maven.apache.org/):
```
mvn clean install
```

The resulting uber-JAR file `target/example-1.2-SNAPSHOT.jar` contains the following command-line applications:
* `org.jpmml.evaluator.EvaluationExample` [(source)] (https://github.com/jpmml/jpmml-evaluator/blob/master/pmml-evaluator-example/src/main/java/org/jpmml/evaluator/EvaluationExample.java). Evaluates a PMML model using data records from a TSV or CSV file.
* `org.jpmml.evaluator.EnhancementExample`. Enhances a PMML model with a ModelVerification element using data records from a TSV or CSV file.

Evaluating model `model.pmml` by loading input data records from `input.tsv` and storing output data records to `output.tsv`:
```
java -cp target/example-1.2-SNAPSHOT.jar org.jpmml.evaluator.EvaluationExample --model model.pmml --input input.tsv --output output.tsv
```

# Documentation #

The [Openscoring.io blog] (http://openscoring.io/blog/) contains fully worked out examples about using JPMML-Model and JPMML-Evaluator libraries.

Recommended reading:
* [Preparing arguments for evaluation] (http://openscoring.io/blog/2014/05/15/jpmml_evaluator_api_prepare_evaluate/)
* [Testing PMML Applications] (http://openscoring.io/blog/2014/05/12/testing_pmml_applications/)

# License #

JPMML-Evaluator is dual-licensed under the [GNU Affero General Public License (AGPL) version 3.0] (http://www.gnu.org/licenses/agpl-3.0.html) and a commercial license.

# Additional information #

Please contact [info@openscoring.io] (mailto:info@openscoring.io)