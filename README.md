# Traffic Signal Simulator (PS No. 68)

**Course:** Data Structures and Algorithms – Rungta International Skills University
**Student:** Surya Pratap Singh | **Language:** Java (no external libraries)
**Concepts:** Circular Queue, Graph, Priority Queue, Greedy, File Handling

A console simulator for a 4-way intersection. Roads turn GREEN in a fixed cycle (North → East → South → West).
Emergency vehicles jump the cycle, but only through a *safe* signal transition (YELLOW → RED → all-red
clearance → GREEN), and the program verifies that no two conflicting roads are ever lit together.

---

## 1. Requirement → implementation map

| Bare-minimum requirement (from PS 68) | Where it lives |
|---|---|
| Signal phases using a **circular queue** | `CircularQueue.java` (array ring buffer) used by `TrafficController.phases` |
| Roads as a **graph** | `RoadGraph.java` (adjacency arrays; edge = conflicting roads) |
| Emergency requests in a **PriorityQueue** | `TrafficController.pending` + `EmergencyRequest.compareTo` |
| **Greedy** rule for emergency priority | `TrafficController.dispatchEmergencies()` |
| Start / next phase / emergency / status | Menu options 1-4 in `Main.java` |
| Validate road names | `RoadGraph.indexOf()` → "Invalid road. Request ignored." |
| No conflicting green signals | Neighbour check inside `TrafficController.switchTo()` |
| Load config at startup, save state on exit | `SignalConfig.load()` / `save()` on `traffic_signals.txt` |

## 2. Project structure

```
TrafficSignalSimulator/
├── src/
│   ├── Main.java               console menu, input validation, save-on-exit
│   ├── TrafficController.java  core logic: start, next phase, emergency dispatch, status
│   ├── CircularQueue.java      custom ring buffer (enqueue, dequeue, peek, rotate)
│   ├── RoadGraph.java          conflict graph + case-insensitive road lookup
│   ├── EmergencyRequest.java   comparable request (priority, then arrival order)
│   ├── VehicleType.java        AMBULANCE(1) < FIRE(2) < POLICE(3)
│   ├── Signal.java             enum RED / YELLOW / GREEN
│   └── SignalConfig.java       reads/writes traffic_signals.txt
├── tests/
│   ├── run_tests.sh            27 automated checks (4 official + 23 extra)
│   ├── default.txt             4-phase config
│   └── single_phase.txt        1-phase config (boundary test)
├── traffic_signals.txt         config + saved state (data file required by PS 68)
└── README.md
```

## 3. Key features

1. **Normal cycle** – `CircularQueue.rotate()` hands out the next road in O(1); wraps West → North automatically.
2. **Emergency override** – several requests can be entered together; they are served by vehicle priority
   (Ambulance > Fire > Police), ties by arrival order.
3. **Safe transitions** – current green goes YELLOW → RED, then an all-red clearance, then the target turns GREEN.
4. **Conflict guarantee** – before any road turns GREEN, every graph neighbour (conflicting road) must be RED,
   otherwise the program throws instead of showing an unsafe state.
5. **Fair cycle** – an emergency does not consume anyone's normal turn; the cycle resumes at the road that was due next.
   If an emergency already made a road green, that road's next normal turn is skipped.
6. **Validation** – unknown roads, unknown vehicle types, invalid menu choices, actions before `Start`, malformed config.
7. **Persistence** – `traffic_signals.txt` stores roads, conflict edges, phase order, timings, and on exit the last
   green road and served counters. Ctrl+D / Ctrl+Z (EOF) also saves. The file is written via a temp file so a crash
   cannot corrupt it.
8. **Status display** – all four signals, current green, next normal phase, pending requests, counters.

## 4. Design notes

**Graph meaning.** Vertex = approach road. Edge = the two roads' movements cross, so they must never be green together.
Default edges: N–E, E–S, S–W, W–N (opposite roads N–S and E–W are not connected).

**Greedy rule.** At every decision point serve the locally best request – highest vehicle priority, earliest arrival –
and never grant GREEN before conflicting roads are RED. If the requested road is already green, grant it with no transition.

**Why the safety check is enough.** Only one road changes state per transition, so verifying that the *target's*
neighbours are RED keeps the whole intersection conflict-free. That is O(deg(target)) instead of re-scanning all roads.

| Operation | Time | Notes |
|---|---|---|
| Next normal phase | O(1) | only the head index moves |
| Add emergency request | O(log k) | k = pending requests |
| Serve emergency | O(log k + deg) | poll + neighbour check |
| Road name lookup | O(1) | HashMap on lower-cased name |
| Memory | O(V + E + k) | arrays and one heap, no per-step allocation |

## 5. Data file format (`traffic_signals.txt`)

```
ROADS=North,East,South,West
EDGE=North,East            # one line per conflicting pair
PHASES=North,East,South,West   # normal cycle order (subset of ROADS is allowed)
GREEN_TIME=30
YELLOW_TIME=5
ALL_RED_TIME=2
# ---- saved state (auto-updated on exit) ----
LAST_GREEN=North
NORMAL_PHASES_SERVED=0
EMERGENCIES_SERVED=0
```
If the file is missing, a default one is created. A bad file gives a one-line error and exit code 1.

## 6. Build and run

```
javac -d out src/*.java            (Windows cmd: javac -d out src\*.java)
java -cp out Main traffic_signals.txt
```
Menu: `1 Start` · `2 Next phase` · `3 Emergency request` · `4 Status` · `5 Save & exit`

## 7. Test cases

| # | Input | Expected | Result |
|---|---|---|---|
| 1 | Start, no emergencies | `Signal: North GREEN` | PASS |
| 2 | Only one phase configured (`tests/single_phase.txt`), Start, Next | `Signal remains in the configured phase.` | PASS |
| 3 | Emergency request from road `Z` | `Invalid road. Request ignored.` | PASS |
| 4 | North green, emergency from South | `After a safe transition, South receives priority.` | PASS |

Run everything: `bash tests/run_tests.sh` (Git Bash on Windows, or Linux/macOS) – 27/27 pass.

**Sample session** (East = Police, South = Ambulance entered together):
```
Signal: North GREEN
Emergency request accepted: East (POLICE)
Emergency request accepted: South (AMBULANCE)
Signal: North YELLOW (5s)
Signal: North RED
All-red clearance (2s)
Signal: South GREEN [EMERGENCY - AMBULANCE]
After a safe transition, South receives priority.
Signal: South YELLOW (5s)
Signal: South RED
All-red clearance (2s)
Signal: East GREEN [EMERGENCY - POLICE]
After a safe transition, East receives priority.
```

## 8. Implementation plan

| Step | Work | Status |
|---|---|---|
| 1 | Read PS 68, list requirements and test cases | done |
| 2 | `CircularQueue` + `Signal` + `VehicleType` | done |
| 3 | `RoadGraph` (conflict graph, name lookup) | done |
| 4 | `EmergencyRequest` + PriorityQueue ordering | done |
| 5 | `SignalConfig` file load/save + default file | done |
| 6 | `TrafficController`: switchTo (safe sequence), nextPhase, dispatch, status | done |
| 7 | `Main` menu, input validation, save-on-exit | done |
| 8 | Automated tests (4 official + edge cases) | done |
| 9 | Screenshots of the 4 test cases for the report | **to do** |
| 10 | Report (problem, design, DS choice, complexity, outputs) | **to do** |
| 11 | Push `src/`, `tests/`, `traffic_signals.txt`, README, report to GitHub, submit before deadline | **to do** |

## 9. Possible extensions

- Let non-conflicting roads (North + South) share green using the graph (greedy independent set).
- Real timers (`ScheduledExecutorService`) using GREEN_TIME / YELLOW_TIME instead of printed durations.
- Per-road vehicle counts to extend the green of busy roads.
- Swing/JavaFX view of the intersection.

## 10. Viva quick answers

- **Why a circular queue?** The cycle never ends; rotate() is O(1) and needs no shifting or growth.
- **Why a graph?** Safety is about *which roads conflict*; adjacency answers that in O(deg).
- **Why a priority queue?** Many requests can wait; poll() always gives the most urgent in O(log k).
- **Where is the greedy choice?** Each step serves the best available request without looking ahead.
- **Is it deadlock/starvation free?** The normal cycle is never removed, only paused; emergencies are finite per request.
