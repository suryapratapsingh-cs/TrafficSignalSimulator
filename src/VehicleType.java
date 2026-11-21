/** Emergency vehicle classes. Lower priority number = served first. */
public enum VehicleType {
    AMBULANCE(1), FIRE(2), POLICE(3);

    public final int priority;

    VehicleType(int priority) { this.priority = priority; }

    /** Accepts 1/2/3 or a name (case-insensitive). Blank -> AMBULANCE. Returns null if unknown. */
    public static VehicleType parse(String s) {
        if (s == null || s.trim().isEmpty()) return AMBULANCE;
        switch (s.trim().toUpperCase()) {
            case "1": case "AMBULANCE": return AMBULANCE;
            case "2": case "FIRE": case "FIRE_TRUCK": return FIRE;
            case "3": case "POLICE": return POLICE;
            default: return null;
        }
    }
}
