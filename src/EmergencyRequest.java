/**
 * One emergency-vehicle request. Ordering (used by PriorityQueue):
 * 1) lower vehicle priority number first, 2) earlier arrival (seq) first.
 */
public final class EmergencyRequest implements Comparable<EmergencyRequest> {
    public final int road;
    public final VehicleType type;
    public final long seq;

    public EmergencyRequest(int road, VehicleType type, long seq) {
        this.road = road;
        this.type = type;
        this.seq = seq;
    }

    @Override
    public int compareTo(EmergencyRequest o) {
        int c = Integer.compare(type.priority, o.type.priority);
        return c != 0 ? c : Long.compare(seq, o.seq);
    }
}
