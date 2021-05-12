package de.tuberlin.mcc.faas4fogsim

import java.io.File
import java.util.*
import kotlin.math.roundToInt


fun main(args: Array<String>) {
    if (args.isNotEmpty() && args[0] == "sim1") {
        print("Running Simulation 1")
        varyRequestLoad()
    } else if (args.isNotEmpty() && args[0] == "sim2") {
        print("Running Simulation 2")
        varyNoOfExecutables()
    } else {
        print("Please describe whether you want to start sim1 (varyNumberOfExecutables) or sim2 (varyRequestLoad)")
    }
}

// Simulation 1: used to study the effect of an increasing request load on the processing prices
fun varyRequestLoad() {
    val writer = File("summary.csv").printWriter()
    val writerPerNode = File("noderesults.csv").printWriter()
    val writerPerReqs = File("reqresults.csv").printWriter()
    writerPerReqs.println("reqPerEdgePerSecond;min_latency;avg_latency;max_latency")
    writerPerNode.println("reqPerEdgePerSecond;name;type;processing_earnings;storage_earnings;processed_reqs;delegated_reqs;no_executables")

    for (reqsPerSec in 100..10000 step 100) {
        Config.storageCapacityEdge = 200
        Config.storageCapacityIntermediary = 1000
        Config.parallelRequestCapacityEdge = 5
        Config.parallelRequestCapacityIntermediary = 20
        Config.numberOfExecutables = 10
        Config.avgExecutionPrice = 100.0
        Config.averageStoragePrice = 10.0

        Config.requestsPerEdgePerSecond = reqsPerSec
        Config.reset()
        val sim = Simulator()
        val result = sim.runSimulation()
        writer.println(result.toCsvString())
        writerPerReqs.println("${Config.requestsPerEdgePerSecond};${result.minLatency};${result.avgLatency.asDecimal()};${result.maxLatency}")
        result.nodeResults.forEach { writerPerNode.println("${Config.requestsPerEdgePerSecond};${it.toCsvString()}") }
    }


    writer.close()
    writerPerNode.close()
    writerPerReqs.close()
}

// Simulation 2: used to study the effect of an increasing number of executeables on storage prices
fun varyNoOfExecutables() {
    val writer = File("summary.csv").printWriter()
    val writerPerNode = File("noderesults.csv").printWriter()
    val writerPerReqs = File("reqresults.csv").printWriter()
    writerPerReqs.println("no_executables;min_latency;avg_latency;max_latency")
    writerPerNode.println("no_executables;name;type;processing_earnings;storage_earnings;processed_reqs;delegated_reqs;no_executables")


    for (noOfExec in 5..100 step 5) {
        Config.numberOfExecutables = noOfExec
        Config.outFileName = "results_noOfExecutables=$noOfExec"
        Config.reset()
        val sim = Simulator()
        val result = sim.runSimulation()
        writer.println(result.toCsvString())
        writerPerReqs.println("${Config.numberOfExecutables};${result.minLatency};${result.avgLatency.asDecimal()};${result.maxLatency}")
        result.nodeResults.forEach { writerPerNode.println("${Config.numberOfExecutables};${it.toCsvString()}") }
    }


    writer.close()
    writerPerNode.close()
    writerPerReqs.close()
}

object Config {
    val randomSeed = 0L

    /**must be in [0;100]*/
    val maxDevInPercent = 50
    val simulationDuration = 120000

    // params for nodes
    var storageCapacityEdge = 100
    var storageCapacityIntermediary = 500
    var parallelRequestCapacityEdge = 10000
    var parallelRequestCapacityIntermediary = 10000

    val avgExecLatency = 30
    val avgEdge2IntermediaryLatency = 20
    val avgIntermediary2CloudLatency = 40

    // params for executables
    var numberOfExecutables = 5
    var averageStoragePrice = 100.0
    val averageExecutableSize = 10

    //params for requests
    var requestsPerEdgePerSecond = 10000
    var avgExecutionPrice = 1.0

    //output will be stored as outFileName.csv
    var outFileName = "results_noOfExecutables=$numberOfExecutables"
    val logDetails = false

    //don't touch this part
    var random = Random(randomSeed)
    var requestsPerEdgeNode = (requestsPerEdgePerSecond * simulationDuration / 1000.0).roundToInt()


    override fun toString(): String {
        return StringBuilder("randomSeed;maxDevInPercent;simDuration;storageEdge;storageIntermediary;parallelReqsEdge;parallelReqsIntermediary;").append(
            "avgExecLatency;avgEdge2IntermediaryLatency;avgIntermediary2CloudLatency;numberOfExecutables;avgStoragePrice;avgExecutableSize;"
        ).append("requestsPerEdgePerSecond;avgExecutionPrice\n")
            .append("$randomSeed;$maxDevInPercent;$simulationDuration;$storageCapacityEdge;$storageCapacityIntermediary;")
            .append("$parallelRequestCapacityEdge;$parallelRequestCapacityIntermediary;$avgExecLatency;$avgEdge2IntermediaryLatency;")
            .append("$avgIntermediary2CloudLatency;$numberOfExecutables;$averageStoragePrice;$averageExecutableSize;$requestsPerEdgePerSecond;$avgExecutionPrice")
            .toString()
    }

    fun reset() {
        random = Random(this.randomSeed)
        requestsPerEdgeNode = (requestsPerEdgePerSecond * simulationDuration / 1000.0).roundToInt()
    }


}