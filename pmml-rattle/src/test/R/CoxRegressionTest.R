library("pmml")
library("survival")

lungData = readCsv("csv/Lung.csv")
lungData$inst = as.factor(lungData$inst)
lungData$ph.ecog = as.factor(lungData$ph.ecog)

writeLung = function(values, file){
	result = data.frame("hazard" = values, "Predicted_hazard" = values, "SurvivalProbability" = exp(-values))

	writeCsv(result, file)
}

generateCoxRegressionLung = function(){
	coxph = coxph(Surv(time, status) ~ age + ph.ecog + strata(inst), data = lungData)
	saveXML(pmml(coxph), "pmml/CoxRegressionLung.pmml")

	writeLung(predict(coxph, type = "expected"), "csv/CoxRegressionLung.csv")
}

generateCoxRegressionLung()
