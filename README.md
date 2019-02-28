JPMML-Evaluator [![Build Status](https://travis-ci.org/jpmml/jpmml-evaluator.png?branch=master)](https://travis-ci.org/jpmml/jpmml-evaluator)
===============

Java Evaluator API for Predictive Model Markup Language (PMML).

# Table of Contents #

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [API](#api)
- [Basic usage](#basic-usage)
- [Advanced usage](#advanced-usage)
    + [Loading models](#loading-models)
    + [Querying the "data schema" of models](#querying-the-data-schema-of-models)
    + [Evaluating models](#evaluating-models)
- [Example applications](#example-applications)
- [Support](#support)
- [License](#license)
- [Additional information](#additional-information)

# Features #

JPMML-Evaluator is *de facto* the reference implementation of the PMML specification versions 3.0, 3.1, 3.2, 4.0, 4.1, 4.2 and 4.3 for the Java/JVM platform:

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

* [R](https://www.r-project.org/) and [Rattle](https://rattle.togaware.com/):
  * [JPMML-R](https://github.com/jpmml/jpmml-r) library.
  * [`r2pmml`](https://github.com/jpmml/r2pmml) package.
  * [`pmml`](https://cran.r-project.org/package=pmml) and [`pmmlTransformations`](https://CRAN.R-project.org/package=pmmlTransformations) packages.
* [Python](https://www.python.org/) and [Scikit-Learn](https://scikit-learn.org/):
  * [JPMML-SkLearn](https://github.com/jpmml/jpmml-sklearn) library.
  * [`sklearn2pmml`](https://github.com/jpmml/sklearn2pmml) package.
* [Apache Spark](https://spark.apache.org/):
  * [JPMML-SparkML](https://github.com/jpmml/jpmml-sparkml) library.
  * [`pyspark2pmml`](https://github.com/pyspark2pmml) and [`sparklyr2pmml`](https://github.com/jpmml/sparklyr2pmml) packages.
  * [`mllib.pmml.PMMLExportable`](https://spark.apache.org/docs/latest/api/java/org/apache/spark/mllib/pmml/PMMLExportable.html) interface.
* [H2O.ai](https://www.h2o.ai/):
  * [JPMML-H2O](https://github.com/jpmml/jpmml-h2o) library.
* [XGBoost](https://github.com/dmlc/xgboost):
  * [JPMML-XGBoost](https://github.com/jpmml/jpmml-xgboost) library.
* [LightGBM](https://github.com/Microsoft/LightGBM):
  * [JPMML-LightGBM](https://github.com/jpmml/jpmml-lightgbm) library.
* [TensorFlow](https://tensorflow.org):
  * [JPMML-TensorFlow](https://github.com/jpmml/jpmml-tensorflow) library.
* [KNIME](https://www.knime.com/)
* [RapidMiner](https://rapidminer.com/products/rapidminer-studio/)
* [SAS](https://www.sas.com/en_us/software/analytics/enterprise-miner.html)
* [SPSS](https://www-01.ibm.com/software/analytics/spss/)

JPMML-Evaluator is fast and memory efficient. It can deliver one million scorings per second already on a desktop computer.

# Prerequisites #

* Java Platform, Standard Edition 8 or newer.

# Installation #

JPMML-Evaluator library JAR files (together with accompanying Java source and Javadocs JAR files) are released via [Maven Central Repository](https://repo1.maven.org/maven2/org/jpmml/).

The current version is **1.4.7** (17 February, 2019).

```xml
<dependency>
	<groupId>org.jpmml</groupId>
	<artifactId>pmml-evaluator</artifactId>
	<version>1.4.7</version>
</dependency>
<dependency>
	<groupId>org.jpmml</groupId>
	<artifactId>pmml-evaluator-extension</artifactId>
	<version>1.4.7</version>
</dependency>
```

# API #

Core types:

* Interface `org.jpmml.evaluator.EvaluatorBuilder`
  * Class `org.jpmml.evaluator.ModelEvaluatorBuilder` - Builds a `ModelEvaluator` instance based on an `org.dmg.pmml.PMML` instance
    * Class `org.jpmml.evaluator.LoadingModelEvaluatorBuilder` - Builds a `ModelEvaluator` instance from a PMML byte stream or a PMML file
* Interface `org.jpmml.evaluator.Evaluator`
  * Abstract class `org.jpmml.evaluator.ModelEvaluator` - Implements model evaluator functionality based on an `org.dmg.pmml.Model` instance
    * Classes `org.jpmml.evaluator.<Model>Evaluator` (`GeneralRegressionModelEvaluator`, `MiningModelEvaluator`, `NeuralNetworkEvaluator`, `RegressionEvaluator`, `TreeModelEvaluator`, `SupportVectorMachineEvaluator` etc.)
* Abstract class `org.jpmml.evaluator.ModelField`
  * Abstract class `org.jpmml.evaluator.InputField` - Describes a model input field
  * Abstract class `org.jpmml.evaluator.ResultField`
    * Class `org.jpmml.evaluator.TargetField` - Describes a primary model result field
    * Class `org.jpmml.evaluator.OutputField` - Describes a secondary model result field
* Abstract class `org.jpmml.evaluator.FieldValue`
  * Class `org.jpmml.evaluator.CollectionValue`
  * Abstract class `org.jpmml.evaluator.ScalarValue`
    * Class `org.jpmml.evaluator.ContinuousValue`
    * Abstract class `org.jpmml.evaluator.DiscreteValue`
      * Class `org.jpmml.evaluator.CategoricalValue`
      * Class `org.jpmml.evaluator.OrdinalValue`
* Utility class `org.jpmml.evaluator.EvaluatorUtil`
* Utility class `org.jpmml.evaluator.FieldValueUtil`

Core methods:

* `EvaluatorBuilder`
  * `#build()`
* `Evaluator`
  * `#verify()`
  * `#getInputFields()`
  * `#getTargetFields()`
  * `#getOutputFields()`
  * `#evaluate(Map<FieldName, ?>)`
* `InputField`
  * `#prepare(Object)`

Target value types:

* Interface `org.jpmml.evaluator.Computable`
  * Abstract class `org.jpmml.evaluator.AbstractComputable`
    * Class `org.jpmml.evaluator.Classification`
    * Class `org.jpmml.evaluator.Regression`
    * Class `org.jpmml.evaluator.Vote`
* Interface `org.jpmml.evaluator.ResultFeature`
  * Marker interface `org.jpmml.evaluator.HasCategoricalResult`
    * Marker interface `org.jpmml.evaluator.HasAffinity`
    * Marker interface `org.jpmml.evaluator.HasConfidence`
    * Marker interface `org.jpmml.evaluator.HasProbability`
  * Marker interface `org.jpmml.evaluator.HasDecisionPath`
  * Marker interface `org.jpmml.evaluator.HasEntityId`
  * Marker interface `org.jpmml.evaluator.HasPrediction`
* Abstract class `org.jpmml.evaluator.Report`
* Utility class `org.jpmml.evaluator.ReportUtil`

Target value methods:

* `Computable`
  * `#getResult()`
* `HasProbability`
  * `#getProbability(String)`
  * `#getProbabilityReport(String)`
* `HasPrediction`
  * `#getPrediction()`
  * `#getPredictionReport()`

Exception types:

* Abstract class `org.jpmml.evaluator.PMMLException`
  * Abstract class `org.jpmml.evaluator.InvalidMarkupException`
  * Abstract class `org.jpmml.evaluator.UnsupportedMarkupException`
  * Abstract class `org.jpmml.evaluator.EvaluationException`

# Basic usage #

```java
// Building a model evaluator from a PMML file
Evaluator evaluator = new LoadingModelEvaluatorBuilder()
	.setLocatable(false)
	.setVisitors(new DefaultVisitorBattery())
	//.setOutputFilter(OutputFilters.KEEP_FINAL_RESULTS)
	.load(new File("model.pmml"))
	.build();

// Perforing the self-check
evaluator.verify();

// Printing input (x1, x2, .., xn) fields
List<? extends InputField> inputFields = evaluator.getInputFields();
System.out.println("Input fields: " + inputFields);

// Printing primary result (y) field(s)
List<? extends TargetField> targetFields = evaluator.getTargetFields();
System.out.println("Target field(s): " + targetFields);

// Printing secondary result (eg. probability(y), decision(y)) fields
List<? extends OutputField> outputFields = evaluator.getOutputFields();
System.out.println("Output fields: " + outputFields);

// Iterating through columnar data (eg. a CSV file, an SQL result set)
while(true){
	// Reading a record from the data source
	Map<String, ?> inputRecord = readRecord();
	if(inputRecord == null){
		break;
	}

	Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();

	// Mapping the record field-by-field from data source schema to PMML schema
	for(InputField inputField : inputFields){
		FieldName inputName = inputField.getName();

		Object rawValue = inputRecord.get(inputName.getValue());

		// Transforming an arbitrary user-supplied value to a known-good PMML value
		FieldValue inputValue = inputField.prepare(rawValue);

		arguments.put(inputName, inputValue);
	}

	// Evaluating the model with known-good arguments
	Map<FieldName, ?> results = evaluator.evaluate(arguments);

	// Decoupling results from the JPMML-Evaluator runtime environment
	Map<String, ?> resultRecord = EvaluatorUtil.decodeAll(results);

	// Writing a record to the data sink
	writeRecord(resultRecord);
}

// Making the model evaluator eligible for garbage collection
evaluator = null;
```

# Advanced usage #

### Loading models ###

JPMML-Evaluator depends on the [JPMML-Model](https://github.com/jpmml/jpmml-model) library for PMML class model.

Loading a PMML schema version 3.X or 4.X document into an `org.dmg.pmml.PMML` instance:
```java
org.dmg.pmml.PMML pmml;

try(InputStream is = ...){
	pmml = org.jpmml.model.PMMLUtil.unmarshal(is);
}
```

The newly loaded `PMML` instance should tailored by applying appropriate `org.dmg.pmml.Visitor` implementation classes to it:
* `org.jpmml.model.visitors.LocatorTransformer`. Transforms SAX Locator information to Java serializable representation. Recommended for development and testing environments.
* `org.jpmml.model.visitors.LocatorNullifier`. Removes SAX Locator information. Recommended for production environments.
* `org.jpmml.model.visitors.<Type>Interner`. Replaces all occurrences of the same PMML attribute value with the singleton attribute value.
* `org.jpmml.evaluator.visitors.<Element>Optimizer`. Pre-parses a PMML element.
* `org.jpmml.evaluator.visitors.<Element>Interner`. Replaces all occurrences of the same PMML element with the singleton element.

To facilitate their discovery and use, visitor classes have been grouped into visitor battery classes:
* `org.jpmml.model.visitors.AttributeInternerBattery`
* `org.jpmml.model.visitors.AttributeOptimizerBattery`
* `org.jpmml.model.visitors.ListFinalizerBattery`
* `org.jpmml.evaluator.visitors.ElementInternerBattery`
* `org.jpmml.evaluator.visitors.ElementOptimizerBattery`

Creating and applying a custom visitor battery to reduce the memory consumption of a `PMML` instance in production environment:
```java
org.jpmml.model.VisitorBattery visitorBattery = new org.jpmml.model.VisitorBattery();

// Getting rid of SAX Locator information
visitorBattery.add(LocatorNullifier.class);

// Pre-parsing PMML elements
visitorBattery.addAll(new AttributeOptimizerBattery());
visitorBattery.addAll(new ElementOptimizerBattery());

// Getting rid of duplicate PMML attribute values and PMML elements
visitorBattery.addAll(new AttributeInternerBattery());
visitorBattery.addAll(new ElementInternerBattery());

// Freezing the final representation of PMML elements
visitorBattery.addAll(new ListFinalizerBattery());

visitorBattery.applyTo(pmml);
```

The PMML standard defines large number of model types.
The evaluation logic for each model type is encapsulated into a corresponding `ModelEvaluator` subclass.

Even though `ModelEvaluator` subclasses can be instantiated directly, the recommended approach is to follow the Builder design pattern as implemented by the `ModelEvaluatorBuilder` builder class.

Creating and configuring a `ModelEvaluatorBuilder` instance:

```java
ModelEvaluatorBuilder modelEvaluatorBuilder = new ModelEvaluatorBuilder(pmml);
	// Activate the generation of MathML prediction reports
	//.setValueFactoryFactory(org.jpmml.evaluator.ReportingValueFactoryFactory.newInstance());
```

By default, the model evaluator builder selects the first scorable model from the `PMML` instance, and builds a corresponding `ModelEvaluator` instance.
However, in order to promote loose coupling, it is advisable to cast the result to a much simplified `Evaluator` instance.

Building an `Evaluator` instance:

```java
Evaluator evaluator = (Evaluator)modelEvaluatorBuilder.build();
```

Model evaluator instances are fairly lightweight, which makes them cheap to create and destroy.
Nevertheless, long-running applications should maintain a one-to-one mapping between `PMML` and `Evaluator` instances for better performance.

Model evaluator classes follow functional programming principles and are completely thread safe.

### Querying the "data schema" of models ###

The model evaluator can be queried for the list of input (ie. independent), target (ie. primary dependent) and output (ie. secondary dependent) field definitions, which provide information about field name, data type, operational type, value domain etc.

Querying and analyzing input fields:
```java
List<? extends InputField> inputFields = evaluator.getInputFields();
for(InputField inputField : inputFields){
	org.dmg.pmml.DataField pmmlDataField = (org.dmg.pmml.DataField)inputField.getField();
	org.dmg.pmml.MiningField pmmlMiningField = inputField.getMiningField();

	org.dmg.pmml.DataType dataType = inputField.getDataType();
	org.dmg.pmml.OpType opType = inputField.getOpType();

	switch(opType){
		case CONTINUOUS:
			com.google.common.collect.RangeSet<Double> validInputRanges = inputField.getContinuousDomain();
			break;
		case CATEGORICAL:
		case ORDINAL:
			List<?> validInputValues = inputField.getDiscreteDomain();
			break;
		default:
			break;
	}
}
```

Querying and analyzing target fields:
```java
List<? extends TargetField> targetFields = evaluator.getTargetFields();
for(TargetField targetField : targetFields){
	org.dmg.pmml.DataField pmmlDataField = targetField.getField();
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
				Object validTargetValue = TypeUtil.parse(dataType, category);
			}
			break;
		default:
			break;
	}
}
```

Querying and analyzing output fields:
```java
List<? extends OutputField> outputFields = evaluator.getOutputFields();
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

### Evaluating models ###

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
Map<String, ?> inputDataRecord = ...;

Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();

List<? extends InputField> inputFields = evaluator.getInputFields();
for(InputField inputField : inputFields){
	FieldName inputName = inputField.getName();

	Object rawValue = inputDataRecord.get(inputName.getValue());

	// Transforming an arbitrary user-supplied value to a known-good PMML value
	// The user-supplied value is passed through: 1) outlier treatment, 2) missing value treatment, 3) invalid value treatment and 4) type conversion
	FieldValue inputValue = inputField.prepare(rawValue);

	arguments.put(inputName, inputValue);
}
```

Performing the evaluation:
```java
Map<FieldName, ?> results = evaluator.evaluate(arguments);
```

Extracting primary results from the result map:
```
List<? extends TargetField> targetFields = evaluator.getTargetFields();
for(TargetField targetField : targetFields){
	FieldName targetName = targetField.getName();

	Object targetValue = results.get(targetName);
}
```

The target value is either a Java primitive value (as a wrapper object) or a complex value as a `Computable` instance.

A complex target value may expose additional information about the prediction by implementing appropriate `ResultFeature` subinterfaces:
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

A complex target value may hold a reference to the model evaluator that created it. It is adisable to decode it to a Java primitive value (ie. decoupling from the JPMML-Evaluator runtime environment) as soon as all the additional information has been retrieved:
```java
if(targetValue instanceof Computable){
	Computable computable = (Computable)targetValue;

	targetValue = computable.getResult();
}
```

Extracting secondary results from the result map:
```
List<? extends OutputField> outputFields = evaluator.getOutputFields();
for(OutputField outputField : outputFields){
	FieldName outputName = outputField.getName();

	Object outputValue = results.get(outputName);
}
```

The output value is always a Java primitive value (as a wrapper object).

# Example applications #

Module `pmml-evaluator-example` exemplifies the use of the JPMML-Evaluator library.

This module can be built using [Apache Maven](https://maven.apache.org/):
```
mvn clean install
```

The resulting uber-JAR file `target/pmml-evaluator-executable-1.4-SNAPSHOT.jar` contains the following command-line applications:
* `org.jpmml.evaluator.EvaluationExample` [(source)](https://github.com/jpmml/jpmml-evaluator/blob/master/pmml-evaluator-example/src/main/java/org/jpmml/evaluator/EvaluationExample.java).
* `org.jpmml.evaluator.RecordCountingExample` [(source)](https://github.com/jpmml/jpmml-evaluator/blob/master/pmml-evaluator-example/src/main/java/org/jpmml/evaluator/RecordCountingExample.java).
* `org.jpmml.evaluator.TestingExample` [(source)](https://github.com/jpmml/jpmml-evaluator/blob/master/pmml-evaluator-example/src/main/java/org/jpmml/evaluator/TestingExample.java).

Evaluating model `model.pmml` with data records from `input.csv`. The predictions are stored to `output.csv`:
```
java -cp target/pmml-evaluator-executable-1.4-SNAPSHOT.jar org.jpmml.evaluator.EvaluationExample --model model.pmml --input input.csv --output output.csv
```

Evaluating model `model.pmml` with data records from `input.csv`. The predictions are verified against data records from `expected-output.csv`:
```
java -cp target/pmml-evaluator-executable-1.4-SNAPSHOT.jar org.jpmml.evaluator.TestingExample --model model.pmml --input input.csv --expected-output expected-output.csv
```

Enhancing model `model.pmml` with verification data records from `input_expected-output.csv`:
```
java -cp target/pmml-evaluator-executable-1.4-SNAPSHOT.jar org.jpmml.evaluator.EnhancementExample --model model.pmml --verification input_expected_output.csv
```

Getting help:
```
java -cp target/example-1.4-SNAPSHOT.jar <application class name> --help
```

# Support #

Limited public support is available via the [JPMML mailing list](https://groups.google.com/forum/#!forum/jpmml).

# License #

JPMML-Evaluator is dual-licensed under the [GNU Affero General Public License (AGPL) version 3.0](https://www.gnu.org/licenses/agpl-3.0.html), and a commercial license.

# Additional information #

JPMML-Evaluator is developed and maintained by Openscoring Ltd, Estonia.

Interested in using JPMML software in your application? Please contact [info@openscoring.io](mailto:info@openscoring.io)
