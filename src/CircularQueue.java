/**
 * Fixed-capacity circular queue (ring buffer) of road indices.
 * All operations are O(1); no modulo, no shifting, no allocation after construction.
 */
public final class CircularQueue {
    private final int[] buf;
    private int head;   // index of front element
    private int size;

    public CircularQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be > 0");
        buf = new int[capacity];
    }

    public boolean isEmpty() { return size == 0; }
    public boolean isFull()  { return size == buf.length; }
    public int size()        { return size; }

    public void enqueue(int v) {
        if (size == buf.length) throw new IllegalStateException("Circular queue is full");
        int tail = head + size;
        if (tail >= buf.length) tail -= buf.length;
        buf[tail] = v;
        size++;
    }

    public int dequeue() {
        if (size == 0) throw new IllegalStateException("Circular queue is empty");
        int v = buf[head];
        if (++head == buf.length) head = 0;
        size--;
        return v;
    }

    public int peek() {
        if (size == 0) throw new IllegalStateException("Circular queue is empty");
        return buf[head];
    }

    /**
     * Dequeue + enqueue in one step: returns the front element and sends it to the back.
     * In a ring buffer the back slot is exactly the old head slot, so only 'head' moves.
     */
    public int rotate() {
        if (size == 0) throw new IllegalStateException("Circular queue is empty");
        int v = buf[head];
        if (++head == buf.length) head = 0;
        return v;
    }
}
