import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Console menu. Usage: java Main [path/to/traffic_signals.txt] */
public final class Main {

    public static void main(String[] args) throws IOException {
        final PrintStream out = System.out;
        final Path file = Paths.get(args.length > 0 ? args[0] : "traffic_signals.txt");

        SignalConfig cfg;
        TrafficController ctl;
        try {
            cfg = SignalConfig.load(file);
            ctl = new TrafficController(cfg, out);
        } catch (IllegalArgumentException e) {
            System.err.println("Config error in " + file + ": " + e.getMessage());
            System.exit(1);
            return;
        }

        out.println("Loaded " + file + " (" + cfg.roads.size() + " roads, " + cfg.phases.size() + " phase(s)).");
        out.print("Road conflict graph:\n" + ctl.describeGraph());
        if (!"-".equals(cfg.lastGreen))
            out.println("Previous session ended on " + cfg.lastGreen + " GREEN (normal phases served: "
                    + cfg.normalServed + ", emergencies served: " + cfg.emergencyServed + ").");

        final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            out.println();
            out.println("=========== TRAFFIC SIGNAL SIMULATOR ===========");
            out.println("1. Start simulator");
            out.println("2. Next phase");
            out.println("3. Emergency request");
            out.println("4. Status display");
            out.println("5. Save & exit");
            String choice = prompt(in, out, "Enter choice: ");
            if (choice == null) { saveAndExit(ctl, cfg, file, out); return; }   // EOF -> save too
            switch (choice.trim()) {
                case "1": ctl.start(); break;
                case "2": ctl.nextPhase(); break;
                case "3": emergencyFlow(in, out, ctl); break;
                case "4": ctl.status(); break;
                case "5": saveAndExit(ctl, cfg, file, out); return;
                default:  out.println("Invalid choice. Enter 1-5.");
            }
        }
    }

    /** Collects one or more emergency requests, then lets the controller dispatch them by priority. */
    private static void emergencyFlow(BufferedReader in, PrintStream out, TrafficController ctl) throws IOException {
        boolean queued = false;
        while (true) {
            String road = prompt(in, out, "Enter road of emergency vehicle: ");
            if (road == null) break;
            int idx = ctl.resolveRoad(road);
            if (idx < 0) {
                out.println("Invalid road. Request ignored.");
                if (!queued) break;                       // nothing pending: back to menu
            } else if (!ctl.isStarted()) {
                out.println("Simulator not started. Choose option 1 first. Request ignored.");
                break;
            } else {
                VehicleType t;
                while (true) {
                    String s = prompt(in, out, "Vehicle type [1=Ambulance, 2=Fire, 3=Police] (Enter = 1): ");
                    t = VehicleType.parse(s);               // null input -> AMBULANCE (EOF safe)
                    if (t != null) break;
                    out.println("Invalid vehicle type. Try again.");
                }
                ctl.request(idx, t);
                queued = true;
            }
            String more = prompt(in, out, "Add another emergency request? (y/n): ");
            if (more == null || !more.trim().equalsIgnoreCase("y")) break;
        }
        if (queued) ctl.dispatchEmergencies();
    }

    private static void saveAndExit(TrafficController ctl, SignalConfig cfg,
                                    Path file, PrintStream out) {
        ctl.exportState(cfg);
        try {
            cfg.save(file);
            out.println("State saved to " + file + ". Goodbye.");
        } catch (IOException e) {
            out.println("Could not save state: " + e.getMessage());
        }
    }

    private static String prompt(BufferedReader in, PrintStream out, String text) throws IOException {
        out.print(text);
        out.flush();
        return in.readLine();
    }
}
