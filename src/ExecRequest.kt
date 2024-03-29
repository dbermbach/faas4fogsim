package de.tuberlin.mcc.faas4fogsim

import java.text.DecimalFormat
import kotlin.math.roundToInt

data class ExecRequest(val executable: Executable, val execPrice: Double, val plannedStart: Int) {
    var totalLatency: Int = 0
    var executionNode: String = "not executed"
    var executionNodeType: NodeType? = null
    var actualStart = plannedStart

    /**
     * invoke when executing this request
     * @param node name of the node executing this request
     * @param nodeType type (cloud/edge/intermediary) of the node executing the request
     */
    fun execute(node: String, nodeType: NodeType, latency: Int) {
        totalLatency += latency
        executionNode = node
        executionNodeType = nodeType
        //println("Executed ${executable.name} on node $node ($nodeType): price=$execPrice, totalLatency = $totalLatency, execLatency=${executable.execLatency}")
    }

    /**
     * invoke when pushing a request to another node
     * @param latency the latency for transmitting the request to the next node
     */
    fun pushTowardsCloud(latency: Int) {
        //  println("push2Cloud invoked: $latency")
        totalLatency += latency
        actualStart += latency
    }

    fun getCSVString() =
        "$plannedStart;$actualStart;${executable.name};${executionNode};${executionNodeType};${execPrice.asDecimal()};${totalLatency}"


    fun getCSVHeader() = "planned_Start;actual_start;executable;executing_node;node_type;price;total_latency"

}


data class Executable(val size: Int, val name: String, val storePrice: Double, val execLatency: Int)


fun Number.asPercent() = DecimalFormat("0.0000%").format(this)
fun Number.asDecimal() = DecimalFormat("0.0000").format(this)


fun Int.withVariance(): Int {
    val temp = (Config.random.nextDouble() * Config.maxDevInPercent * this / 100.0).roundToInt()
    return if (Config.random.nextBoolean()) this + temp else this - temp
}

fun Double.withVariance(): Double {
    val temp = Config.random.nextDouble() * this * Config.maxDevInPercent / 100.0
    return if (Config.random.nextBoolean()) this + temp else this - temp
}



