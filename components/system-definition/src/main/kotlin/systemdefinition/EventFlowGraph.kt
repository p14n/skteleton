package systemdefinition

/**
 * Internal graph representation for analyzing event flows.
 *
 * EventFlowGraph models the system as a directed graph where:
 * - Nodes represent handlers
 * - Edges represent event flows between handlers
 *
 * This structure enables detection of circular flows and other
 * graph-based analysis.
 *
 * @property adjacencyList Maps handler names to sets of handlers they flow to
 */
internal data class EventFlowGraph(
    val adjacencyList: Map<String, Set<String>>
) {
    /**
     * Detect circular flows using depth-first search.
     *
     * @return List of cycles, where each cycle is a list of handler names
     */
    fun detectCycles(): List<List<String>> {
        val cycles = mutableListOf<List<String>>()
        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()
        val path = mutableListOf<String>()

        fun dfs(node: String) {
            visited.add(node)
            recursionStack.add(node)
            path.add(node)

            adjacencyList[node]?.forEach { neighbor ->
                if (!visited.contains(neighbor)) {
                    dfs(neighbor)
                } else if (recursionStack.contains(neighbor)) {
                    // Found a cycle - extract the cycle from path
                    val cycleStart = path.indexOf(neighbor)
                    val cycle = path.subList(cycleStart, path.size) + neighbor
                    cycles.add(cycle)
                }
            }

            path.removeAt(path.lastIndex)
            recursionStack.remove(node)
        }

        adjacencyList.keys.forEach { node ->
            if (!visited.contains(node)) {
                dfs(node)
            }
        }

        return cycles
    }

    companion object {
        /**
         * Build an EventFlowGraph from a SystemDefinition.
         *
         * Creates edges between handlers based on event flows:
         * If handler A returns event X and handler B receives event X,
         * there's an edge from A to B.
         */
        fun fromSystemDefinition(system: SystemDefinition): EventFlowGraph {
            val adjacencyList = mutableMapOf<String, MutableSet<String>>()

            // Initialize all handlers in the graph
            system.handlers.forEach { handler ->
                adjacencyList[handler.name] = mutableSetOf()
            }

            // Build edges based on event flows
            system.events.forEach { event ->
                val producers = event.producedBy
                val consumers = event.consumedBy

                producers.forEach { producer ->
                    consumers.forEach { consumer ->
                        if (producer != consumer) {
                            adjacencyList[producer]?.add(consumer)
                        }
                    }
                }
            }

            return EventFlowGraph(adjacencyList)
        }
    }
}

