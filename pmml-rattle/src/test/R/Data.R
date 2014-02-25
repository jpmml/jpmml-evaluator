library("faraway")
library("rattle")

data("iris")
writeCsv(iris, "csv/Iris.csv")

data("audit")
audit = na.omit(audit)
names(audit)[11] = "Account"
names(audit)[12] = "Adjustment"
names(audit)[13] = "Adjusted"
writeCsv(audit, "csv/Audit.csv")

data("ozone")
writeCsv(ozone, "csv/Ozone.csv")