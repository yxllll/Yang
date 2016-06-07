package streamproducer

import java.io.PrintWriter
import java.net.ServerSocket

import org.apache.hadoop.io.LongWritable
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapred.SequenceFileInputFormat
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext

/**
 * Created by yang on 16-6-7.
 */
class SocketData {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("Query by Slide Window").setMaster("local[3]")
    conf.set("spark.serializer", "org,apache.spark.serializer.KryoSerializer")
    conf.registerKryoClasses(Array(classOf[LongWritable], classOf[Text]))
    val sc = new SparkContext(conf)
    sc.textFile(args(0))
    val sourceline = sc.hadoopFile(args(0),
      classOf[SequenceFileInputFormat[LongWritable, Text]],
      classOf[LongWritable],
      classOf[Text]
    )
      .groupByKey(13)
      .sortByKey(true)
      .collect()

    var i = 0
    val listener = new ServerSocket(19999)
    val socket = listener.accept()
    val out = new PrintWriter(socket.getOutputStream)

    new Thread() {
      override def run = {
        println("Get socket link from: " + socket.getInetAddress)
        val out = new PrintWriter(socket.getOutputStream(), true)
        while (i < sourceline.length) {
          Thread.sleep(500)
          // 当该端口接受请求时，随机获取某行数据发送给对方
          println("!!!!!!!!!!!!!--------------" + i + "--------------!!!!!!!!!!!!!!!!!")
          val key = sourceline.apply(i)._1
          sourceline.apply(i)._2.foreach(x => println(key + "\t" + x))
          sourceline.apply(i)._2.foreach(x => out.println(key + "\t" + x))
          out.flush()

          i = i + 1
        }
        socket.close()
      }
    }.start()
  }
}
