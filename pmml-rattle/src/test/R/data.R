library("rattle")
library("survival")

data("iris")
names(iris) = c("Sepal_Length", "Sepal_Width", "Petal_Length", "Petal_Width", "Species")
writeCsv(iris, "csv/Iris.csv")

data("audit")
names(audit)[11] = "Account"
names(audit)[12] = "Adjustment"
names(audit)[13] = "Adjusted"
audit = na.omit(audit)
writeCsv(audit, "csv/Audit.csv")

auto = read.table("http://archive.ics.uci.edu/ml/machine-learning-databases/auto-mpg/auto-mpg.data", quote = "\"", header = FALSE, na.strings = "?", row.names = NULL, col.names = c("mpg", "cylinders", "displacement", "horsepower", "weight", "acceleration", "model_year", "origin", "car_name"))
auto$car_name = NULL
auto = na.omit(auto)
writeCsv(auto, "csv/Auto.csv")

data("lung")
lung = lung[!(is.na(lung$inst) | is.na(lung$age) | is.na(lung$ph.ecog)), ]
lung = lung[!(lung$inst %in% c(2, 4, 10, 33)), ]
writeCsv(lung, "csv/Lung.csv")
