JPMML-Evaluator [![Build Status](https://travis-ci.org/jpmml/jpmml-evaluator.png?branch=master)](https://travis-ci.org/jpmml/jpmml-evaluator)
===============

Java Evaluator API for Predictive Model Markup Language (PMML).

# Features #

* Full support for [DataDictionary] (http://www.dmg.org/v4-2/DataDictionary.html) and [MiningSchema] (http://www.dmg.org/v4-2/MiningSchema.html) elements:
  * Complete data type system.
  * Complete operational type system. For example, continuous integers, categorical integers and ordinal integers are handled differently in equality check and comparison operations.
  * Detection and treatment of outlier, missing and invalid values.
* Full support for [transformations] (http://www.dmg.org/v4-2/Transformations.html) and [functions] (http://www.dmg.org/v4-2/Functions.html):
  * Built-in functions.
  * User defined functions (PMML, Java).
* Full support for [Targets] (http://www.dmg.org/v4-2/Targets.html) and [Output] (http://www.dmg.org/v4-2/Output.html) elements.
* Fully supported model elements:
  * [Association rules] (http://www.dmg.org/v4-2/AssociationRules.html)
  * [Cluster model] (http://www.dmg.org/v4-2/ClusteringModel.html)
  * [General regression] (http://www.dmg.org/v4-2/GeneralRegression.html)
  * [Naive Bayes] (http://www.dmg.org/v4-2/NaiveBayes.html)
  * [k-Nearest neighbors] (http://www.dmg.org/v4-2/KNN.html)
  * [Neural network] (http://www.dmg.org/v4-2/NeuralNetwork.html)
  * [Regression] (http://www.dmg.org/v4-2/Regression.html)
  * [Rule set] (http://www.dmg.org/v4-2/RuleSet.html)
  * [Scorecard] (http://www.dmg.org/v4-2/Scorecard.html)
  * [Support Vector Machine] (http://www.dmg.org/v4-2/SupportVectorMachine.html)
  * [Tree model] (http://www.dmg.org/v4-2/TreeModel.html)
  * [Ensemble model] (http://www.dmg.org/v4-2/MultipleModels.html)
* Fully interoperable with popular open source software:
  * [R] (http://www.r-project.org/) and [Rattle] (http://rattle.togaware.com/)
  * [KNIME] (http://www.knime.org/)
  * [RapidMiner] (http://rapid-i.com/content/view/181/190/)

# Installation #

JPMML-Evaluator library JAR files (together with accompanying Java source and Javadocs JAR files) are released via [Maven Central Repository] (http://repo1.maven.org/maven2/org/jpmml/). Please join the [JPMML mailing list] (https://groups.google.com/forum/#!forum/jpmml) for release announcements.

The current version is **1.1.0** (6 March, 2014).

```xml
<dependency>
	<groupId>org.jpmml</groupId>
	<artifactId>pmml-evaluator</artifactId>
	<version>1.1.0</version>
</dependency>
```

# Usage #

A model evaluator class can be instantiated directly when the contents of the PMML document is known:
```java
PMML pmml = ...;

ModelEvaluator<TreeModel> modelEvaluator = new TreeModelEvaluator(pmml);
```

Otherwise, a PMML manager class should be instantiated first, which will inspect the contents of the PMML document and instantiate the right model evaluator class later:
```java
PMML pmml = ...;

PMMLManager pmmlManager = new PMMLManager(pmml);
 
ModelEvaluator<?> modelEvaluator = (ModelEvaluator<?>)pmmlManager.getModelManager(null, ModelEvaluatorFactory.getInstance());
```

Model evaluator classes follow functional programming principles. Model evaluator instances are cheap enough to be created and discarded as needed (ie. not worth the pooling effort).

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

Module `pmml-evaluator-example` exemplifies the use of JPMML-Evaluator library.

This module can be built using [Apache Maven] (http://maven.apache.org/):
```
mvn clean install
```

The resulting uber-JAR file `target/example-1.1-SNAPSHOT.jar` contains the following command-line applications:
* `org.jpmml.evaluator.CsvEvaluationExample` [(source)] (https://github.com/jpmml/jpmml-evaluator/blob/master/pmml-evaluator-example/src/main/java/org/jpmml/evaluator/CsvEvaluationExample.java). Evaluates a PMML model using data records from a CSV file.

For example, evaluating `model.pmml` using data records from `input.tsv`:
```
java -cp target/example-1.1-SNAPSHOT.jar org.jpmml.evaluator.CsvEvaluationExample --model model.pmml --input input.tsv --output output.tsv
```

# License #

JPMML-Evaluator is dual-licensed under the [GNU Affero General Public License (AGPL) version 3.0] (http://www.gnu.org/licenses/agpl-3.0.html) and a commercial license.

# Additional information #

Please contact [info@openscoring.io] (mailto:info@openscoring.io)