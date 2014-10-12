library("amap")
library("pmml")
library("rattle")

irisData = readCsv("csv/Iris.csv")

numericIrisData = irisData[, c("Sepal_Length", "Sepal_Width", "Petal_Length", "Petal_Width")]

writeIris = function(clusters, affinities, file){
	result = NULL

	if(is.null(affinities)){
		result = data.frame("predictedValue" = clusters)
	} else

	{
		result = data.frame("predictedValue" = clusters, affinities)
	}

	writeCsv(result, file)
}

generateHierarchicalClusteringIris = function(){
	hcluster = hcluster(numericIrisData)
	centers = centers.hclust(numericIrisData, hcluster, 10)
	saveXML(pmml(hcluster, centers = centers), "pmml/HierarchicalClusteringIris.pmml")

	clusters = predict(hcluster, numericIrisData, numericIrisData, 10)
	writeIris(clusters, NULL, "csv/HierarchicalClusteringIris.csv")
}

generateKMeansIris = function(){
	set.seed(42)

	kmeans = kmeans(numericIrisData, 3)
	saveXML(pmml(kmeans), "pmml/KMeansIris.pmml")

	affinity = function(center){
		return (colSums(apply(numericIrisData, 1, function(x) { ((x - center) ^ 2) })))
	}

	clusters = predict(kmeans, numericIrisData)
	affinities = data.frame("affinity_1" = affinity(kmeans$centers[1, ]), "affinity_2" = affinity(kmeans$centers[2, ]), "affinity_3" = affinity(kmeans$centers[3, ]))
	writeIris(clusters, affinities, "csv/KMeansIris.csv")
}

generateHierarchicalClusteringIris()
generateKMeansIris()