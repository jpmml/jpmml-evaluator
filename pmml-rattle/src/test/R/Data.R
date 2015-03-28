library("faraway")
library("rattle")
library("survival")

data("iris")
names(iris) = c("Sepal_Length", "Sepal_Width", "Petal_Length", "Petal_Width", "Species")
writeCsv(iris, "csv/Iris.csv")

data("audit")
audit = na.omit(audit)
names(audit)[11] = "Account"
names(audit)[12] = "Adjustment"
names(audit)[13] = "Adjusted"
writeCsv(audit, "csv/Audit.csv")

data("ozone")
writeCsv(ozone, "csv/Ozone.csv")

data("lung")
lung = lung[!(is.na(lung$inst) | is.na(lung$age) | is.na(lung$ph.ecog)), ]
lung = lung[!(lung$inst %in% c(2, 4, 10, 33)), ]
writeCsv(lung, "csv/Lung.csv")
