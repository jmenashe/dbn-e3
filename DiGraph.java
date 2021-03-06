import java.util.regex.*;
import java.util.*;

/**
 * Use DiGraph.graphFromString(STRINGSTRINGNGNGN) to create a digraph.
 */
public class DiGraph {
    private Map<Integer, List<Integer>> edges;

    public DiGraph() {
        edges = new HashMap<>();
    }

    public Map<Integer, List<Integer>> edges() {
        return edges;
    }

    /**
     * Insert a directed edge into graph
     */
    public void addEdge(int from, int to) {
        List<Integer> tos = edges.get(from);

        if (tos == null) {
            tos = new ArrayList<>();
            edges.put(from, tos);
        }

        tos.add(to);
    }

    /**
     * Parse DiGraph from a string
     * 
     * @param edgeString looks like "[(1, 2), (5, 4), ...]"
     */
    public static DiGraph graphFromString(String edgeString) {
        DiGraph graph = new DiGraph();

        Pattern pairP = Pattern.compile("\\((?<from>\\d+), ?(?<to>\\d+)\\)");
        Matcher pairM = pairP.matcher(edgeString);

        while (pairM.find()) {
            int from = Integer.parseInt(pairM.group("from"));
            int to = Integer.parseInt(pairM.group("to"));
            graph.addEdge(from, to);
        }

        for (int self : graph.edges.keySet()) {
            graph.addEdge(self, self);
        }

        return graph;
    }

    /**
     * Test this shit!
     */
    public static void main(String[] args) {
        DiGraph graph = graphFromString(args[0]);
        System.out.println(graph.edges());
    }
}
