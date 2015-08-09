library("amap")
library("pmml")
library("rattle")

irisData = readCsv("csv/Iris.csv")
irisData$Species = NULL

writeIris = function(clusters, affinities, file){
	result = data.frame("predictedValue" = clusters)

	if(!is.null(affinities)){
		result = data.frame(result, affinities)
	}

	writeCsv(result, file)
}

generateHierarchicalClusteringIris = function(){
	hcluster = hcluster(irisData)
	centers = centers.hclust(irisData, hcluster, 10)
	saveXML(pmml(hcluster, centers = centers), "pmml/HierarchicalClusteringIris.pmml")

	clusters = predict(hcluster, irisData, irisData, 10)
	writeIris(clusters, NULL, "csv/HierarchicalClusteringIris.csv")
}

generateKMeansIris = function(){
	set.seed(42)

	kmeans = kmeans(irisData, 3)

	kmeansPmml = pmml(kmeans)

	clusteringModelNode = kmeansPmml["ClusteringModel"][[1]]

	outputNode = xmlNode("Output")

	outputFieldNodes = list(
		xmlNode(name = "OutputField", attrs = list(name = "predictedValue", feature = "predictedValue")),
		xmlNode(name = "OutputField", attrs = list(name = "affinity_1", feature = "affinity", value = "1")),
		xmlNode(name = "OutputField", attrs = list(name = "affinity_2", feature = "affinity", value = "2")),
		xmlNode(name = "OutputField", attrs = list(name = "affinity_3", feature = "affinity", value = "3"))
	)

	outputNode = append.xmlNode(outputNode, outputFieldNodes)

	clusteringModelNode["Output"][[1]] = outputNode

	kmeansPmml["ClusteringModel"][[1]] = clusteringModelNode

	saveXML(kmeansPmml, "pmml/KMeansIris.pmml")

	affinity = function(center){
		return (colSums(apply(irisData, 1, function(x) { ((x - center) ^ 2) })))
	}

	clusters = predict(kmeans, irisData)
	affinities = data.frame("affinity_1" = affinity(kmeans$centers[1, ]), "affinity_2" = affinity(kmeans$centers[2, ]), "affinity_3" = affinity(kmeans$centers[3, ]))
	writeIris(clusters, affinities, "csv/KMeansIris.csv")
}

generateHierarchicalClusteringIris()
generateKMeansIris()