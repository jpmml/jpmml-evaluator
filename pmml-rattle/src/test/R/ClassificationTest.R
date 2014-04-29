library("e1071")
library("kernlab")
library("nnet")
library("pmml")
library("randomForest")
library("rpart")

irisData = readCsv("csv/Iris.csv")
irisFormula = formula(Species ~ .)

writeIris = function(classes, probabilities, file){
	result = NULL

	if(is.null(probabilities)){
		result = data.frame(classes, classes)
		names(result) = c("Species", "Predicted_Species")
	} else

	{
		result = data.frame(classes, classes, probabilities)
		names(result) = c("Species", "Predicted_Species", "Probability_setosa", "Probability_versicolor", "Probability_virginica")
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
versicolor = as.character(as.integer(irisData$Species == 'versicolor'))
versicolorData = cbind(irisData[, 1:4], versicolor)
versicolorFormula = formula(versicolor ~ .)

writeVersicolor = function(classes, probabilities, file){
	result = data.frame(classes, classes, probabilities)
	names(result) = c("versicolor", "Predicted_versicolor", "Probability_1")

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
		result = data.frame(classes, classes)
		names(result) = c("Adjusted", "Predicted_Adjusted")
	} else

	{
		result = data.frame(classes, classes, probabilities)
		names(result) = c("Adjusted", "Predicted_Adjusted", "Probability_0", "Probability_1")
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
	naiveBayes = naiveBayes(auditFormula, auditData, threshold = 0) 
	saveXML(pmml(naiveBayes, predictedField = "Adjusted"), "pmml/NaiveBayesAudit.pmml")

	classes = predict(naiveBayes, newdata = auditData, threshold = 0, type = "class")
	probabilities = predict(naiveBayes, newdata = auditData, threshold = 0, type = "raw")
	writeAudit(classes, probabilities, "csv/NaiveBayesAudit.csv")
}

generateNeuralNetworkAudit = function(){
	set.seed(41)

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