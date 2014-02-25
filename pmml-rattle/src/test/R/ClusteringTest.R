library("amap")
library("pmml")
library("rattle")

irisData = readCsv("csv/Iris.csv")

numericIrisData = irisData[, c("Sepal.Length", "Sepal.Width", "Petal.Length", "Petal.Width")]

writeIris = function(clusters, file){
	result = data.frame(clusters)
	names(result) = c("Cluster")

	writeCsv(result, file)
}

generateHierarchicalClusteringIris = function(){
	hcluster = hcluster(numericIrisData)
	centers = centers.hclust(numericIrisData, hcluster, 10)
	saveXML(pmml(hcluster, centers = centers), "pmml/HierarchicalClusteringIris.pmml")

	clusters = predict(hcluster, numericIrisData, numericIrisData, 10)
	writeIris(clusters, "csv/HierarchicalClusteringIris.csv")
}

generateKMeansIris = function(){
	kmeans = kmeans(numericIrisData, 3)
	saveXML(pmml(kmeans), "pmml/KMeansIris.pmml")
	
	clusters = predict(kmeans, numericIrisData)
	writeIris(clusters, "csv/KMeansIris.csv")
}

generateHierarchicalClusteringIris()
generateKMeansIris()