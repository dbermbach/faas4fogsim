package de.tuberlin.mcc.faas4fogsim

import java.io.File

import java.util.Random
import kotlin.math.roundToInt
import kotlin.text.StringBuilder


class Simulator() {

    val cloud = mutableSetOf<ComputeNode>()
    val intermediary = mutableSetOf<ComputeNode>()
    val edge = mutableSetOf<ComputeNode>()
    val executables = mutableMapOf<String, Executable>()
    val requests: MutableMap<Int, MutableMap<ComputeNode, MutableList<ExecRequest>>> = mutableMapOf()


    /**
     * defines the network topology of cloud, intermediary and edge nodes
     */
    fun defineTopology() {
        //TODO edit here and insert network topology for simulation. EXAMPLE:


        var cNode = buildCloudNode()
        var iNode = buildIntermediaryNode("intermediary")
        var eNode = buildEdgeNode("edge")
        iNode.connectIntermediaryToCloud(cNode)
        eNode.connectEdgeToIntermediary(iNode)


    }

    /**
     * defines the set of executables for functions that shall be deployed on the topology
     */
    fun defineExecutables() {
        //define executables and their parameter settings here manually or use Config
        //for (x in 1..5) defineExecutable(1, "executable$x", x.toDouble(), 2)

        for (x in 1..Config.numberOfExecutables) defineExecutable(
            Config.averageExecutableSize.withVariance(),
            "executable$x",
            Config.averageStoragePrice.withVariance(),
            Config.avgExecLatency
        )


    }

    /**
     * defines request arrival rates and characteristics
     */
    fun defineRequests() {
        // define request arrival rates and parameter settings here or use config
//        for (exec in executables.values) {
//            defineRequest(exec, 1.0, 0, edge.first())
//        }

        val reqs = mutableListOf<Triple<Int, ComputeNode, ExecRequest>>()
        for (node in edge) {
            //var counter = 0
            for (reqNo in 1..Config.requestsPerEdgeNode) {
                val timestamp = Config.random.nextInt(Config.simulationDuration)
                val executable = executables.values.elementAt(Config.random.nextInt(Config.numberOfExecutables))
                val price = Config.avgExecutionPrice.withVariance()
                reqs.add(Triple(timestamp, node, ExecRequest(executable, price, timestamp)))
                //  counter++
            }
            //println("defined $counter requests for node ${node.name}")
        }
        reqs.sortedBy { it.first }.forEach { addRequestToSimulation(it.third, it.first, it.second) }


    }


    fun offerRequests() {

        for ((timestamp, nodeToReqMap) in requests.toSortedMap()) {
            for ((node, reqList) in nodeToReqMap) {
                node.offerRequests(timestamp, reqList)
            }
        }
    }

    /**
     * writes all results to the file system
     */
    fun persistResults() {
        val writer = File("${Config.outFileName}.csv").printWriter()
        writer.println(Config.toString() + "\n\n")
        writer.println(cloud.first()?.getStatsStringHeader())
        cloud.union(intermediary).union(edge).forEach { writer.println(it.getStatsString()) }
        writer.println("\n\n\n")
        writer.println(ExecRequest(executables.values.first(), 1.0, -1).getCSVHeader())
        requests.forEach { timestamp, nodeToRequestsMap ->
            nodeToRequestsMap.forEach {
                it.value.forEach {
                    writer.println(it.getCSVString())
                }
            }
        }
        writer.close()
    }


    private fun addRequestToSimulation(request: ExecRequest, timestamp: Int, node: ComputeNode) {
        var nodeToReqList = requests[timestamp]
        if (nodeToReqList == null) {
            nodeToReqList = mutableMapOf()
            requests[timestamp] = nodeToReqList
        }
        var reqList = nodeToReqList[node]
        if (reqList == null) {
            reqList = mutableListOf()
            nodeToReqList[node] = reqList
        }
        reqList.add(request)
    }

    private fun defineExecutable(size: Int, name: String, storePrice: Double, execLatency: Int) {
        val exec = Executable(size, name, storePrice, execLatency)
        cloud.union(intermediary).union(edge).forEach { it.offerExecutable(exec) }
        executables[exec.name] = exec
    }


    private fun buildCloudNode(): ComputeNode = buildNode(NodeType.CLOUD, Int.MAX_VALUE, Int.MAX_VALUE, "Cloud")
    private fun buildIntermediaryNode(name: String): ComputeNode = buildNode(
        NodeType.INTERMEDIARY,
        Config.storageCapacityIntermediary,
        Config.parallelRequestCapacityIntermediary,
        name
    )

    private fun buildEdgeNode(name: String): ComputeNode = buildNode(
        NodeType.EDGE,
        Config.storageCapacityEdge,
        Config.parallelRequestCapacityEdge,
        name
    )

    private fun buildNode(
        nodeType: NodeType,
        storageCapacity: Int,
        parallelRequestCapacity: Int,
        name: String
    ): ComputeNode {
        val node = ComputeNode(
            nodeType,
            storageCapacity,
            parallelRequestCapacity,
            name,
            Config.simulationDuration
        )
        when (nodeType) {
            NodeType.EDGE -> edge.add(node)
            NodeType.INTERMEDIARY -> intermediary.add(node)
            NodeType.CLOUD -> cloud.add(node)
        }
        return node
    }

    fun ComputeNode.connectEdgeToIntermediary(intermediary: ComputeNode) =
        this.connectTo(intermediary, Config.avgEdge2IntermediaryLatency)

    fun ComputeNode.connectIntermediaryToCloud(cloud: ComputeNode) =
        this.connectTo(cloud, Config.avgIntermediary2CloudLatency)

    fun runSimulation(): SimulationResult {
        defineTopology()
        println("Added the following nodes:")
        edge.union(intermediary).union(cloud).forEach { println(it) }
        defineExecutables()
        println("Added ${executables.size} executables")
        defineRequests()
        println("Defined ${requests.flatMap { it.value.flatMap { it.value } }.count()} requests")
        offerRequests()
        if (Config.logDetails) persistResults()

        val nodeResults = mutableListOf<NodeResult>()
        for (node in edge.union(intermediary.union(cloud))) {
            val (processed, delegated) = node.getRequestStats()
            val (procEarning, storeEarning) = node.getEarningStats()
            val noOfExec = node.executables.size
            nodeResults.add(NodeResult(node.name, node.nodeType, procEarning, storeEarning, processed, delegated,noOfExec))
        }
        var min = Integer.MAX_VALUE
        var max = Integer.MIN_VALUE
        var sum = 0
        var counter = 0
        requests.flatMap { it.value.flatMap { it.value } }.forEach {
            counter++
            sum += it.totalLatency
            if (it.totalLatency < min) min = it.totalLatency
            if (it.totalLatency > max) max = it.totalLatency
        }
        return SimulationResult(Config.toString(), nodeResults, min, max, sum.div(counter.toDouble()))
    }

}

data class SimulationResult(
    val configAsCSV: String,
    val nodeResults: Collection<NodeResult>,
    val minLatency: Int,
    val maxLatency: Int,
    val avgLatency: Double
) {
    fun toCsvString(): String {
        val result = StringBuilder("Config:\n").append(Config).append("\nRequest Latency:\nmin;avg;max\n")
            .append("$minLatency;${avgLatency.asDecimal()};$maxLatency\n").append(getNodeResultHeader())
        for (res in nodeResults) {
            result.append("\n${res.toCsvString()}")
        }
        return result.toString()
    }

    fun getNodeResultHeader() = "name;type;processing_earnings;storage_earnings;processed_reqs;delegated_reqs;no_executables"
}

data class NodeResult(
    val nodeName: String,
    val nodeType: NodeType,
    val processingEarnings: Double,
    val storageEarnings: Double,
    val processedRequests: Int,
    val delegatedRequests: Int,
    val numberOfStoredExecutables: Int
) {
    fun toCsvString() =
        "$nodeName;$nodeType;${processingEarnings.asDecimal()};${storageEarnings.asDecimal()};$processedRequests;$delegatedRequests;$numberOfStoredExecutables"

}