# Frodo Interactor — Import-Only Runner

Runs a suite of tests from a JSON/JSONL file against one Java solver (A* or Backtracking), and produces a summary with wins/losses and runtime statistics.

## Location
- Script: `src/tester.py`, where src - directory with algorithms codes
- Default output directory: `analysis/imported`

## Requirements
- Windows with Python 3.9+
- Java JDK installed
- Compiled Java solvers:
  - `Astar.java` → class in either `bin/astar` or `bin`
  - `Backtracking.java` → class in either `bin/backtracking` or `bin`

Classpath selection:
- If `bin/astar` and `bin/backtracking` exist, those are used
- Otherwise it falls back to `bin`

Java executable path:
- Update `JAVA_BIN` near the top of `src/run_stats.py` if your `java.exe` path differs
  - Default: `C:/Program Files/Java/jdk1.8.0_202/bin/java.exe`

## Test File Format

Accepted formats:
- JSON array (single file with an array of objects)
- JSONL (one JSON object per line)

Each test object:
- `variant`: 1 or 2
- `map`:
  - `g`: `[x, y]` — Gollum’s position
  - `m`: `[x, y]` — Mount Doom position
  - `c`: `[x, y]` — Coat position
  - `enemies`: array of objects with fields:
    - `kind`: `"O" | "U" | "N" | "W"`
    - `x`: int
    - `y`: int

Example (JSON array):
```json
[
  {
    "variant": 1,
    "map": {
      "g": [3, 5],
      "m": [9, 11],
      "c": [4, 2],
      "enemies": [
        {"kind": "W", "x": 6, "y": 6},
        {"kind": "U", "x": 8, "y": 2},
        {"kind": "O", "x": 1, "y": 7},
        {"kind": "N", "x": 10, "y": 10}
      ]
    }
  }
]
```

Example (JSONL):
```
{"variant":2,"map":{"g":[1,1],"m":[10,10],"c":[3,3],"enemies":[{"kind":"W","x":6,"y":6}]}}
{"variant":1,"map":{"g":[2,4],"m":[8,12],"c":[5,5],"enemies":[{"kind":"O","x":4,"y":7},{"kind":"U","x":9,"y":2}]}}
```

## How to Run (PowerShell)

A* on a JSON suite:
```powershell
python C:\Users\valer\IdeaProjects\LordOfRing\src\run_stats.py --suite-in C:\path\to\tests.json --algo astar
```

Backtracking on a JSONL suite:
```powershell
python C:\Users\valer\IdeaProjects\LordOfRing\src\run_stats.py --suite-in C:\path\to\tests.jsonl --algo backtracking
```

Custom output directory:
```powershell
python C:\Users\valer\IdeaProjects\LordOfRing\src\run_stats.py --suite-in C:\path\to\tests.json --algo astar --outdir C:\Users\valer\IdeaProjects\LordOfRing\analysis\imported_astar
```

Flags:
- `--suite-in`: required, path to `.json` or `.jsonl` tests file
- `--algo`: required, `astar` or `backtracking`
- `--outdir`: optional, defaults to `analysis/imported`

## Output

- Summary file:
  - `analysis/imported/astar_import_summary.txt` or `backtracking_import_summary.txt`
- Metrics:
  - `total`, `wins`, `losses`
  - `runtime_mean`, `runtime_mode`, `runtime_median`, `runtime_std`

Notes:
- win = solver reported a non-negative length (not `-1`)
- loss = solver reported `-1` (unsolvable)

