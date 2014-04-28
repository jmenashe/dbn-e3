import java.util.regex.*;
import java.util.*;

public class Graph {
    private static Map<Integer, List<Integer>> edges;

    /**
     * Insert a directed edge into graph
     */
    private static void addEdge(int from, int to) {
        List<Integer> tos = edges.get(from);

        if (tos == null) {
            tos = new ArrayList<>();
            edges.put(from, tos);
        }

        tos.add(to);
    }

    /**
     * Parse Graph from a string
     *
     * @param edgeString looks like "[(1, 2), (5, 4), ...]"
     */
    public static Map<Integer, List<Integer>> graphFromString(String edgeString) {
        edges = new HashMap<>();

        Pattern pairP = Pattern.compile("\\((?<from>\\d+), ?(?<to>\\d+)\\)");
        Matcher pairM = pairP.matcher(edgeString);

        while (pairM.find()) {
            int from = Integer.parseInt(pairM.group("from"));
            int to = Integer.parseInt(pairM.group("to"));
            addEdge(from, to);
        }

        // Every node depends on self
        for (int self : edges.keySet()) {
            addEdge(self, self);
        }

        // Remove edge to "last" node
        for (int i = 0; i < edges.size(); i++) {
            if (edges.get(i).contains(E3Agent.NBR_REACHES)) {
                edges.get(i)
                        .remove(edges.get(i)
                                .indexOf(E3Agent.NBR_REACHES));
            }
        }

        return edges;
    }
}