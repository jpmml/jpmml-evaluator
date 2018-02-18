JPMML-Evaluator [![Build Status](https://travis-ci.org/jpmml/jpmml-evaluator.png?branch=master)](https://travis-ci.org/jpmml/jpmml-evaluator)
===============

Java Evaluator API for Predictive Model Markup Language (PMML).

# Features #

JPMML-Evaluator is *de facto* the reference implementation of the PMML specification versions 3.0, 3.1, 3.2, 4.0, 4.1, 4.2 and 4.3 for the Java platform:

* Pre-processing of input fields according to the [DataDictionary](http://www.dmg.org/pmml/v4-3/DataDictionary.html) and [MiningSchema](http://www.dmg.org/pmml/v4-3/MiningSchema.html) elements:
  * Complete data type system.
  * Complete operational type system.
  * Treatment of outlier, missing and/or invalid values.
* Model evaluation:
  * [Association rules](http://www.dmg.org/pmml/v4-3/AssociationRules.html)
  * [Cluster model](http://www.dmg.org/pmml/v4-3/ClusteringModel.html)
  * [General regression](http://www.dmg.org/pmml/v4-3/GeneralRegression.html)
  * [Naive Bayes](http://www.dmg.org/pmml/v4-3/NaiveBayes.html)
  * [k-Nearest neighbors](http://www.dmg.org/pmml/v4-3/KNN.html)
  * [Neural network](http://www.dmg.org/pmml/v4-3/NeuralNetwork.html)
  * [Regression](http://www.dmg.org/pmml/v4-3/Regression.html)
  * [Rule set](http://www.dmg.org/pmml/v4-3/RuleSet.html)
  * [Scorecard](http://www.dmg.org/pmml/v4-3/Scorecard.html)
  * [Support Vector Machine](http://www.dmg.org/pmml/v4-3/SupportVectorMachine.html)
  * [Tree model](http://www.dmg.org/pmml/v4-3/TreeModel.html)
  * [Ensemble model](http://www.dmg.org/pmml/v4-3/MultipleModels.html)
* Post-processing of target fields according to the [Targets](http://www.dmg.org/pmml/v4-3/Targets.html) element:
  * Rescaling and/or casting regression results.
  * Replacing a missing regression result with the default value.
  * Replacing a missing classification result with the map of prior probabilities.
* Calculation of auxiliary output fields according to the [Output](http://www.dmg.org/pmml/v4-3/Output.html) element:
  * Over 20 different result feature types.
* Model verification according to the [ModelVerification](http://www.dmg.org/pmml/v4-3/ModelVerification.html) element.
* Vendor extensions:
  * Java-backed model, expression and predicate types - integrate any 3rd party Java library into PMML data flow.
  * MathML prediction reports.

For more information please see the [features.md](https://github.com/jpmml/jpmml-evaluator/blob/master/features.md) file.

JPMML-Evaluator is interoperable with most popular statistics and data mining software:

* [R](http://www.r-project.org/) and [Rattle](http://rattle.togaware.com/):
  * [JPMML-R](https://github.com/jpmml/jpmml-r) library and [`r2pmml`](https://github.com/jpmml/r2pmml) package.
  * [`pmml`](https://cran.r-project.org/web/packages/pmml/) and [`pmmlTransformations`](https://cran.r-project.org/web/packages/pmmlTransformations/) packages.
* [Python](http://www.python.org/) and [Scikit-Learn](http://scikit-learn.org/):
  * [JPMML-SkLearn](https://github.com/jpmml/jpmml-sklearn) library and [`sklearn2pmml`](https://github.com/jpmml/sklearn2pmml) package.
* [Apache Spark](http://spark.apache.org/):
  * [JPMML-SparkML](https://github.com/jpmml/jpmml-sparkml) library.
  * [`mllib.pmml.PMMLExportable`](https://spark.apache.org/docs/latest/api/java/org/apache/spark/mllib/pmml/PMMLExportable.html) interface.
* [XGBoost](https://github.com/dmlc/xgboost):
  * [JPMML-XGBoost](https://github.com/jpmml/jpmml-xgboost) library.
* [LightGBM](https://github.com/Microsoft/LightGBM):
  * [JPMML-LightGBM](https://github.com/jpmml/jpmml-lightgbm) library.
* [TensorFlow](http://tensorflow.org):
  * [JPMML-TensorFlow](https://github.com/jpmml/jpmml-tensorflow) library.
* [KNIME](http://www.knime.com/)
* [RapidMiner](http://rapidminer.com/products/rapidminer-studio/)
* [SAS](http://www.sas.com/en_us/software/analytics/enterprise-miner.html)
* [SPSS](http://www-01.ibm.com/software/analytics/spss/)

JPMML-Evaluator is fast and memory efficient. It can deliver one million scorings per second already on a desktop computer.

# Prerequisites #

* Java 1.8 or newer.

# Installation #

JPMML-Evaluator library JAR files (together with accompanying Java source and Javadocs JAR files) are released via [Maven Central Repository](http://repo1.maven.org/maven2/org/jpmml/).

The current version is **1.4.0** (18 February, 2018).

```xml
<dependency>
	<groupId>org.jpmml</groupId>
	<artifactId>pmml-evaluator</artifactId>
	<version>1.4.0</version>
</dependency>
<dependency>
	<groupId>org.jpmml</groupId>
	<artifactId>pmml-evaluator-extension</artifactId>
	<version>1.4.0</version>
</dependency>
```

# Usage #

### Loading models

JPMML-Evaluator depends on the [JPMML-Model](https://github.com/jpmml/jpmml-model) library for PMML class model.

Loading a PMML schema version 3.X or 4.X document into an `org.dmg.pmml.PMML` instance:
```java
PMML pmml;

try(InputStream is = ...){
	pmml = org.jpmml.model.PMMLUtil.unmarshal(is);
}
```

The newly loaded `PMML` instance should tailored by applying appropriate `org.dmg.pmml.Visitor` implementation classes to it:
* `org.jpmml.model.visitors.LocatorTransformer`. Transforms SAX Locator information to Java serializable representation. Recommended for development and testing environments.
* `org.jpmml.model.visitors.LocatorNullifier`. Removes SAX Locator information. Recommended for production environments.
* `org.jpmml.model.visitors.<Type>Interner`. Replaces all occurrences of the same Java value with the singleton value.
* `org.jpmml.evaluator.visitors.<Element>Optimizer`. Pre-parses PMML element.
* `org.jpmml.evaluator.visitors.<Element>Interner`. Replaces all occurrences of the same PMML element with the singleton element.

Removing SAX Locator information (reduces memory consumption up to 25%):
```java
Visitor visitor = new org.jpmml.model.visitors.LocatorNullifier();
visitor.applyTo(pmml);
```

The PMML standard defines large number of model types.
The evaluation logic for each model type is encapsulated into a corresponding `org.jpmml.evaluator.ModelEvaluator` subclass.

Even though `ModelEvaluator` subclasses can be created directly, the recommended approach is to follow the factory design pattern as implemented by the `org.jpmml.evaluator.ModelEvaluatorFactory` factory class.

Obtaining and configuring a `ModelEvaluatorFactory` instance:
```java
ModelEvaluatorFactory modelEvaluatorFactory = ModelEvaluatorFactory.newInstance();

// Activate the generation of MathML prediction reports
ValueFactoryFactory valueFactoryFactory = ReportingValueFactoryFactory.newInstance();
modelEvaluatorFactory.setValueFactoryFactory(valueFactoryFactory);
```

The model evaluator factory selects the first model from the `PMML` instance, and creates and configures a corresponding `ModelEvaluator` instance.
However, in order to promote loose coupling, it is advisable to cast the result to a much simplified `org.jpmml.evaluator.Evaluator` instance.

Obtaining an `Evaluator` instance for the `PMML` instance:
```java
Evaluator evaluator = (Evaluator)modelEvaluatorFactory.newModelEvaluator(pmml);
```

Model evaluator classes follow functional programming principles and are completely thread safe.

Model evaluator instances are fairly lightweight, which makes them cheap to create and destroy.
Nevertheless, long-running applications should maintain a one-to-one mapping between `PMML` and `Evaluator` instances for better performance.

### Querying the "data schema" of models

The model evaluator can be queried for the list of input (ie. independent), target (ie. primary dependent) and output (ie. secondary dependent) field definitions, which provide information about field name, data type, operational type, value domain etc. information.

Querying and analyzing input fields:
```java
List<InputField> inputFields = evaluator.getInputFields();
for(InputField inputField : inputFields){
	org.dmg.pmml.DataField pmmlDataField = (org.dmg.pmml.DataField)inputField.getField();
	org.dmg.pmml.MiningField pmmlMiningField = inputField.getMiningField();

	org.dmg.pmml.DataType dataType = inputField.getDataType();
	org.dmg.pmml.OpType opType = inputField.getOpType();

	switch(opType){
		case CONTINUOUS:
			RangeSet<Double> validArgumentRanges = inputField.getContinuousDomain();
			break;
		case CATEGORICAL:
		case ORDINAL:
			List<?> validArgumentValues = inputField.getDiscreteDomain();
			break;
		default:
			break;
	}
}
```

Querying and analyzing target fields:
```java
List<TargetField> targetFields = evaluator.getTargetFields();
for(TargetField targetField : targetFields){
	org.dmg.pmml.DataField pmmlDataField = targetField.getDataField();
	org.dmg.pmml.MiningField pmmlMiningField = targetField.getMiningField(); // Could be null
	org.dmg.pmml.Target pmmlTarget = targetField.getTarget(); // Could be null

	org.dmg.pmml.DataType dataType = targetField.getDataType();
	org.dmg.pmml.OpType opType = targetField.getOpType();

	switch(opType){
		case CONTINUOUS:
			break;
		case CATEGORICAL:
		case ORDINAL:
			List<String> categories = targetField.getCategories();
			for(String category : categories){
				Object validResultValue = TypeUtil.parse(dataType, category);
			}
			break;
		default:
			break;
	}
}
```

Querying and analyzing output fields:
```java
List<OutputField> outputFields = evaluator.getOutputFields();
for(OutputField outputField : outputFields){
	org.dmg.pmml.OutputField pmmlOutputField = outputField.getOutputField();

	org.dmg.pmml.DataType dataType = outputField.getDataType(); // Could be null
	org.dmg.pmml.OpType opType = outputField.getOpType(); // Could be null

	boolean finalResult = outputField.isFinalResult();
	if(!finalResult){
		continue;
	}
}
```

### Evaluating models

A model may contain verification data, which is a small but representative set of data records (inputs plus expected outputs) for ensuring that the model evaluator is behaving correctly in this deployment configuration (JPMML-Evaluator version, Java/JVM version and vendor etc. variables).
The model evaluator should be verified once, before putting it into actual use.

Performing the self-check:
```java
evaluator.verify();
```

During scoring, the application code should iterate over data records (eg. rows of a table), and apply the following encode-evaluate-decode sequence of operations to each one of them.

The processing of the first data record will be significantly slower than the processing of all subsequent data records, because the model evaluator needs to lookup, validate and pre-parse model content.
If the model contains verification data, then this warm-up cost is borne during the self-check.

Preparing the argument map:
```java
Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();

List<InputField> inputFields = evaluator.getInputFields();
for(InputField inputField : inputFields){
	FieldName inputFieldName = inputField.getName();

	// The raw (ie. user-supplied) value could be any Java primitive value
	Object rawValue = ...;

	// The raw value is passed through: 1) outlier treatment, 2) missing value treatment, 3) invalid value treatment and 4) type conversion
	FieldValue inputFieldValue = inputField.prepare(rawValue);

	arguments.put(inputFieldName, inputFieldValue);
}
```

Performing the evaluation:
```java
Map<FieldName, ?> results = evaluator.evaluate(arguments);
```

Extracting primary results from the result map:
```
List<TargetField> targetFields = evaluator.getTargetFields();
for(TargetField targetField : targetFields){
	FieldName targetFieldName = targetField.getName();

	Object targetFieldValue = results.get(targetFieldName);
}
```

The target value is either a Java primitive value (as a wrapper object) or an instance of `org.jpmml.evaluator.Computable`:
```java
if(targetFieldValue instanceof Computable){
	Computable computable = (Computable)targetFieldValue;

	Object unboxedTargetFieldValue = computable.getResult();
}
```

The target value may implement interfaces that descend from interface `org.jpmml.evaluator.ResultFeature`:
```java
// Test for "entityId" result feature
if(targetFieldValue instanceof HasEntityId){
	HasEntityId hasEntityId = (HasEntityId)targetFieldValue;
	HasEntityRegistry<?> hasEntityRegistry = (HasEntityRegistry<?>)evaluator;
	BiMap<String, ? extends Entity> entities = hasEntityRegistry.getEntityRegistry();
	Entity winner = entities.get(hasEntityId.getEntityId());

	// Test for "probability" result feature
	if(targetFieldValue instanceof HasProbability){
		HasProbability hasProbability = (HasProbability)targetFieldValue;
		Double winnerProbability = hasProbability.getProbability(winner.getId());
	}
}
```

Extracting secondary results from the result map:
```
List<OutputField> outputFields = evaluator.getOutputFields();
for(OutputField outputField : outputFields){
	FieldName outputFieldName = outputField.getName();

	Object outputFieldValue = results.get(outputFieldName);
}
```

The output value is always a Java primitive value (as a wrapper object).

# Example applications #

Module `pmml-evaluator-example` exemplifies the use of the JPMML-Evaluator library.

This module can be built using [Apache Maven](http://maven.apache.org/):
```
mvn clean install
```

The resulting uber-JAR file `target/example-1.4-SNAPSHOT.jar` contains the following command-line applications:
* `org.jpmml.evaluator.EvaluationExample` [(source)](https://github.com/jpmml/jpmml-evaluator/blob/master/pmml-evaluator-example/src/main/java/org/jpmml/evaluator/EvaluationExample.java).
* `org.jpmml.evaluator.TestingExample` [(source)](https://github.com/jpmml/jpmml-evaluator/blob/master/pmml-evaluator-example/src/main/java/org/jpmml/evaluator/TestingExample.java).
* `org.jpmml.evaluator.EnhancementExample`.

Evaluating model `model.pmml` with data records from `input.csv`. The predictions are stored to `output.csv`:
```
java -cp target/example-1.4-SNAPSHOT.jar org.jpmml.evaluator.EvaluationExample --model model.pmml --input input.csv --output output.csv
```

Evaluating model `model.pmml` with data records from `input.csv`. The predictions are verified against data records from `expected-output.csv`:
```
java -cp target/example-1.4-SNAPSHOT.jar org.jpmml.evaluator.TestingExample --model model.pmml --input input.csv --expected-output expected-output.csv
```

Enhancing model `model.pmml` with verification data records from `input_expected-output.csv`:
```
java -cp target/example-1.4-SNAPSHOT.jar org.jpmml.evaluator.EnhancementExample --model model.pmml --verification input_expected_output.csv
```

Getting help:
```
java -cp target/example-1.4-SNAPSHOT.jar <application class name> --help
```

# Support and Documentation #

Limited public support is available via the [JPMML mailing list](https://groups.google.com/forum/#!forum/jpmml).

The [Openscoring.io blog](http://openscoring.io/blog/) contains fully worked out examples about using JPMML-Model and JPMML-Evaluator libraries.

Recommended reading:
* [Preparing arguments for evaluation](http://openscoring.io/blog/2014/05/15/jpmml_evaluator_api_prepare_evaluate/)
* [Testing PMML Applications](http://openscoring.io/blog/2014/05/12/testing_pmml_applications/)

# License #

JPMML-Evaluator is licensed under the [GNU Affero General Public License (AGPL) version 3.0](http://www.gnu.org/licenses/agpl-3.0.html). Other licenses are available on request.

# Additional information #

Please contact [info@openscoring.io](mailto:info@openscoring.io)
