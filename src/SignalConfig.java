import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reads / writes traffic_signals.txt (plain KEY=VALUE text, '#' = comment).
 * Config keys : ROADS, EDGE (repeatable), PHASES, GREEN_TIME, YELLOW_TIME, ALL_RED_TIME
 * State keys  : LAST_GREEN, NORMAL_PHASES_SERVED, EMERGENCIES_SERVED  (rewritten on exit)
 */
public final class SignalConfig {
    public final List<String> roads = new ArrayList<>();
    public final List<String[]> edges = new ArrayList<>();
    public final List<String> phases = new ArrayList<>();
    public int greenTime = 30, yellowTime = 5, allRedTime = 2;

    public String lastGreen = "-";
    public long normalServed, emergencyServed;

    public static SignalConfig defaults() {
        SignalConfig c = new SignalConfig();
        c.roads.addAll(Arrays.asList("North", "East", "South", "West"));
        // Crossing roads conflict; opposite roads (North-South, East-West) do not.
        c.edges.add(new String[]{"North", "East"});
        c.edges.add(new String[]{"East", "South"});
        c.edges.add(new String[]{"South", "West"});
        c.edges.add(new String[]{"West", "North"});
        c.phases.addAll(c.roads);
        return c;
    }

    /** Loads the file; if it does not exist, writes a default one and uses it. */
    public static SignalConfig load(Path file) throws IOException {
        if (!Files.exists(file)) {
            SignalConfig d = defaults();
            d.save(file);
            System.out.println("Config not found. Created default " + file + ".");
            return d;
        }
        SignalConfig c = new SignalConfig();
        List<String> lines = Files.readAllLines(file);
        for (int i = 0, n = lines.size(); i < n; i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty() || line.charAt(0) == '#') continue;
            int eq = line.indexOf('=');
            if (eq < 0) throw new IllegalArgumentException("Line " + (i + 1) + ": expected KEY=VALUE");
            String key = line.substring(0, eq).trim().toUpperCase();
            String val = line.substring(eq + 1).trim();
            try {
                switch (key) {
                    case "ROADS":  c.roads.addAll(csv(val)); break;
                    case "PHASES": c.phases.addAll(csv(val)); break;
                    case "EDGE": {
                        List<String> p = csv(val);
                        if (p.size() != 2) throw new IllegalArgumentException("EDGE needs exactly two roads");
                        c.edges.add(new String[]{p.get(0), p.get(1)});
                        break;
                    }
                    case "GREEN_TIME":   c.greenTime = Integer.parseInt(val); break;
                    case "YELLOW_TIME":  c.yellowTime = Integer.parseInt(val); break;
                    case "ALL_RED_TIME": c.allRedTime = Integer.parseInt(val); break;
                    case "LAST_GREEN":   c.lastGreen = val.isEmpty() ? "-" : val; break;
                    case "NORMAL_PHASES_SERVED": c.normalServed = Long.parseLong(val); break;
                    case "EMERGENCIES_SERVED":   c.emergencyServed = Long.parseLong(val); break;
                    default: throw new IllegalArgumentException("unknown key '" + key + "'");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Line " + (i + 1) + ": '" + val + "' is not a valid number");
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Line " + (i + 1) + ": " + e.getMessage());
            }
        }
        c.validate();
        return c;
    }

    private static List<String> csv(String v) {
        List<String> out = new ArrayList<>();
        for (String s : v.split(",")) {
            s = s.trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    private void validate() {
        if (roads.isEmpty())  throw new IllegalArgumentException("ROADS is missing or empty");
        if (phases.isEmpty()) throw new IllegalArgumentException("PHASES is missing or empty");
        if (greenTime <= 0 || yellowTime <= 0 || allRedTime < 0)
            throw new IllegalArgumentException("GREEN_TIME/YELLOW_TIME must be > 0 and ALL_RED_TIME >= 0");
    }

    /** Writes config + state. Writes to a temp file first so a crash cannot corrupt the original. */
    public void save(Path file) throws IOException {
        List<String> out = new ArrayList<>();
        out.add("# Traffic Signal Simulator - configuration and saved state");
        out.add("# EDGE = two roads whose movements conflict (cannot be green together)");
        out.add("ROADS=" + String.join(",", roads));
        for (String[] e : edges) out.add("EDGE=" + e[0] + "," + e[1]);
        out.add("PHASES=" + String.join(",", phases));
        out.add("GREEN_TIME=" + greenTime);
        out.add("YELLOW_TIME=" + yellowTime);
        out.add("ALL_RED_TIME=" + allRedTime);
        out.add("");
        out.add("# ---- saved state (auto-updated on exit) ----");
        out.add("LAST_GREEN=" + lastGreen);
        out.add("NORMAL_PHASES_SERVED=" + normalServed);
        out.add("EMERGENCIES_SERVED=" + emergencyServed);
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.write(tmp, out);
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
    }
}
