package net.cakemc.meshing.redundant.monitor

object MetricsCollector {

    fun collect(): Triple<Double, Double, Double> {
        val cpu = getCpuUsage()
        val ram = getRamUsage()
        val net = getNetworkUsage()
        return Triple(cpu, ram, net)
    }

    private fun getCpuUsage(): Double {
        val osBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean()
        return (osBean as com.sun.management.OperatingSystemMXBean).systemCpuLoad.coerceAtLeast(0.0)
    }

    private fun getRamUsage(): Double {
        val runtime = Runtime.getRuntime()
        val used = runtime.totalMemory() - runtime.freeMemory()
        return used.toDouble() / runtime.maxMemory().toDouble()
    }

    private var lastBytes = 0L
    private fun getNetworkUsage(): Double {
        // Stub: Replace with real value using Sigar, OSHI, etc.
        return Math.random() * 0.5 // Simulate 0-50% load
    }
}
