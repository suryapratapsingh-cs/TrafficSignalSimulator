import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Undirected graph of the intersection.
 *   vertex = approach road, edge = two roads whose movements CONFLICT (cannot show green together).
 * Adjacency is stored as int[][] (built once) so conflict checks never allocate or re-scan.
 */
public final class RoadGraph {
    private final String[] names;
    private final Map<String, Integer> index = new HashMap<>();   // lower-case name -> id
    private final int[][] adj;

    public RoadGraph(List<String> roads, List<String[]> edges) {
        int n = roads.size();
        names = new String[n];
        for (int i = 0; i < n; i++) {
            String r = roads.get(i);
            names[i] = r;
            if (index.put(r.toLowerCase(), i) != null)
                throw new IllegalArgumentException("Duplicate road: " + r);
        }
        boolean[][] m = new boolean[n][n];
        int[] deg = new int[n];
        for (String[] e : edges) {
            int a = indexOf(e[0]), b = indexOf(e[1]);
            if (a < 0 || b < 0) throw new IllegalArgumentException("EDGE uses unknown road: " + e[0] + "," + e[1]);
            if (a == b) throw new IllegalArgumentException("EDGE cannot connect a road to itself: " + e[0]);
            if (!m[a][b]) { m[a][b] = m[b][a] = true; deg[a]++; deg[b]++; }
        }
        adj = new int[n][];
        for (int i = 0; i < n; i++) {
            int[] row = new int[deg[i]];
            int k = 0;
            boolean[] mi = m[i];
            for (int j = 0; j < n; j++) if (mi[j]) row[k++] = j;
            adj[i] = row;
        }
    }

    /** Case-insensitive lookup. Returns -1 for an unknown road (null-safe). */
    public int indexOf(String road) {
        if (road == null) return -1;
        Integer i = index.get(road.trim().toLowerCase());
        return i == null ? -1 : i;
    }

    public String name(int i)      { return names[i]; }
    public int size()              { return names.length; }
    public int[] conflictsOf(int i){ return adj[i]; }

    public String describe() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.length; i++) {
            sb.append("  ").append(names[i]).append(" conflicts with: ");
            int[] row = adj[i];
            if (row.length == 0) sb.append("-");
            for (int k = 0; k < row.length; k++) {
                if (k > 0) sb.append(", ");
                sb.append(names[row[k]]);
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
