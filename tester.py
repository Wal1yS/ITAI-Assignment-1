from __future__ import annotations

import json
import random
import statistics
import subprocess
import threading
import time
from collections import deque
from dataclasses import dataclass, field
from pathlib import Path
from queue import Empty, Queue
from typing import Dict, Iterable, List, Optional, Sequence, Tuple
from typing import Any

# Board constants
SIZE = 13
DIRS = [(0, 1), (1, 0), (0, -1), (-1, 0)]
SENTINEL = object()

# Paths to compiled Java solutions
ROOT = Path(__file__).resolve().parents[1]
JAVA_BIN = Path("C:/Program Files/Java/jdk1.8.0_202/bin/java.exe")

# Prefer subdirectories if present (bin/astar, bin/backtracking),
# otherwise fall back to plain bin where classes may be compiled.
_BIN = ROOT / "bin"
_BIN_ASTAR = _BIN / "astar"
_BIN_BACK = _BIN / "backtracking"
if _BIN_ASTAR.exists() and _BIN_BACK.exists():
    ASTAR_CLASSPATH = str(_BIN_ASTAR)
    BACKTRACK_CLASSPATH = str(_BIN_BACK)
else:
    ASTAR_CLASSPATH = str(_BIN)
    BACKTRACK_CLASSPATH = str(_BIN)

# Simulation parameters
# Timeouts tuned to avoid false no_output/timeout on Windows
# while still catching true hangs.
COMMAND_TIMEOUT_SEC = 12.0  # max silence between commands
PROCESS_TIMEOUT_SEC = 120   # max total run time per simulation


@dataclass(frozen=True)
class Enemy:
    kind: str
    x: int
    y: int

    @property
    def pos(self) -> Tuple[int, int]:
        return (self.x, self.y)


@dataclass(frozen=True)
class MapDefinition:
    g_pos: Tuple[int, int]
    m_pos: Tuple[int, int]
    c_pos: Tuple[int, int]
    enemies: Tuple[Enemy, ...]

    def enemy_positions(self) -> set[Tuple[int, int]]:
        return {enemy.pos for enemy in self.enemies}


@dataclass
class RunResult:
    success: bool
    reason: str
    moves: int
    toggles: int
    reported_length: Optional[int]
    runtime_sec: float
    claimed_unsolvable: bool
    was_solvable: bool
    stderr: str = ""
    log: List[str] = field(default_factory=list)


def inside(x: int, y: int) -> bool:
    return 0 <= x < SIZE and 0 <= y < SIZE


def moore(radius: int) -> List[Tuple[int, int]]:
    return [
        (dx, dy)
        for dx in range(-radius, radius + 1)
        for dy in range(-radius, radius + 1)
        if max(abs(dx), abs(dy)) <= radius
    ]


def von_neumann(radius: int) -> List[Tuple[int, int]]:
    return [
        (dx, dy)
        for dx in range(-radius, radius + 1)
        for dy in range(-radius, radius + 1)
        if abs(dx) + abs(dy) <= radius
    ]


def translate(pos: Tuple[int, int], offsets: Iterable[Tuple[int, int]]) -> List[Tuple[int, int]]:
    x, y = pos
    return [(x + dx, y + dy) for dx, dy in offsets if inside(x + dx, y + dy)]


def nazgul_zone(pos: Tuple[int, int], ring_on: bool, has_coat: bool) -> set[Tuple[int, int]]:
    if ring_on:
        radius, with_ears = 2, True
    elif has_coat:
        radius, with_ears = 1, False
    else:
        radius, with_ears = 1, True

    zone = set(translate(pos, moore(radius)))
    if with_ears:
        ext = radius + 1
        for dx in (-ext, ext):
            for dy in (-ext, ext):
                nx, ny = pos[0] + dx, pos[1] + dy
                if inside(nx, ny):
                    zone.add((nx, ny))
    return zone


def watchtower_zone(pos: Tuple[int, int], ring_on: bool) -> set[Tuple[int, int]]:
    radius = 2
    zone = set(translate(pos, moore(radius)))
    if ring_on:
        ext = radius + 1
        for dx in (-ext, ext):
            for dy in (-ext, ext):
                nx, ny = pos[0] + dx, pos[1] + dy
                if inside(nx, ny):
                    zone.add((nx, ny))
    return zone


def enemy_zone(enemy: Enemy, ring_on: bool, has_coat: bool) -> set[Tuple[int, int]]:
    pos = enemy.pos
    if enemy.kind == "O":
        radius = 1
        if ring_on or has_coat:
            radius = max(0, radius - 1)
        offsets = von_neumann(radius)
        return set(translate(pos, offsets))
    if enemy.kind == "U":
        radius = 2
        if ring_on or has_coat:
            radius = max(0, radius - 1)
        offsets = von_neumann(radius)
        return set(translate(pos, offsets))
    if enemy.kind == "N":
        return nazgul_zone(pos, ring_on, has_coat)
    if enemy.kind == "W":
        return watchtower_zone(pos, ring_on)
    raise ValueError(f"Unknown enemy type {enemy.kind}")


def compute_hazard_cache(map_def: MapDefinition) -> Dict[Tuple[bool, bool], set[Tuple[int, int]]]:
    cache: Dict[Tuple[bool, bool], set[Tuple[int, int]]] = {}
    for ring_on in (False, True):
        for has_coat in (False, True):
            zone: set[Tuple[int, int]] = set()
            for enemy in map_def.enemies:
                zone.update(enemy_zone(enemy, ring_on, has_coat))
            cache[(ring_on, has_coat)] = zone
    return cache


## Random map generation and bulk experiments removed — import-only runner remains.


def stage_transition(stage: int, position: Tuple[int, int], g_pos: Tuple[int, int]) -> int:
    if stage == 0 and position == g_pos:
        return 1
    return stage


def compute_shortest_paths(map_def: MapDefinition) -> Dict[str, Optional[int]]:
    hazards = compute_hazard_cache(map_def)
    enemy_cells = map_def.enemy_positions()
    c_pos = map_def.c_pos
    g_pos = map_def.g_pos
    m_pos = map_def.m_pos

    start_state = (0, 0, 0, 0, 0)  # x, y, ring(0/1), coat(0/1), stage(0=start,1=after G)
    dist: Dict[Tuple[int, int, int, int, int], int] = {start_state: 0}
    dq: deque[Tuple[int, int, int, int, int]] = deque([start_state])

    while dq:
        state = dq.popleft()
        x, y, ring, coat, stage = state
        moves = dist[state]

        current_hazard = hazards[(bool(ring), bool(coat))]

        # Try toggling ring (cost 0)
        new_ring = 1 - ring
        new_hazard = hazards[(bool(new_ring), bool(coat))]
        if (x, y) not in new_hazard and (x, y) not in enemy_cells:
            nxt = (x, y, new_ring, coat, stage)
            if nxt not in dist or moves < dist[nxt]:
                dist[nxt] = moves
                dq.appendleft(nxt)

        # Try moving (cost 1)
        for dx, dy in DIRS:
            nx, ny = x + dx, y + dy
            if not inside(nx, ny):
                continue
            if (nx, ny) in enemy_cells:
                continue
            if (nx, ny) in current_hazard:
                continue
            next_coat = 1 if (coat or (nx, ny) == c_pos) else 0
            next_stage = stage_transition(stage, (nx, ny), g_pos)
            nxt = (nx, ny, ring, next_coat, next_stage)
            if nxt not in dist or moves + 1 < dist[nxt]:
                dist[nxt] = moves + 1
                dq.append(nxt)

    g_candidates = [
        dist[state]
        for state in dist
        if state[0] == g_pos[0] and state[1] == g_pos[1] and state[4] == 1
    ]
    m_candidates = [
        dist[state]
        for state in dist
        if state[0] == m_pos[0] and state[1] == m_pos[1] and state[4] == 1
    ]

    return {
        "dist_to_g": min(g_candidates) if g_candidates else None,
        "dist_to_m": min(m_candidates) if m_candidates else None,
    }


def get_visible_entries(
    map_def: MapDefinition,
    hazards: Dict[Tuple[bool, bool], set[Tuple[int, int]]],
    position: Tuple[int, int],
    variant: int,
    ring_on: bool,
    has_coat: bool,
    g_active: Optional[Tuple[int, int]],
    c_active: Optional[Tuple[int, int]],
    m_active: Optional[Tuple[int, int]],
) -> List[Tuple[int, int, str]]:
    radius = 1 if variant == 1 else 2
    hazard_set = hazards[(ring_on, has_coat)]
    entries: List[Tuple[int, int, str]] = []
    px, py = position
    items = {}
    if g_active is not None:
        items[g_active] = "G"
    if c_active is not None:
        items[c_active] = "C"
    if m_active is not None:
        items[m_active] = "M"

    enemy_at = {enemy.pos: enemy.kind for enemy in map_def.enemies}

    for dx in range(-radius, radius + 1):
        for dy in range(-radius, radius + 1):
            if max(abs(dx), abs(dy)) > radius:
                continue
            nx, ny = px + dx, py + dy
            if not inside(nx, ny):
                continue
            if (nx, ny) == (px, py):
                continue
            if (nx, ny) in enemy_at:
                entries.append((nx, ny, enemy_at[(nx, ny)]))
            elif (nx, ny) in items:
                entries.append((nx, ny, items[(nx, ny)]))
            elif (nx, ny) in hazard_set:
                entries.append((nx, ny, "P"))
    entries.sort()
    return entries


def spawn_algorithm(algo: str) -> subprocess.Popen:
    if algo == "astar":
        classpath = ASTAR_CLASSPATH
        main_class = "Astar"
    elif algo == "backtracking":
        classpath = BACKTRACK_CLASSPATH
        main_class = "Backtracking"
    else:
        raise ValueError(f"Unknown algorithm {algo}")
    command = [JAVA_BIN, "-cp", classpath, main_class]
    return subprocess.Popen(
        command,
        cwd=ROOT,
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        bufsize=1,
    )


def reader_thread(stream, queue: Queue):
    try:
        for line in iter(stream.readline, ""):
            if not line:
                break
            queue.put(line.rstrip("\n"))
    finally:
        queue.put(SENTINEL)


def send_line(proc: subprocess.Popen, data: str) -> None:
    assert proc.stdin is not None
    proc.stdin.write(data + "\n")
    proc.stdin.flush()


def receive_line(queue: Queue, timeout: float) -> Tuple[Optional[str], bool]:
    try:
        item = queue.get(timeout=timeout)
    except Empty:
        return None, False
    if item is SENTINEL:
        return None, True
    return item, False


def cleanup_process(proc: subprocess.Popen, stdout_thread: threading.Thread, stderr_thread: threading.Thread) -> None:
    """Ensure the spawned process is fully terminated and OS handles are reaped.

    - Close stdin to signal no more input
    - Wait briefly for graceful exit; otherwise kill and wait
    - Join reader threads and close pipes to avoid handle leaks on Windows
    """
    # Close stdin to avoid the child blocking on input
    try:
        if proc.stdin and not proc.stdin.closed:
            proc.stdin.close()
    except Exception:
        pass

    # Give the process a moment to exit on its own
    try:
        proc.wait(timeout=0.5)
    except Exception:
        # Force kill if still running, then wait for termination
        try:
            if proc.poll() is None:
                proc.kill()
        except Exception:
            pass
        try:
            proc.wait(timeout=2)
        except Exception:
            pass

    # Join reader threads (they will exit when streams close)
    try:
        stdout_thread.join(timeout=0.5)
    except Exception:
        pass
    try:
        stderr_thread.join(timeout=0.5)
    except Exception:
        pass

    # Close pipes explicitly to release OS handles
    for stream in (getattr(proc, "stdout", None), getattr(proc, "stderr", None)):
        try:
            if stream and not stream.closed:
                stream.close()
        except Exception:
            pass


def run_single_simulation(
    map_def: MapDefinition,
    variant: int,
    algo: str,
    rng: random.Random,
    hazards: Dict[Tuple[bool, bool], set[Tuple[int, int]]],
    map_stats: Dict[str, Optional[int]],
) -> RunResult:
    proc = spawn_algorithm(algo)
    stdout_queue: Queue = Queue()
    stderr_queue: Queue = Queue()
    enemy_cells = map_def.enemy_positions()

    stdout_thread = threading.Thread(target=reader_thread, args=(proc.stdout, stdout_queue))
    stderr_thread = threading.Thread(target=reader_thread, args=(proc.stderr, stderr_queue))
    stdout_thread.daemon = True
    stderr_thread.daemon = True
    stdout_thread.start()
    stderr_thread.start()

    start_time = time.perf_counter()

    ring_on = False
    has_coat = False
    position = (0, 0)
    g_active: Optional[Tuple[int, int]] = map_def.g_pos
    c_active: Optional[Tuple[int, int]] = map_def.c_pos
    m_active: Optional[Tuple[int, int]] = None
    g_found = False
    moves = 0
    toggles = 0
    claimed_unsolvable = False
    log: List[str] = []

    send_line(proc, str(variant))
    send_line(proc, f"{map_def.g_pos[0]} {map_def.g_pos[1]}")
    initial_entries = get_visible_entries(
        map_def,
        hazards,
        position,
        variant,
        ring_on,
        has_coat,
        g_active,
        c_active,
        m_active,
    )
    send_line(proc, str(len(initial_entries)))
    for x, y, kind in initial_entries:
        send_line(proc, f"{x} {y} {kind}")

    stdout_timeout = COMMAND_TIMEOUT_SEC

    def consume_stderr() -> str:
        collected: List[str] = []
        while True:
            try:
                line = stderr_queue.get_nowait()
            except Empty:
                break
            if line is SENTINEL:
                continue
            collected.append(line)
        return "\n".join(collected)

    try:
        while True:
            elapsed = time.perf_counter() - start_time
            if elapsed > PROCESS_TIMEOUT_SEC:
                proc.kill()
                return RunResult(
                    success=False,
                    reason="timeout",
                    moves=moves,
                    toggles=toggles,
                    reported_length=None,
                    runtime_sec=elapsed,
                    claimed_unsolvable=False,
                    was_solvable=map_stats["dist_to_m"] is not None,
                    stderr=consume_stderr(),
                    log=log,
                )

            line, eof = receive_line(stdout_queue, stdout_timeout)
            if eof:
                if proc.poll() is not None:
                    return RunResult(
                        success=False,
                        reason="unexpected_termination",
                        moves=moves,
                        toggles=toggles,
                        reported_length=None,
                        runtime_sec=time.perf_counter() - start_time,
                        claimed_unsolvable=False,
                        was_solvable=map_stats["dist_to_m"] is not None,
                        stderr=consume_stderr(),
                        log=log,
                    )
                continue
            if line is None:
                proc.kill()
                return RunResult(
                    success=False,
                    reason="no_output",
                    moves=moves,
                    toggles=toggles,
                    reported_length=None,
                    runtime_sec=time.perf_counter() - start_time,
                    claimed_unsolvable=False,
                    was_solvable=map_stats["dist_to_m"] is not None,
                    stderr=consume_stderr(),
                    log=log,
                )
            log.append(line)

            tokens = line.strip().split()
            if not tokens:
                continue
            cmd = tokens[0]

            if cmd == "m":
                if len(tokens) != 3:
                    proc.kill()
                    return RunResult(
                        success=False,
                        reason="invalid_move_format",
                        moves=moves,
                        toggles=toggles,
                        reported_length=None,
                        runtime_sec=time.perf_counter() - start_time,
                        claimed_unsolvable=False,
                        was_solvable=map_stats["dist_to_m"] is not None,
                        stderr=consume_stderr(),
                        log=log,
                    )
                nx, ny = int(tokens[1]), int(tokens[2])
                if not inside(nx, ny):
                    proc.kill()
                    return RunResult(
                        success=False,
                        reason="move_out_of_bounds",
                        moves=moves,
                        toggles=toggles,
                        reported_length=None,
                        runtime_sec=time.perf_counter() - start_time,
                        claimed_unsolvable=False,
                        was_solvable=map_stats["dist_to_m"] is not None,
                        stderr=consume_stderr(),
                        log=log,
                    )
                if abs(position[0] - nx) + abs(position[1] - ny) != 1:
                    proc.kill()
                    return RunResult(
                        success=False,
                        reason="non_adjacent_move",
                        moves=moves,
                        toggles=toggles,
                        reported_length=None,
                        runtime_sec=time.perf_counter() - start_time,
                        claimed_unsolvable=False,
                        was_solvable=map_stats["dist_to_m"] is not None,
                        stderr=consume_stderr(),
                        log=log,
                    )
                hazard = hazards[(ring_on, has_coat)]
                if (nx, ny) in hazard or (nx, ny) in enemy_cells:
                    proc.kill()
                    return RunResult(
                        success=False,
                        reason="stepped_into_hazard",
                        moves=moves,
                        toggles=toggles,
                        reported_length=None,
                        runtime_sec=time.perf_counter() - start_time,
                        claimed_unsolvable=False,
                        was_solvable=map_stats["dist_to_m"] is not None,
                        stderr=consume_stderr(),
                        log=log,
                    )
                position = (nx, ny)
                moves += 1
                just_met_g = False
                if c_active is not None and position == c_active:
                    has_coat = True
                    c_active = None
                if g_active is not None and position == g_active:
                    g_found = True
                    g_active = None
                    just_met_g = True
                    m_active = map_def.m_pos
                entries = get_visible_entries(
                    map_def,
                    hazards,
                    position,
                    variant,
                    ring_on,
                    has_coat,
                    g_active,
                    c_active,
                    m_active,
                )
                # Send surroundings first, then Gollum's message if applicable
                send_line(proc, str(len(entries)))
                for x, y, kind in entries:
                    send_line(proc, f"{x} {y} {kind}")
                if just_met_g:
                    send_line(proc, f"{map_def.m_pos[0]} {map_def.m_pos[1]}")

            elif cmd == "r":
                if ring_on:
                    proc.kill()
                    return RunResult(
                        success=False,
                        reason="ring_already_on",
                        moves=moves,
                        toggles=toggles,
                        reported_length=None,
                        runtime_sec=time.perf_counter() - start_time,
                        claimed_unsolvable=False,
                        was_solvable=map_stats["dist_to_m"] is not None,
                        stderr=consume_stderr(),
                        log=log,
                    )
                new_hazard = hazards[(True, has_coat)]
                if position in new_hazard:
                    proc.kill()
                    return RunResult(
                        success=False,
                        reason="toggle_into_hazard",
                        moves=moves,
                        toggles=toggles,
                        reported_length=None,
                        runtime_sec=time.perf_counter() - start_time,
                        claimed_unsolvable=False,
                        was_solvable=map_stats["dist_to_m"] is not None,
                        stderr=consume_stderr(),
                        log=log,
                    )
                ring_on = True
                toggles += 1
                entries = get_visible_entries(
                    map_def,
                    hazards,
                    position,
                    variant,
                    ring_on,
                    has_coat,
                    g_active,
                    c_active,
                    m_active,
                )
                send_line(proc, str(len(entries)))
                for x, y, kind in entries:
                    send_line(proc, f"{x} {y} {kind}")

            elif cmd == "rr":
                if not ring_on:
                    proc.kill()
                    return RunResult(
                        success=False,
                        reason="ring_already_off",
                        moves=moves,
                        toggles=toggles,
                        reported_length=None,
                        runtime_sec=time.perf_counter() - start_time,
                        claimed_unsolvable=False,
                        was_solvable=map_stats["dist_to_m"] is not None,
                        stderr=consume_stderr(),
                        log=log,
                    )
                new_hazard = hazards[(False, has_coat)]
                if position in new_hazard:
                    proc.kill()
                    return RunResult(
                        success=False,
                        reason="toggle_into_hazard",
                        moves=moves,
                        toggles=toggles,
                        reported_length=None,
                        runtime_sec=time.perf_counter() - start_time,
                        claimed_unsolvable=False,
                        was_solvable=map_stats["dist_to_m"] is not None,
                        stderr=consume_stderr(),
                        log=log,
                    )
                ring_on = False
                toggles += 1
                entries = get_visible_entries(
                    map_def,
                    hazards,
                    position,
                    variant,
                    ring_on,
                    has_coat,
                    g_active,
                    c_active,
                    m_active,
                )
                send_line(proc, str(len(entries)))
                for x, y, kind in entries:
                    send_line(proc, f"{x} {y} {kind}")

            elif cmd == "e":
                if len(tokens) != 2:
                    proc.kill()
                    return RunResult(
                        success=False,
                        reason="invalid_end_format",
                        moves=moves,
                        toggles=toggles,
                        reported_length=None,
                        runtime_sec=time.perf_counter() - start_time,
                        claimed_unsolvable=False,
                        was_solvable=map_stats["dist_to_m"] is not None,
                        stderr=consume_stderr(),
                        log=log,
                    )
                value = int(tokens[1])
                claimed_unsolvable = value == -1
                was_solvable = map_stats["dist_to_m"] is not None
                ended_on_goal = position == map_def.m_pos and g_found
                # Expected optimal length is sum of shortest move counts to G and from G to M
                if was_solvable and map_stats["dist_to_m"] is not None:
                    # dist_to_m already includes the segment to G (stage==1 constraint)
                    expected_len = map_stats["dist_to_m"]
                else:
                    expected_len = None

                if claimed_unsolvable:
                    success = not was_solvable
                    reason = "ok" if success else "false_unsolvable"
                else:
                    if not ended_on_goal:
                        success = False
                        reason = "ended_without_goal"
                    else:
                        if expected_len is None:
                            # No optimal reference; accept any non-negative report
                            success = value >= 0
                            reason = "ok" if success else "invalid_result"
                        else:
                            success = (value == expected_len)
                            reason = "ok" if success else "wrong_length"
                return RunResult(
                    success=success,
                    reason=reason,
                    moves=moves,
                    toggles=toggles,
                    reported_length=value if value >= 0 else None,
                    runtime_sec=time.perf_counter() - start_time,
                    claimed_unsolvable=claimed_unsolvable,
                    was_solvable=was_solvable,
                    stderr=consume_stderr(),
                    log=log,
                )
            else:
                proc.kill()
                return RunResult(
                    success=False,
                    reason="unknown_command",
                    moves=moves,
                    toggles=toggles,
                    reported_length=None,
                    runtime_sec=time.perf_counter() - start_time,
                    claimed_unsolvable=False,
                    was_solvable=map_stats["dist_to_m"] is not None,
                    stderr=consume_stderr(),
                    log=log,
                )
    finally:
        cleanup_process(proc, stdout_thread, stderr_thread)
        consume_stderr()


# ===================== Новые функции для дампа проблемных карт =====================

def render_ascii_map(
    map_def: MapDefinition,
    hazards: Optional[set[Tuple[int, int]]] = None,
) -> str:
    """ASCII-карта: '.' — пусто, '*' — опасность (если hazards передан),
    S — старт, G/C/M — объекты, O/U/N/W — враги."""
    grid = [['.' for _ in range(SIZE)] for _ in range(SIZE)]
    grid[0][0] = 'S'

    gx, gy = map_def.g_pos
    cx, cy = map_def.c_pos
    mx, my = map_def.m_pos
    grid[gx][gy] = 'G'
    grid[cx][cy] = 'C'
    grid[mx][my] = 'M'

    for e in map_def.enemies:
        grid[e.x][e.y] = e.kind  # O, U, N, W

    if hazards:
        for (x, y) in hazards:
            if grid[x][y] == '.':
                grid[x][y] = '*'

    header = "    " + " ".join(f"{y:2d}" for y in range(SIZE))
    lines = [header]
    for x in range(SIZE):
        row = " ".join(f"{cell:2s}" for cell in grid[x])
        lines.append(f"{x:2d}  {row}")
    return "\n".join(lines)


def serialize_map(map_def: MapDefinition) -> Dict[str, object]:
    return {
        "g": map_def.g_pos,
        "m": map_def.m_pos,
        "c": map_def.c_pos,
        "enemies": [{"kind": e.kind, "x": e.x, "y": e.y} for e in map_def.enemies],
    }


# ===================== Импорт и запуск тестов из JSON =====================

@dataclass
class TestCase:
    variant: int
    map: Dict[str, Any]

    @staticmethod
    def from_obj(obj: Dict[str, Any]) -> "TestCase":
        return TestCase(variant=int(obj["variant"]), map=obj["map"])  # type: ignore[arg-type]


def map_from_serialized(data: Dict[str, Any]) -> MapDefinition:
    g = tuple(data["g"])  # type: ignore
    m = tuple(data["m"])  # type: ignore
    c = tuple(data["c"])  # type: ignore
    enemies = [Enemy(kind=e["kind"], x=int(e["x"]), y=int(e["y"])) for e in data["enemies"]]  # type: ignore
    return MapDefinition(g_pos=g, m_pos=m, c_pos=c, enemies=tuple(enemies))


def read_tests_file(path: Path) -> List[TestCase]:
    """Read tests from a JSON file.
    Supports two formats:
    1) JSON array: [{"variant": 1, "map": {...}}, ...]
    2) JSONL: one object per line with same keys
    """
    text = path.read_text(encoding="utf-8").strip()
    tests: List[TestCase] = []
    if not text:
        return tests
    if text.lstrip().startswith("["):
        arr = json.loads(text)
        for obj in arr:
            tests.append(TestCase.from_obj(obj))
    else:
        for line in text.splitlines():
            line = line.strip()
            if not line:
                continue
            obj = json.loads(line)
            tests.append(TestCase.from_obj(obj))
    return tests


def run_case(case: TestCase, algo: str) -> RunResult:
    map_def = map_from_serialized(case.map)
    hazards = compute_hazard_cache(map_def)
    map_stats = compute_shortest_paths(map_def)
    return run_single_simulation(
        map_def=map_def,
        variant=case.variant,
        algo=algo,
        rng=random.Random(0),
        hazards=hazards,
        map_stats=map_stats,
    )


def run_imported_tests(algo: str, in_path: Path, out_dir: Path) -> Dict[str, object]:
    tests = read_tests_file(in_path)
    out_dir.mkdir(parents=True, exist_ok=True)
    results: List[RunResult] = []
    for idx, case in enumerate(tests):
        res = run_case(case, algo)
        results.append(res)

    runtimes = [r.runtime_sec for r in results]
    wins = sum(1 for r in results if r.reported_length is not None)
    losses = len(results) - wins

    def safe_mode(values: Sequence[float | int]) -> Optional[float]:
        try:
            return statistics.mode(values) if values else None
        except statistics.StatisticsError:
            return None

    summary = {
        "algo": algo,
        "total": len(results),
        "wins": wins,
        "losses": losses,
        "runtime_mean": statistics.fmean(runtimes) if runtimes else None,
        "runtime_mode": safe_mode(runtimes),
        "runtime_median": statistics.median(runtimes) if runtimes else None,
        "runtime_std": statistics.pstdev(runtimes) if len(runtimes) > 1 else 0.0,
    }

    with (out_dir / f"{algo}_import_summary.txt").open("w", encoding="utf-8") as f:
        for k, v in summary.items():
            f.write(f"{k}: {v}\n")

    return summary


def dump_failure_case(
    out_dir: Path,
    algo_key: str,          # например "astar_v1" или "backtracking_v2"
    map_idx: int,           # порядковый номер карты в эксперименте
    seed: int,              # общий seed запуска
    map_def: MapDefinition,
    map_stats: Dict[str, Optional[int]],
    result: RunResult,
) -> None:
    out_dir = out_dir / "failures"
    out_dir.mkdir(parents=True, exist_ok=True)

    hazards = compute_hazard_cache(map_def)
    base_h = hazards[(False, False)]
    ring_h = hazards[(True, False)]
    coat_h = hazards[(False, True)]

    expected_len = map_stats.get("dist_to_m")
    fname = f"{algo_key}_map{map_idx:04d}_{result.reason}.txt"
    fp = out_dir / fname

    with fp.open("w", encoding="utf-8") as f:
        f.write(f"seed: {seed} | map_index: {map_idx}\n")
        f.write(f"algo: {algo_key}\n")
        f.write(f"success: {result.success} | reason: {result.reason}\n")
        f.write(f"claimed_unsolvable: {result.claimed_unsolvable} | was_solvable: {result.was_solvable}\n")
        f.write(f"moves: {result.moves} | toggles: {result.toggles}\n")
        f.write(f"reported_length: {result.reported_length} | expected_shortest (G→M, incl. to G): {expected_len}\n")

        f.write("\n-- ASCII map (base hazards='*', ring_off, no_coat) --\n")
        f.write(render_ascii_map(map_def, base_h))
        f.write("\n\n-- With ring ON (hazards='+') --\n")
        f.write(render_ascii_map(map_def, ring_h).replace('*', '+'))
        f.write("\n\n-- With COAT (hazards='#') --\n")
        f.write(render_ascii_map(map_def, coat_h).replace('*', '#'))

        f.write("\n\n-- JSON (для воспроизведения) --\n")
        json.dump(serialize_map(map_def), f, ensure_ascii=False)
        f.write("\n")

        if result.log:
            f.write("\n-- Program log (stdout) --\n")
            f.write("\n".join(result.log))
            f.write("\n")

        if result.stderr:
            f.write("\n-- Program stderr --\n")
            f.write(result.stderr)
            f.write("\n")


# (Test suite generation and unsolvable-case generation intentionally removed; interactor only.)


# ===================== Агрегация и запуск эксперимента =====================

def aggregate_statistics(results: Sequence[RunResult]) -> Dict[str, object]:
    runtimes_all = [res.runtime_sec for res in results]
    runtimes_success = [res.runtime_sec for res in results if res.success]
    moves_success = [res.moves for res in results if res.success]
    toggles_success = [res.toggles for res in results if res.success]
    success_count = sum(1 for res in results if res.success)
    failure_count = len(results) - success_count

    def safe_mode(values: Sequence[float | int]) -> Optional[float]:
        if not values:
            return None
        try:
            return float(statistics.mode(values))
        except statistics.StatisticsError:
            return None

    reason_counts: Dict[str, int] = {}
    for res in results:
        reason_counts[res.reason] = reason_counts.get(res.reason, 0) + 1

    claimed_unsolvable_total = sum(1 for res in results if res.claimed_unsolvable)
    correct_unsolvable = sum(
        1 for res in results if res.claimed_unsolvable and not res.was_solvable and res.success
    )
    false_unsolvable = sum(1 for res in results if res.reason == "false_unsolvable")

    summary = {
        "total_runs": len(results),
        "successes": success_count,
        "failures": failure_count,
        "success_rate": success_count / len(results) if results else 0.0,
        "failure_rate": failure_count / len(results) if results else 0.0,
        "runtime_mean_all": statistics.fmean(runtimes_all) if runtimes_all else None,
        "runtime_median_all": statistics.median(runtimes_all) if runtimes_all else None,
        "runtime_mode_all": safe_mode(runtimes_all),
        "runtime_std_all": statistics.pstdev(runtimes_all) if len(runtimes_all) > 1 else 0.0,
        "runtime_mean_success": statistics.fmean(runtimes_success) if runtimes_success else None,
        "runtime_median_success": statistics.median(runtimes_success) if runtimes_success else None,
        "runtime_mode_success": safe_mode(runtimes_success),
        "runtime_std_success": statistics.pstdev(runtimes_success) if len(runtimes_success) > 1 else 0.0,
        "moves_mean_success": statistics.fmean(moves_success) if moves_success else None,
        "moves_median_success": statistics.median(moves_success) if moves_success else None,
        "moves_mode_success": safe_mode(moves_success),
        "moves_std_success": statistics.pstdev(moves_success) if len(moves_success) > 1 else 0.0,
        "toggles_mean_success": statistics.fmean(toggles_success) if toggles_success else None,
        "toggles_median_success": statistics.median(toggles_success) if toggles_success else None,
        "toggles_mode_success": safe_mode(toggles_success),
        "toggles_std_success": statistics.pstdev(toggles_success) if len(toggles_success) > 1 else 0.0,
        "claimed_unsolvable_total": claimed_unsolvable_total,
        "claimed_unsolvable_correct": correct_unsolvable,
        "claimed_unsolvable_false": false_unsolvable,
        "reason_breakdown": reason_counts,
    }
    return summary


## Bulk experiment runner removed.


def main() -> None:
    import argparse

    parser = argparse.ArgumentParser(description="Frodo pathfinding interactor (import-only)")
    # Import-and-run mode (no generation). Provide a JSON/JSONL file with tests.
    parser.add_argument("--suite-in", dest="suite_in", type=str, required=True, help="Path to tests file (.json or .jsonl)")
    parser.add_argument(
        "--algo",
        choices=["astar", "backtracking"],
        required=True,
        help="Which algorithm to run for imported tests",
    )
    parser.add_argument(
        "--outdir",
        type=str,
        default=str((ROOT / "analysis" / "imported").resolve()),
        help="Where to write summaries/logs for imported tests",
    )

    args = parser.parse_args()

    in_path = Path(args.suite_in)
    out_dir = Path(args.outdir)
    summary = run_imported_tests(args.algo, in_path, out_dir)
    print("Imported tests summary:")
    for k, v in summary.items():
        print(f"  {k}: {v}")
    print(f"Summary file: {(out_dir / (args.algo + '_import_summary.txt')).resolve()}")


if __name__ == "__main__":
    main()
