package br.org.gsj.ml.spark.clustering.kmeans

import java.io.ByteArrayInputStream
import java.io.File

import org.apache.spark.ml.clustering.KMeans
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import java.awt.image.MemoryImageSource

import javax.imageio.ImageIO
import java.awt.Toolkit
import java.awt.image.BufferedImage
import br.org.gsj.ml.scala.util.ImageUtils

object ImageSegmentation {
  
  
  def main(args: Array[String]): Unit = {
    
    val spark = SparkSession.builder().appName("mriClass").master("local[*]").getOrCreate()
    val mri_healthy_brain_image = "src/main/resources/datasets/images/ls.jpg"
    
    val image_array = ImageUtils.loadImageArray(mri_healthy_brain_image)
        
    import spark.implicits._
    
    val image_df = spark.sparkContext.parallelize(image_array).map(f => f).toDF
              
    val features_col = Array("a","r","g","b")
    val vector_assembler = new VectorAssembler()
    .setInputCols(features_col)
    .setOutputCol("features")
//    
    val mri_healthy_brain_df = vector_assembler.transform(image_df)  
    
    val k = 256
    val kmeans = new KMeans().setK(k).setSeed(12345).setFeaturesCol("features")
    val kmeans_model = kmeans.fit(mri_healthy_brain_df)   
//    val kmeans_centers = kmeans_model.clusterCenters
//    println("Cluster Centers --------")
//    for(k <- kmeans_centers)
//      println(k)
      
      
    val mri_healthy_brain_clusters_df = kmeans_model.transform(mri_healthy_brain_df)
    .select("pixel","features","prediction","a","r","g","b")
    
    var colors:Map[Int,Int] = Map[Int,Int]()
    
    for(i<-0 to k-1){
     val s = mri_healthy_brain_clusters_df.filter(col("prediction") === i).rdd.first
      colors += (i-> s(0).asInstanceOf[Int])
    }
    
   
    val image_array_final = mri_healthy_brain_clusters_df.select("prediction").rdd.map(f => f.getAs[Int](0).toByte).collect()
   
    val photo1 = ImageIO.read(new File(mri_healthy_brain_image))
    val photo2 = ImageUtils.generateImage(photo1, image_array_final,colors)
    
    ImageIO.write(photo2, "jpg", new File("src/main/resources/datasets/clustering/data/mri-images-data/mri-healthy-test.png"))



  }
  

  
}