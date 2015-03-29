library("kernlab")
library("nnet")
library("pmml")
library("randomForest")

autoData = readCsv("csv/Auto.csv")
autoData$cylinders = as.factor(autoData$cylinders)
autoData$model_year = as.factor(autoData$model_year)
autoData$origin = as.factor(autoData$origin)

autoFormula = formula(mpg ~ .)

writeAuto = function(values, file){
	result = data.frame("mpg" = values, "Predicted_mpg" = values)

	writeCsv(result, file)
}

generateGeneralRegressionAuto = function(){
	glm = glm(autoFormula, autoData, family = gaussian)
	saveXML(pmml(glm), "pmml/GeneralRegressionAuto.pmml")

	writeAuto(predict(glm), "csv/GeneralRegressionAuto.csv")
}

generateNeuralNetworkAuto = function(){
	set.seed(13)

	nnet = nnet(autoFormula, autoData, size = 7, decay = 0.1, maxit = 1000, linout = TRUE)
	saveXML(pmml(nnet), "pmml/NeuralNetworkAuto.pmml")

	writeAuto(predict(nnet), "csv/NeuralNetworkAuto.csv")
}

generateRandomForestAuto = function(){
	set.seed(42)

	randomForest = randomForest(autoFormula, autoData, ntree = 10, mtry = 3, nodesize = 10)
	saveXML(pmml(randomForest), "pmml/RandomForestAuto.pmml")

	writeAuto(predict(randomForest, newdata = autoData), "csv/RandomForestAuto.csv")
}

generateRegressionAuto = function(){
	lm = lm(autoFormula, autoData)
	saveXML(pmml(lm), "pmml/RegressionAuto.pmml")

	writeAuto(predict(lm), "csv/RegressionAuto.csv")
}

generateSupportVectorMachineAuto = function(){
	set.seed(42)

	ksvm = ksvm(autoFormula, autoData)
	saveXML(pmml(ksvm, dataset = autoData), "pmml/SupportVectorMachineAuto.pmml")

	writeAuto(predict(ksvm, newdata = autoData), "csv/SupportVectorMachineAuto.csv")
}

generateGeneralRegressionAuto()
generateNeuralNetworkAuto()
generateRandomForestAuto()
generateRegressionAuto()
generateSupportVectorMachineAuto()