library("e1071")
library("kernlab")
library("nnet")
library("pmml")
library("pmmlTransformations")
library("randomForest")
library("rpart")

irisData = readCsv("csv/Iris.csv")
irisFormula = formula(Species ~ .)
irisXformFormula = formula(Species ~ derived_Sepal.Length + derived_Sepal.Width + derived_Petal.Length + derived_Petal.Width)

writeIris = function(classes, probabilities, file){
	result = NULL

	if(is.null(probabilities)){
		result = data.frame(classes)
		names(result) = c("Species")
	} else

	{
		result = data.frame(classes, probabilities)
		names(result) = c("Species", "Probability_setosa", "Probability_versicolor", "Probability_virginica")
	}

	writeCsv(result, file)
}

generateDecisionTreeIris = function(){
	rpart = rpart(irisFormula, irisData)
	saveXML(pmml(rpart), "pmml/DecisionTreeIris.pmml")

	classes = predict(rpart, type = "class")
	probabilities = predict(rpart, type = "prob")
	writeIris(classes, probabilities, "csv/DecisionTreeIris.csv")
}

generateNaiveBayesIris = function(){
	naiveBayes = naiveBayes(irisFormula, irisData) 
	saveXML(pmml(naiveBayes, predictedField = "Species"), "pmml/NaiveBayesIris.pmml")

	classes = predict(naiveBayes, newdata = irisData, type = "class")
	probabilities = predict(naiveBayes, newdata = irisData, type = "raw")
	writeIris(classes, probabilities, "csv/NaiveBayesIris.csv")
}

generateNeuralNetworkIris = function(){
	nnet = nnet(irisFormula, irisData, size = 5)
	saveXML(pmml(nnet), "pmml/NeuralNetworkIris.pmml")

	classes = predict(nnet, type = "class", decay = 1e-3, maxit = 10000)
	probabilities = predict(nnet, type = "raw")
	writeIris(classes, probabilities, "csv/NeuralNetworkIris.csv")
}

generateRandomForestIris = function(){
	irisBox = WrapData(irisData)
	irisBox = ZScoreXform(irisBox)

	randomForest = randomForest(irisXformFormula, irisBox$data, ntree = 13, mtry = 4, nodesize = 10)
	saveXML(pmml(randomForest, transforms = irisBox), "pmml/RandomForestIris.pmml")

	classes = predict(randomForest, newdata = irisBox$data, type = "class")
	probabilities = predict(randomForest, newdata = irisBox$data, type = "prob")
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
	ksvm = ksvm(irisFormula, irisData)
	saveXML(pmml(ksvm, dataset = irisData), "pmml/SupportVectorMachineIris.pmml")

	writeIris(predict(ksvm, newdata = irisData), NULL, "csv/SupportVectorMachineIris.csv")
}

generateDecisionTreeIris()
generateNaiveBayesIris()
generateNeuralNetworkIris()
generateRandomForestIris()
generateRegressionIris()
generateSupportVectorMachineIris()

# Convert target field from categorical to binomial
versicolor = as.character(as.integer(irisData$Species == 'versicolor'))
versicolorData = cbind(irisData[, 1:4], versicolor)
versicolorFormula = formula(versicolor ~ .)

writeVersicolor = function(classes, probabilities, file){
	result = data.frame(classes, probabilities)
	names(result) = c("versicolor", "Probability_1")

	writeCsv(result, file)
}

binomialProbabilities = function(probabilities){
	return (probabilities / (probabilities + (1.0 / (1.0 + exp(0)))))
}

generateGeneralRegressionIris = function(){
	glm = glm(versicolorFormula, versicolorData, family = binomial)
	saveXML(pmml(glm), "pmml/GeneralRegressionIris.pmml")

	probabilities = binomialProbabilities(predict(glm, type = "response"))
	classes = as.character(as.integer(probabilities > 0.5))
	writeVersicolor(classes, probabilities, "csv/GeneralRegressionIris.csv")
}

generateGeneralRegressionIris()

auditData = readCsv("csv/Audit.csv")
auditData[, "Adjusted"] = as.factor(auditData[, "Adjusted"])
auditFormula = formula(Adjusted ~ Employment + Education + Marital + Occupation + Income + Gender + Deductions + Hours)

writeAudit = function(classes, probabilities, file){
	result = NULL

	if(is.null(probabilities)){
		result = data.frame(classes)
		names(result) = c("Adjusted")
	} else

	{
		result = data.frame(classes, probabilities)
		names(result) = c("Adjusted", "Probability_0", "Probability_1")
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
	saveXML(pmml(glm), "pmml/GeneralRegressionAudit.pmml")

	probabilities = binomialProbabilities(predict(glm, type = "response"))
	classes = as.character(as.integer(probabilities > 0.5))
	probabilities = cbind(1 - probabilities, probabilities)
	writeAudit(classes, probabilities, "csv/GeneralRegressionAudit.csv")
}

generateNaiveBayesAudit = function(){
	naiveBayes = naiveBayes(auditFormula, auditData) 
	saveXML(pmml(naiveBayes, predictedField = "Adjusted"), "pmml/NaiveBayesAudit.pmml")

	classes = predict(naiveBayes, newdata = auditData, type = "class")
	probabilities = predict(naiveBayes, newdata = auditData, type = "raw")
	writeAudit(classes, probabilities, "csv/NaiveBayesAudit.csv")
}

generateNeuralNetworkAudit = function(){
	nnet = nnet(auditFormula, auditData, size = 9, decay = 1e-3, maxit = 10000)
	saveXML(pmml(nnet), "pmml/NeuralNetworkAudit.pmml")

	classes = predict(nnet, type = "class")
	writeAudit(classes, NULL, "csv/NeuralNetworkAudit.csv")
}

generateRandomForestAudit = function(){
	randomForest = randomForest(auditFormula, auditData, ntree = 15, mtry = 8, nodesize = 10)
	saveXML(pmml(randomForest), "pmml/RandomForestAudit.pmml")
	
	classes = predict(randomForest, newdata = auditData, type = "class")
	probabilities = predict(randomForest, newdata = auditData, type = "prob")
	writeAudit(classes, probabilities, "csv/RandomForestAudit.csv")
}

generateSupportVectorMachineAudit = function(){
	ksvm = ksvm(auditFormula, auditData)
	saveXML(pmml(ksvm, dataset = auditData), "pmml/SupportVectorMachineAudit.pmml")

	writeAudit(predict(ksvm, newdata = auditData), NULL, "csv/SupportVectorMachineAudit.csv")
}

generateDecisionTreeAudit()
generateGeneralRegressionAudit()
generateNaiveBayesAudit()
generateNeuralNetworkAudit()
generateRandomForestAudit()
generateSupportVectorMachineAudit()