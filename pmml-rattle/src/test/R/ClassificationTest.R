library("e1071")
library("kernlab")
library("nnet")
library("pmml")
library("randomForest")
library("rpart")

irisData = readCsv("csv/Iris.csv")

irisFormula = formula(Species ~ .)

writeIris = function(classes, probabilities, file){
	result = data.frame("Species" = classes, "Predicted_Species" = classes)

	if(!is.null(probabilities)){
		result = data.frame(result, "Probability_setosa" = probabilities[, 1], "Probability_versicolor" = probabilities[, 2], "Probability_virginica" = probabilities[, 3])
	}

	writeCsv(result, file)
}

categoricalLogitProbabilities = function(probabilities){
	return (probabilities / ((1.0 / (1.0 + exp(0))) + probabilities))
}

categoricalProbitProbabilities = function(probabilities){
	return (probabilities / (pnorm(0) + probabilities))
}

generateDecisionTreeIris = function(){
	rpart = rpart(irisFormula, irisData)
	saveXML(pmml(rpart), "pmml/DecisionTreeIris.pmml")

	classes = predict(rpart, type = "class")
	probabilities = predict(rpart, type = "prob")
	writeIris(classes, probabilities, "csv/DecisionTreeIris.csv")
}

generateNaiveBayesIris = function(){
	naiveBayes = naiveBayes(irisFormula, irisData, threshold = 0)
	saveXML(pmml(naiveBayes, predictedField = "Species"), "pmml/NaiveBayesIris.pmml")

	classes = predict(naiveBayes, newdata = irisData, threshold = 0, type = "class")
	probabilities = predict(naiveBayes, newdata = irisData, threshold = 0, type = "raw")
	writeIris(classes, probabilities, "csv/NaiveBayesIris.csv")
}

generateNeuralNetworkIris = function(){
	set.seed(42)

	nnet = nnet(irisFormula, irisData, size = 5)
	saveXML(pmml(nnet), "pmml/NeuralNetworkIris.pmml")

	classes = predict(nnet, type = "class", decay = 1e-3, maxit = 10000)
	probabilities = predict(nnet, type = "raw")
	writeIris(classes, probabilities, "csv/NeuralNetworkIris.csv")
}

generateRandomForestIris = function(){
	set.seed(42)

	randomForest = randomForest(irisFormula, irisData, ntree = 7)
	saveXML(pmml(randomForest), "pmml/RandomForestIris.pmml")

	classes = predict(randomForest, newdata = irisData, type = "class")
	probabilities = predict(randomForest, newdata = irisData, type = "prob")
	writeIris(classes, probabilities, "csv/RandomForestIris.csv")
}

generateRegressionIris = function(){
	multinom = multinom(irisFormula, irisData)
	saveXML(pmml(multinom), "pmml/RegressionIris.pmml")

	classes = predict(multinom)
	probabilities = predict(multinom, type = "probs")
	writeIris(classes, probabilities, "csv/RegressionIris.csv")
}

generateSupportVectorMachineIris = function(){
	set.seed(42)

	ksvm = ksvm(irisFormula, irisData)
	saveXML(pmml(ksvm, dataset = irisData), "pmml/SupportVectorMachineIris.pmml")

	classes = predict(ksvm, newdata = irisData, type = "response")
	votes = predict(ksvm, newdata = irisData, type = "votes")
	writeIris(classes, t(votes / 3), "csv/SupportVectorMachineIris.csv")
}

generateDecisionTreeIris()
generateNaiveBayesIris()
generateNeuralNetworkIris()
generateRandomForestIris()
generateRegressionIris()
generateSupportVectorMachineIris()

# Convert target field from categorical to binomial
versicolor = as.factor(as.integer(irisData$Species == 'versicolor'))
versicolorData = cbind(irisData[, 1:4], versicolor)

versicolorFormula = formula(versicolor ~ .)

writeVersicolor = function(classes, probabilities, file){
	result = data.frame("versicolor" = classes, "Predicted_versicolor" = classes, "Probability_1" = probabilities)

	writeCsv(result, file)
}

generateGeneralRegressionIris = function(){
	glm = glm(versicolorFormula, versicolorData, family = binomial(link = probit))
	saveXML(pmml(glm), "pmml/GeneralRegressionIris.pmml")

	probabilities = categoricalProbitProbabilities(predict(glm, type = "response"))
	classes = as.character(as.integer(probabilities > 0.5))
	writeVersicolor(classes, probabilities, "csv/GeneralRegressionIris.csv")
}

generateGeneralRegressionIris()

auditData = readCsv("csv/Audit.csv")
auditData$Adjusted = as.factor(auditData$Adjusted)

auditFormula = formula(Adjusted ~ .)

writeAudit = function(classes, probabilities, file){
	result = data.frame("Adjusted" = classes, "Predicted_Adjusted" = classes)

	if(!is.null(probabilities)){
		result = data.frame(result, "Probability_0" = probabilities[, 1], "Probability_1" = probabilities[, 2])
	}

	writeCsv(result, file)
}

generateDecisionTreeAudit = function(){
	rpart = rpart(auditFormula, auditData, method = "class")
	saveXML(pmml(rpart), "pmml/DecisionTreeAudit.pmml")

	classes = predict(rpart, type = "class")
	probabilities = predict(rpart, type = "prob")
	writeAudit(classes, probabilities, "csv/DecisionTreeAudit.csv")
}

generateGeneralRegressionAudit = function(){
	glm = glm(auditFormula, auditData, family = binomial)
	pmml.glm = pmml.glm(glm)
	saveXML(pmml.glm, "pmml/GeneralRegressionAudit.pmml")

	pmml.lm = pmml.lm(glm)
	# Change the normalization method from "softmax" to "logit"
	xmlAttrs(pmml.lm[3]$RegressionModel)["normalizationMethod"] = "logit"
	saveXML(pmml.lm, "pmml/RegressionAudit.pmml")

	probabilities = categoricalLogitProbabilities(predict(glm, type = "response"))
	classes = as.character(as.integer(probabilities > 0.5))
	probabilities = cbind(1 - probabilities, probabilities)
	writeAudit(classes, probabilities, "csv/GeneralRegressionAudit.csv")
	writeAudit(classes, probabilities, "csv/RegressionAudit.csv")
}

generateNaiveBayesAudit = function(){
	naiveBayes = naiveBayes(auditFormula, auditData, threshold = 0)
	saveXML(pmml(naiveBayes, predictedField = "Adjusted"), "pmml/NaiveBayesAudit.pmml")

	classes = predict(naiveBayes, newdata = auditData, threshold = 0, type = "class")
	probabilities = predict(naiveBayes, newdata = auditData, threshold = 0, type = "raw")
	probabilities[is.nan(probabilities[, 2]), 2] = 0
	writeAudit(classes, probabilities, "csv/NaiveBayesAudit.csv")
}

generateNeuralNetworkAudit = function(){
	set.seed(13)

	nnet = nnet(auditFormula, auditData, size = 9, decay = 1e-3, maxit = 10000)
	saveXML(pmml(nnet), "pmml/NeuralNetworkAudit.pmml")

	classes = predict(nnet, type = "class")
	writeAudit(classes, NULL, "csv/NeuralNetworkAudit.csv")
}

generateRandomForestAudit = function(){
	set.seed(42)

	randomForest = randomForest(auditFormula, auditData, ntree = 15, mtry = 8, nodesize = 10)
	saveXML(pmml(randomForest), "pmml/RandomForestAudit.pmml")

	classes = predict(randomForest, newdata = auditData, type = "class")
	probabilities = predict(randomForest, newdata = auditData, type = "prob")
	writeAudit(classes, probabilities, "csv/RandomForestAudit.csv")
}

generateSupportVectorMachineAudit = function(){
	set.seed(42)

	ksvm = ksvm(auditFormula, auditData)
	saveXML(pmml(ksvm, dataset = auditData), "pmml/SupportVectorMachineAudit.pmml")

	classes = predict(ksvm, newdata = auditData, type = "response")
	votes = predict(ksvm, newdata = auditData, type = "votes")
	writeAudit(classes, t(votes), "csv/SupportVectorMachineAudit.csv")
}

generateDecisionTreeAudit()
generateGeneralRegressionAudit()
generateNaiveBayesAudit()
generateNeuralNetworkAudit()
generateRandomForestAudit()
generateSupportVectorMachineAudit()