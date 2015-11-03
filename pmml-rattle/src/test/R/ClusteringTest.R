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

	saveXML(pmml(kmeans), "pmml/KMeansIris.pmml")

	affinity = function(center){
		return (colSums(apply(irisData, 1, function(x) { ((x - center) ^ 2) })))
	}

	clusters = predict(kmeans, irisData)
	affinities = data.frame("clusterAffinity_1" = affinity(kmeans$centers[1, ]), "clusterAffinity_2" = affinity(kmeans$centers[2, ]), "clusterAffinity_3" = affinity(kmeans$centers[3, ]))
	writeIris(clusters, affinities, "csv/KMeansIris.csv")
}

generateHierarchicalClusteringIris()
generateKMeansIris()
