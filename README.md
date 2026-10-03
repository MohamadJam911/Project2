# Linear Algebra Engine (LAE)

A multithreaded matrix calculator written in Java 21. It reads an expression tree of
matrix operations from a JSON file, evaluates it in parallel on a custom thread pool,
and writes the resulting matrix (or an error message) to a JSON file.

Built for Assignment 2 of the *Systems Programming Laboratory (SPL)* course.

## Features

- **Operations:** addition, multiplication, negation and transpose, nested to any depth.
- **Row-level parallelism:** each operation is split into one task per matrix row.
- **"Tired" thread pool:** every worker has a random fatigue factor, and each task goes
  to the least fatigued idle worker (see [Scheduling](#scheduling)).
- **Read-write locking** on every vector, so concurrent tasks never corrupt shared data.
- **Clear errors** for bad input: unknown operators, uneven rows, dimension mismatches.

## Requirements

- JDK 21 or newer
- Maven 3.9+

## Build and run

```bash
mvn compile
mvn exec:java -Dexec.mainClass=spl.lae.Main -Dexec.args="<numThreads> <inputFile> <outputFile>"
```

Example:

```bash
mvn exec:java -Dexec.mainClass=spl.lae.Main -Dexec.args="4 src/Examples/example1.json result.json"
```

A worker report (time used, idle time and fatigue per thread) is printed to stderr when
the computation finishes.

## Input format

An input file holds a single node. A node is either a **matrix** (a 2D JSON array of
numbers) or an **operation**:

```json
{ "operator": "<op>", "operands": [ <node>, <node>, ... ] }
```

| Operator | Meaning     | Operands                                       |
|----------|-------------|------------------------------------------------|
| `+`      | Addition    | 2 or more matrices with the same dimensions    |
| `*`      | Multiply    | 2 or more; each left operand's columns = right operand's rows |
| `-`      | Negation    | exactly 1                                      |
| `T`      | Transpose   | exactly 1                                      |

Operations with more than two operands are evaluated left to right:
`A + B + C` is computed as `(A + B) + C`.

### Example

Input ([src/Examples/example1.json](src/Examples/example1.json)), which computes
`A * (-B) + C + Dᵀ`:

```json
{
  "operator": "+",
  "operands": [
    { "operator": "*", "operands": [
        [[1, 2, 3], [4, 5, 6]],
        { "operator": "-", "operands": [ [[7, 8], [9, 10], [11, 12]] ] }
    ]},
    [[13, 14], [15, 16]],
    { "operator": "T", "operands": [ [[17, 18], [19, 20]] ] }
  ]
}
```

Output:

```json
{ "result": [ [-28.0, -31.0], [-106.0, -118.0] ] }
```

If the input is invalid, the output contains an error instead:

```json
{ "error": "Dimension mismatch for multiplication: 1x3 * 2x2" }
```

More inputs and their expected outputs are in [src/Examples/](src/Examples/)
(`exampleN.json` → `outN.json`).

## How it works

```
JSON file ──► InputParser ──► ComputationNode tree
                                     │
              LinearAlgebraEngine ◄──┘  repeatedly picks a node whose operands
                     │                  are all matrices, and computes it:
                     ▼
     load operands into SharedMatrix ──► one task per row ──► TiredExecutor
                     │
                     ▼
     replace the node with the result ──► ... until the root is a matrix ──► OutputWriter
```

| Package      | Class                  | Responsibility |
|--------------|------------------------|----------------|
| `parser`     | `InputParser`          | Parses the JSON input into a tree of `ComputationNode`s. |
|              | `ComputationNode`      | A tree node: an operator with children, or a matrix leaf. |
|              | `OutputWriter`         | Writes `{"result": ...}` or `{"error": ...}`. |
| `memory`     | `SharedVector`         | A row or column of doubles guarded by a `ReentrantReadWriteLock`. |
|              | `SharedMatrix`         | A matrix stored as `SharedVector`s, row-major or column-major. |
| `scheduling` | `TiredThread`          | A worker thread that tracks its busy time and fatigue. |
|              | `TiredExecutor`        | The thread pool; assigns tasks to the least fatigued idle worker. |
| `spl.lae`    | `LinearAlgebraEngine`  | Walks the tree, creates the row tasks, and collects results. |
|              | `Main`                 | Command-line entry point. |
|              | `MergeExamples`        | Helper that merges the example inputs/outputs into combined JSON files. |

### Computing each operation

- **Add:** both matrices are loaded row-major; task *i* adds right row *i* into left row *i*.
- **Multiply:** the right matrix is loaded **column-major**, so task *i* computes left row *i*
  times the right matrix as a series of dot products (row · column).
- **Negate:** task *i* negates row *i* in place.
- **Transpose:** task *i* marks row *i* as a column; reading the matrix back row-major then
  produces the transpose without copying data inside the tasks.

### Scheduling

Each `TiredThread` gets a random fatigue factor in `[0.5, 1.5)`, and its
**fatigue = fatigue factor × total time spent running tasks**. Idle workers wait in a
priority queue ordered by fatigue, so each new task goes to the least tired one.
A worker returns to the queue only after its statistics are updated, so the queue
always orders by current fatigue. Each worker receives tasks through a one-slot
hand-off queue and exits when it receives a "poison pill" on shutdown.

## Tests

```bash
mvn test
```

The JUnit 5 suite in [src/test/java/spl/lae/](src/test/java/spl/lae/) covers every
operation, nested and n-ary expressions, invalid input, dimension mismatches, and checks
that results are the same for 1 to 64 threads. Result files from the tests are
written to `script_output/`.

## Authors

- Mohamad Jammal
- Mohamad Mahajne
