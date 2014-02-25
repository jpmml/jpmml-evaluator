library("arules")
library("pmml")

shoppingData = readCsv("csv/Shopping.csv")

# Remove 2 duplicate rows
shoppingData = unique(shoppingData)

baskets = split(shoppingData[, "Product"], shoppingData[, "Transaction"])

transactions = as(baskets, "transactions")

rules = apriori(transactions, parameter = list(support = 5 / 1000))

saveXML(pmml(rules), "pmml/AssociationRulesShopping.pmml")