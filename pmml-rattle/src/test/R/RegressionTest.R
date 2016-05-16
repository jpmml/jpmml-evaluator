library("e1071")
library("kernlab")
library("nnet")
library("pmml")
library("pmmlTransformations")
library("randomForest")
library("rpart")

autoData = readCsv("csv/Auto.csv")
autoData$cylinders = as.factor(autoData$cylinders)
autoData$model_year = as.factor(autoData$model_year)
autoData$origin = as.factor(autoData$origin)

autoFormula = formula(mpg ~ .)

autoBox = WrapData(autoData)
autoBox = ZScoreXform(autoBox, xformInfo = "displacement")
autoBox = ZScoreXform(autoBox, xformInfo = "horsepower")
autoBox = ZScoreXform(autoBox, xformInfo = "weight")
autoBox = ZScoreXform(autoBox, xformInfo = "acceleration")

autoXformFormula = formula(mpg ~ cylinders + derived_displacement + derived_horsepower + derived_weight + derived_acceleration + model_year + origin)

writeAuto = function(values, file){
	result = data.frame("mpg" = values, "Predicted_mpg" = values)

	writeCsv(result, file)
}

generateDecisionTreeAuto = function(){
	rpart = rpart(autoFormula, autoData, method = "anova", control = rpart.control(maxcompete = 0, maxsurrogate = 0))
	saveXML(pmml(rpart, dataset = autoData), "pmml/DecisionTreeAuto.pmml")

	writeAuto(predict(rpart), "csv/DecisionTreeAuto.csv")
}

generateDecisionTreeXformAuto = function(){
	rpart = rpart(autoXformFormula, autoBox$data, method = "anova", control = rpart.control(maxcompete = 0, maxsurrogate = 0))
	saveXML(pmml(rpart, transform = autoBox), "pmml/DecisionTreeXformAuto.pmml")

	writeAuto(predict(rpart), "csv/DecisionTreeXformAuto.csv")
}

generateGeneralRegressionAuto = function(){
	glm = glm(autoFormula, autoData, family = gaussian)
	saveXML(pmml(glm), "pmml/GeneralRegressionAuto.pmml")

	writeAuto(predict(glm), "csv/GeneralRegressionAuto.csv")
}

generateGeneralRegressionXformAuto = function(){
	glm = glm(autoXformFormula, autoBox$data, family = gaussian)
	saveXML(pmml(glm, transform = autoBox), "pmml/GeneralRegressionXformAuto.pmml")

	writeAuto(predict(glm), "csv/GeneralRegressionXformAuto.csv")
}

generateKernlabSVMAuto = function(){
	set.seed(42)

	ksvm = ksvm(autoFormula, autoData)
	saveXML(pmml(ksvm, dataset = autoData), "pmml/KernlabSVMAuto.pmml")

	writeAuto(predict(ksvm, newdata = autoData), "csv/KernlabSVMAuto.csv")
}

generateLibSVMAuto = function(){
	svm = svm(autoFormula, autoData)
	saveXML(pmml(svm), "pmml/LibSVMAuto.pmml")

	result = data.frame("svm_predict_function" = predict(svm))
	writeCsv(result, "csv/LibSVMAuto.csv")
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

generateRegressionXformAuto = function(){
	lm = lm(autoXformFormula, autoBox$data)
	saveXML(pmml(lm, transform = autoBox), "pmml/RegressionXformAuto.pmml")

	writeAuto(predict(lm), "csv/RegressionXformAuto.csv")
}

generateDecisionTreeAuto()
generateDecisionTreeXformAuto()
generateGeneralRegressionAuto()
generateGeneralRegressionXformAuto()
generateKernlabSVMAuto()
generateLibSVMAuto()
generateNeuralNetworkAuto()
generateRandomForestAuto()
generateRegressionAuto()
generateRegressionXformAuto()
