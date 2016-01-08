library("e1071")
library("kernlab")
library("nnet")
library("pmml")
library("pmmlTransformations")
library("randomForest")
library("rpart")

irisData = readCsv("csv/Iris.csv")

irisFormula = formula(Species ~ .)

irisBox = WrapData(irisData)
irisBox = ZScoreXform(irisBox, xformInfo = "Sepal_Length")
irisBox = ZScoreXform(irisBox, xformInfo = "Sepal_Width")
irisBox = ZScoreXform(irisBox, xformInfo = "Petal_Length")
irisBox = ZScoreXform(irisBox, xformInfo = "Petal_Width")

irisXformFormula = formula(Species ~ derived_Sepal_Length + derived_Sepal_Width + derived_Petal_Length + derived_Petal_Width)

writeIris = function(classes, probabilities, file){
	result = data.frame("Species" = classes, "Predicted_Species" = classes)

	if(!is.null(probabilities)){
		result = data.frame(result, "Probability_setosa" = probabilities[, 1], "Probability_versicolor" = probabilities[, 2], "Probability_virginica" = probabilities[, 3])
	}

	writeCsv(result, file)
}

fixGeneralRegressionOutput = function(pmml, targetFieldName){
	generalRegressionModelNode = pmml["GeneralRegressionModel"][[1]]

	outputNode = generalRegressionModelNode["Output"][[1]]

	outputFieldNode = xmlNode("OutputField", attrs = list(name = "Probability_0", targetField = targetFieldName, feature = "probability", value = "0"))

	outputNode = append.xmlNode(outputNode, outputFieldNode)

	generalRegressionModelNode["Output"][[1]] = outputNode

	pmml["GeneralRegressionModel"][[1]] = generalRegressionModelNode

	return (pmml)
}

generateDecisionTreeIris = function(){
	rpart = rpart(irisFormula, irisData, method = "class", control = rpart.control(maxcompete = 0, maxsurrogate = 0))
	saveXML(pmml(rpart, dataset = irisData), "pmml/DecisionTreeIris.pmml")

	classes = predict(rpart, type = "class")
	probabilities = predict(rpart, type = "prob")
	writeIris(classes, probabilities, "csv/DecisionTreeIris.csv")
}

generateKernlabSVMIris = function(){
	set.seed(42)

	ksvm = ksvm(irisFormula, irisData)
	saveXML(pmml(ksvm, dataset = irisData), "pmml/KernlabSVMIris.pmml")

	classes = predict(ksvm, newdata = irisData, type = "response")
	votes = predict(ksvm, newdata = irisData, type = "votes")
	writeIris(classes, t(votes / 3), "csv/KernlabSVMIris.csv")
}

generateLibSVMIris = function(){
	svm = svm(irisFormula, irisData)
	saveXML(pmml(svm), "pmml/LibSVMIris.pmml")

	classes = predict(svm)
	writeIris(classes, NULL, "csv/LibSVMIris.csv")
}

generateLogisticRegressionIris = function(){
	multinom = multinom(irisFormula, irisData)
	saveXML(pmml(multinom), "pmml/LogisticRegressionIris.pmml")

	classes = predict(multinom)
	probabilities = predict(multinom, type = "probs")
	writeIris(classes, probabilities, "csv/LogisticRegressionIris.csv")
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

	classes = predict(nnet, type = "class")
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

generateRandomForestXformIris = function(){
	set.seed(42)

	randomForest = randomForest(irisXformFormula, irisBox$data, ntree = 7)
	saveXML(pmml(randomForest, transform = irisBox), "pmml/RandomForestXformIris.pmml")

	classes = predict(randomForest, newdata = irisBox$data, type = "class")
	probabilities = predict(randomForest, newdata = irisBox$data, type = "prob")
	writeIris(classes, probabilities, "csv/RandomForestXformIris.csv")
}

generateDecisionTreeIris()
generateKernlabSVMIris()
generateLibSVMIris()
generateLogisticRegressionIris()
generateNaiveBayesIris()
generateNeuralNetworkIris()
generateRandomForestIris()
generateRandomForestXformIris()

versicolorData = readCsv("csv/Versicolor.csv")
versicolorData$Species = as.factor(versicolorData$Species)

versicolorFormula = formula(Species ~ .,)

writeVersicolor = function(classes, probabilities, file){
	result = data.frame("Species" = classes, "Predicted_Species" = classes, "Probability_0" = probabilities[, 1], "Probability_1" = probabilities[, 2])

	writeCsv(result, file)
}

generateGeneralRegressionVersicolor = function(){
	glm = glm(versicolorFormula, versicolorData, family = binomial(link = probit))

	glmPmml = pmml(glm)

	glmPmml = fixGeneralRegressionOutput(glmPmml, "Species")

	saveXML(glmPmml, "pmml/GeneralRegressionVersicolor.pmml")

	probabilities = predict(glm, type = "response")
	classes = as.character(as.integer(probabilities > 0.5))
	probabilities = cbind(1 - probabilities, probabilities)
	writeVersicolor(classes, probabilities, "csv/GeneralRegressionVersicolor.csv")
}

generateGeneralRegressionVersicolor()

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
	rpart = rpart(auditFormula, auditData, method = "class", control = rpart.control(maxcompete = 0, maxsurrogate = 0))
	saveXML(pmml(rpart, dataset = auditData), "pmml/DecisionTreeAudit.pmml")

	classes = predict(rpart, type = "class")
	probabilities = predict(rpart, type = "prob")
	writeAudit(classes, probabilities, "csv/DecisionTreeAudit.csv")
}

generateGeneralRegressionAudit = function(){
	glm = glm(auditFormula, auditData, family = binomial)

	glmPmml = pmml(glm)

	glmPmml = fixGeneralRegressionOutput(glmPmml, "Adjusted")

	saveXML(glmPmml, "pmml/GeneralRegressionAudit.pmml")

	probabilities = predict(glm, type = "response")
	classes = as.character(as.integer(probabilities > 0.5))
	probabilities = cbind(1 - probabilities, probabilities)
	writeAudit(classes, probabilities, "csv/GeneralRegressionAudit.csv")
}

generateKernlabSVMAudit = function(){
	set.seed(42)

	ksvm = ksvm(auditFormula, auditData)
	saveXML(pmml(ksvm, dataset = auditData), "pmml/KernlabSVMAudit.pmml")

	classes = predict(ksvm, newdata = auditData, type = "response")
	votes = predict(ksvm, newdata = auditData, type = "votes")
	writeAudit(classes, t(votes), "csv/KernlabSVMAudit.csv")
}

generateLibSVMAudit = function(){
	svm = svm(auditFormula, auditData)
	saveXML(pmml(svm), "pmml/LibSVMAudit.pmml")

	classes = predict(svm)
	writeAudit(classes, NULL, "csv/LibSVMAudit.csv")
}

generateLogisticRegressionAudit = function(){
	multinom = multinom(auditFormula, auditData)
	saveXML(pmml(multinom), "pmml/LogisticRegressionAudit.pmml")

	classes = predict(multinom)
	probabilities = predict(multinom, type = "prob")
	probabilities = cbind(1 - probabilities, probabilities)
	writeAudit(classes, probabilities, "csv/LogisticRegressionAudit.csv")
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

generateDecisionTreeAudit()
generateGeneralRegressionAudit()
generateKernlabSVMAudit()
generateLibSVMAudit()
generateLogisticRegressionAudit()
generateNaiveBayesAudit()
generateNeuralNetworkAudit()
generateRandomForestAudit()