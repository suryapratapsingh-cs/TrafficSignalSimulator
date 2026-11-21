import java.io.PrintStream;
import java.util.Arrays;
import java.util.PriorityQueue;

/**
 * Core logic of the intersection.
 *
 * Data structures
 *   CircularQueue          -> normal phase cycle (North, East, South, West, ...)
 *   RoadGraph              -> which roads conflict (used to prove every transition is safe)
 *   PriorityQueue          -> pending emergency requests
 *
 * Greedy rule (emergency dispatch)
 *   Always serve the best pending request first: highest vehicle priority, ties by arrival order.
 *   A request is only turned GREEN after the current green goes YELLOW -> RED and every road
 *   that conflicts with the target (graph neighbours) is verified RED.
 */
public final class TrafficController {
    private final RoadGraph graph;
    private final CircularQueue phases;
    private final PriorityQueue<EmergencyRequest> pending = new PriorityQueue<>();
    private final Signal[] signals;
    private final int yellowTime, allRedTime;
    private final PrintStream out;

    private int green = -1;          // road currently GREEN, -1 = none
    private boolean started;
    private long seq;                // arrival counter for FIFO tie-break
    private long normalServed, emergencyServed;

    public TrafficController(SignalConfig cfg, PrintStream out) {
        this.out = out;
        this.graph = new RoadGraph(cfg.roads, cfg.edges);
        int n = graph.size();
        signals = new Signal[n];
        Arrays.fill(signals, Signal.RED);
        yellowTime = cfg.yellowTime;
        allRedTime = cfg.allRedTime;
        normalServed = cfg.normalServed;
        emergencyServed = cfg.emergencyServed;

        phases = new CircularQueue(cfg.phases.size());
        boolean[] seen = new boolean[n];
        for (String p : cfg.phases) {
            int i = graph.indexOf(p);
            if (i < 0)  throw new IllegalArgumentException("PHASES uses unknown road: " + p);
            if (seen[i]) throw new IllegalArgumentException("PHASES lists a road twice: " + p);
            seen[i] = true;
            phases.enqueue(i);
        }
    }

    public boolean isStarted()            { return started; }
    public int resolveRoad(String road)   { return graph.indexOf(road); }
    public String roadName(int i)         { return graph.name(i); }
    public String describeGraph()         { return graph.describe(); }

    /** Option 1: first configured phase turns GREEN, everything else is RED. */
    public void start() {
        if (started) { out.println("Simulator is already running."); return; }
        started = true;
        switchTo(phases.rotate(), "");
        normalServed++;
    }

    /** Option 2: advance the circular queue to the next normal phase. */
    public void nextPhase() {
        if (!started) { out.println("Simulator not started. Choose option 1 first."); return; }
        int target = phases.rotate();
        if (target == green) {
            if (phases.size() == 1) {
                out.println("Signal remains in the configured phase.");
                return;
            }
            // An emergency GREEN already served this road; skip to the following phase.
            out.println(graph.name(target) + " is already GREEN (emergency turn counted). Moving on.");
            target = phases.rotate();
        }
        switchTo(target, "");
        normalServed++;
    }

    /** Option 3 (step 1): road must already be validated with resolveRoad(). */
    public void request(int road, VehicleType type) {
        pending.add(new EmergencyRequest(road, type, seq++));
        out.println("Emergency request accepted: " + graph.name(road) + " (" + type + ")");
    }

    /** Option 3 (step 2): greedy dispatch - repeatedly serve the best pending request. */
    public void dispatchEmergencies() {
        EmergencyRequest r;
        while ((r = pending.poll()) != null) {
            String name = graph.name(r.road);
            if (switchTo(r.road, " [EMERGENCY - " + r.type + "]"))
                out.println("After a safe transition, " + name + " receives priority.");
            else
                out.println(name + " is already GREEN. Priority granted without any transition.");
            emergencyServed++;
        }
    }

    /**
     * Moves GREEN to 'target' using the safe sequence
     *   current GREEN -> YELLOW -> RED -> (all-red clearance) -> target GREEN.
     * Returns false (and does nothing) if 'target' is already GREEN.
     */
    private boolean switchTo(int target, String tag) {
        final Signal[] sig = signals;
        final int cur = green;
        if (cur == target) return false;

        if (cur >= 0) {
            String curName = graph.name(cur);
            sig[cur] = Signal.YELLOW;
            out.println("Signal: " + curName + " YELLOW (" + yellowTime + "s)");
            sig[cur] = Signal.RED;
            out.println("Signal: " + curName + " RED");
            if (allRedTime > 0) out.println("All-red clearance (" + allRedTime + "s)");
        }

        // Safety invariant: only 'target' changes, so checking its graph neighbours is enough
        // to guarantee no two conflicting roads are ever non-RED together.
        for (int nb : graph.conflictsOf(target))
            if (sig[nb] != Signal.RED)
                throw new IllegalStateException("Conflict: " + graph.name(nb) + " is not RED");

        sig[target] = Signal.GREEN;
        green = target;
        out.println("Signal: " + graph.name(target) + " GREEN" + tag);
        return true;
    }

    /** Option 4. */
    public void status() {
        StringBuilder sb = new StringBuilder(256);
        sb.append("===== INTERSECTION STATUS =====\n");
        sb.append("Simulator           : ").append(started ? "RUNNING" : "NOT STARTED").append('\n');
        final int n = signals.length;
        int w = 0;
        for (int i = 0; i < n; i++) w = Math.max(w, graph.name(i).length());
        for (int i = 0; i < n; i++) {
            String nm = graph.name(i);
            sb.append("  ").append(nm);
            for (int k = nm.length(); k < w; k++) sb.append(' ');
            sb.append(" : ").append(signals[i]).append('\n');
        }
        sb.append("Current green       : ").append(green >= 0 ? graph.name(green) : "none").append('\n');
        sb.append("Next normal phase   : ").append(graph.name(phases.peek())).append('\n');
        if (pending.isEmpty()) {
            sb.append("Pending emergencies : none\n");
        } else {
            EmergencyRequest[] arr = pending.toArray(new EmergencyRequest[0]);
            Arrays.sort(arr);                       // PriorityQueue iteration is not sorted
            sb.append("Pending emergencies : ").append(arr.length).append('\n');
            for (EmergencyRequest r : arr)
                sb.append("  ").append(graph.name(r.road)).append(" (").append(r.type).append(")\n");
        }
        sb.append("Normal phases served: ").append(normalServed).append('\n');
        sb.append("Emergencies served  : ").append(emergencyServed).append('\n');
        out.print(sb);
    }

    /** Copies the state that must survive a restart into the config object. */
    public void exportState(SignalConfig cfg) {
        if (green >= 0) cfg.lastGreen = graph.name(green);
        cfg.normalServed = normalServed;
        cfg.emergencyServed = emergencyServed;
    }
}
