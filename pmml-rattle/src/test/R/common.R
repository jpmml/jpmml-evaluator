setwd("../resources")

readCsv = function(file){
	return (read.csv(file = file, header = TRUE))
}

writeCsv = function(data, file){
	write.table(data, file = file, sep = ",", quote = FALSE, row.names = FALSE, col.names = TRUE)
}