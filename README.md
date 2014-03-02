JPMML-Evaluator [![Build Status](https://travis-ci.org/jpmml/jpmml-evaluator.png?branch=master)](https://travis-ci.org/jpmml/jpmml-evaluator)
===============

Java Evaluator API for Predictive Model Markup Language (PMML).

# Features #

* Full support for [DataDictionary] (http://www.dmg.org/v4-1/DataDictionary.html) and [MiningSchema] (http://www.dmg.org/v4-1/MiningSchema.html) elements:
  * Complete data type system.
  * Complete operational type system. For example, continuous integers, categorical integers and ordinal integers are handled differently in equality check and comparison operations.
  * Detection and treatment of outlier, missing and invalid values.
* Full support for [transformations] (http://www.dmg.org/v4-1/Transformations.html) and [functions] (http://www.dmg.org/v4-1/Functions.html):
  * Built-in functions.
  * User defined functions (PMML, Java).
* Full support for [Targets] (http://www.dmg.org/v4-1/Targets.html) and [Output] (http://www.dmg.org/v4-1/Output.html) elements.
* Fully supported model elements:
  * [Association rules] (http://www.dmg.org/v4-1/AssociationRules.html)
  * [Cluster model] (http://www.dmg.org/v4-1/ClusteringModel.html)
  * [General regression] (http://www.dmg.org/v4-1/GeneralRegression.html)
  * [Naive Bayes] (http://www.dmg.org/v4-1/NaiveBayes.html)
  * [k-Nearest neighbors] (http://www.dmg.org/v4-1/KNN.html)
  * [Neural network] (http://www.dmg.org/v4-1/NeuralNetwork.html)
  * [Regression] (http://www.dmg.org/v4-1/Regression.html)
  * [Rule set] (http://www.dmg.org/v4-1/RuleSet.html)
  * [Scorecard] (http://www.dmg.org/v4-1/Scorecard.html)
  * [Support Vector Machine] (http://www.dmg.org/v4-1/SupportVectorMachine.html)
  * [Tree model] (http://www.dmg.org/v4-1/TreeModel.html)
  * [Ensemble model] (http://www.dmg.org/v4-1/MultipleModels.html)
* Fully interoperable with popular open source software:
  * [R] (http://www.r-project.org/) and [Rattle] (http://rattle.togaware.com/)
  * [KNIME] (http://www.knime.org/)
  * [RapidMiner] (http://rapid-i.com/content/view/181/190/)

# Installation #

JPMML library JAR files (together with accompanying Java source and Javadocs JAR files) are released via [Maven Central Repository] (http://repo1.maven.org/maven2/org/jpmml/). Please join the [JPMML mailing list] (https://groups.google.com/forum/#!forum/jpmml) for release announcements.

The current version is **1.0.22** (17 February, 2014).

```xml
<dependency>
	<groupId>org.jpmml</groupId>
	<artifactId>pmml-evaluator</artifactId>
	<version>${jpmml.version}</version>
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

An evaluator instance can be queried for the definition of active (ie. independent), predicted (ie. primary dependent) and output (ie. secondary dependent) fields:
```java
List<FieldName> activeFields = evaluator.getActiveFields();
List<FieldName> predictedFields = evaluator.getPredictedFields();
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

Typically, a model has exactly one predicted field, which is called the target field:
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

# License #

JPMML-Evaluator is dual-licensed under the [GNU Affero General Public License (AGPL) version 3.0] (http://www.gnu.org/licenses/agpl-3.0.html) and a commercial license.

# Contact and Support #

Get in touch: [info@openscoring.io] (mailto:info@openscoring.io)