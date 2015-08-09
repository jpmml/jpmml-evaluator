library("arules")
library("pmml")

shoppingData = readCsv("csv/Shopping.csv")

# Remove 2 duplicate rows
shoppingData = unique(shoppingData)

baskets = split(shoppingData[, "item"], shoppingData[, "transaction"])

transactions = as(baskets, "transactions")

rules = apriori(transactions, parameter = list(support = 5 / 1000))

rulesPmml = pmml(rules)

associationModelNode = rulesPmml["AssociationModel"][[1]]

outputNode = xmlNode("Output")

outputFieldNodes = list(
	xmlNode(name = "OutputField", attrs = list(name = "ruleId", feature = "ruleValue", ruleFeature = "ruleId", algorithm = "exclusiveRecommendation", rank = "1", rankBasis = "support")),
	xmlNode(name = "OutputField", attrs = list(name = "support", feature = "ruleValue", ruleFeature = "support", algorithm = "exclusiveRecommendation", rank = "1", rankBasis = "support")),
	xmlNode(name = "OutputField", attrs = list(name = "confidence", feature = "ruleValue", ruleFeature = "confidence", algorithm = "exclusiveRecommendation", rank = "1", rankBasis = "support")),
	xmlNode(name = "OutputField", attrs = list(name = "lift", feature = "ruleValue", ruleFeature = "lift", algorithm = "exclusiveRecommendation", rank = "1", rankBasis = "support"))
)

outputNode = append.xmlNode(outputNode, outputFieldNodes)

associationModelNode = append.xmlNode(associationModelNode, outputNode)

rulesPmml["AssociationModel"][[1]] = associationModelNode

saveXML(rulesPmml, "pmml/AssociationRulesShopping.pmml")

formatRuleSide = function(x){
	return (paste(strsplit(LIST(x)[[1]], split = " "), collapse = ","))
}

formatRule = function(rule){
	return (paste("{", formatRuleSide(rule@lhs), "}->{", formatRuleSide(rule@rhs), "}", sep = ""))
}

result = data.frame("ruleId" = character(), support = numeric(), confidence = numeric(), lift = numeric(), stringsAsFactors = FALSE)

for(i in 1:length(transactions)){
	transaction = transactions[i]

	recommendations = is.subset(rules@lhs, transaction)
	exclusiveRecommendations = recommendations & !(is.subset(rules@rhs, transaction))

	trueIndices = grep(TRUE, exclusiveRecommendations)

	if(length(trueIndices) > 0){
		trueRules = rules[trueIndices]

		maxSupportTrueIndex = order(trueRules@quality$support, decreasing = TRUE)[1]

		# Rules are identified by an implicit 1-based index
		ruleId = as.character(trueIndices[maxSupportTrueIndex])

		maxSupportTrueRule = trueRules[maxSupportTrueIndex]

		support = maxSupportTrueRule@quality$support
		confidence = maxSupportTrueRule@quality$confidence
		lift = maxSupportTrueRule@quality$lift

		result[nrow(result) + 1, ] = c(ruleId, support, confidence, lift)
	} else

	{
		result[nrow(result) + 1, ] = c(NA, NA, NA, NA)
	}
}

writeCsv(result, "csv/AssociationRulesShopping.csv")
