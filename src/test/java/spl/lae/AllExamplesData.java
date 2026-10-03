package spl.lae;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class AllExamplesData {
    // ----- Original Inputs -----
    public static final String COMPLEX_NESTED = """
{
  "operator": "+",
  "operands": [
    {
      "operator": "*",
      "operands": [
        {
          "operator": "+",
          "operands": [
            [ [1,2], [3,4] ],
            [ [5,6], [7,8] ]
          ]
        },
        {
          "operator": "+",
          "operands": [
            [ [1,0], [0,1] ],
            { "operator": "-", "operands": [ [ [0,1], [1,0] ] ] }
          ]
        }
      ]
    },
    {
      "operator": "T",
      "operands": [
        [ [2,3], [4,5] ]
      ]
    }
  ]
}
""";

    public static final String CPOILOT_TEST = """
{
  "operator": "*",
  "operands": [
    [ [1, 2], [3, 4] ],
    [ [5, 6], [7, 8] ]
  ]
}
""";

    public static final String DIM_MISMATCH = """
{
  "operator": "*",
  "operands": [
    [ [1,2,3], [4,5,6] ],
    [ [7,8], [9,10] ]
  ]
}
""";

    // ----- NEW: Your 10x10 Example Input -----
    public static final String EXAMPLE_10X10_INPUT = """
{
  "operator": "*",
  "operands": [
    {
      "operator": "+",
      "operands": [
        [
          [1, 2, 3, 4, 5, 6, 7, 8, 9, 10],
          [11, 12, 13, 14, 15, 16, 17, 18, 19, 20],
          [21, 22, 23, 24, 25, 26, 27, 28, 29, 30],
          [31, 32, 33, 34, 35, 36, 37, 38, 39, 40],
          [41, 42, 43, 44, 45, 46, 47, 48, 49, 50],
          [51, 52, 53, 54, 55, 56, 57, 58, 59, 60],
          [61, 62, 63, 64, 65, 66, 67, 68, 69, 70],
          [71, 72, 73, 74, 75, 76, 77, 78, 79, 80],
          [81, 82, 83, 84, 85, 86, 87, 88, 89, 90],
          [91, 92, 93, 94, 95, 96, 97, 98, 99, 100]
        ],
        {
          "operator": "T",
          "operands": [
            [
              [10, 9, 8, 7, 6, 5, 4, 3, 2, 1],
              [20, 19, 18, 17, 16, 15, 14, 13, 12, 11],
              [30, 29, 28, 27, 26, 25, 24, 23, 22, 21],
              [40, 39, 38, 37, 36, 35, 34, 33, 32, 31],
              [50, 49, 48, 47, 46, 45, 44, 43, 42, 41],
              [60, 59, 58, 57, 56, 55, 54, 53, 52, 51],
              [70, 69, 68, 67, 66, 65, 64, 63, 62, 61],
              [80, 79, 78, 77, 76, 75, 74, 73, 72, 71],
              [90, 89, 88, 87, 86, 85, 84, 83, 82, 81],
              [100, 99, 98, 97, 96, 95, 94, 93, 92, 91]
            ]
          ]
        }
      ]
    },
    [
      [101,102,103,104,105,106,107,108,109,110],
      [111,112,113,114,115,116,117,118,119,120],
      [121,122,123,124,125,126,127,128,129,130],
      [131,132,133,134,135,136,137,138,139,140],
      [141,142,143,144,145,146,147,148,149,150],
      [151,152,153,154,155,156,157,158,159,160],
      [161,162,163,164,165,166,167,168,169,170],
      [171,172,173,174,175,176,177,178,179,180],
      [181,182,183,184,185,186,187,188,189,190],
      [191,192,193,194,195,196,197,198,199,200]
    ],
    {
      "operator": "-",
      "operands": [
        [
          [201,202,203,204,205,206,207,208,209,210],
          [211,212,213,214,215,216,217,218,219,220],
          [221,222,223,224,225,226,227,228,229,230],
          [231,232,233,234,235,236,237,238,239,240],
          [241,242,243,244,245,246,247,248,249,250],
          [251,252,253,254,255,256,257,258,259,260],
          [261,262,263,264,265,266,267,268,269,270],
          [271,272,273,274,275,276,277,278,279,280],
          [281,282,283,284,285,286,287,288,289,290],
          [291,292,293,294,295,296,297,298,299,300]
        ]
      ]
    }
  ]
}
""";

    // ----- NEW: Your 10x10 Expected Output -----
    public static final String EXAMPLE_10X10_EXPECTED = """
{
  "result" : [ [ -2.46812775E8, -2.4781405E8, -2.48815325E8, -2.498166E8, -2.50817875E8, -2.5181915E8, -2.52820425E8, -2.538217E8, -2.54822975E8, -2.5582425E8 ], [ -2.80207725E8, -2.8134445E8, -2.82481175E8, -2.836179E8, -2.84754625E8, -2.8589135E8, -2.87028075E8, -2.881648E8, -2.89301525E8, -2.9043825E8 ], [ -3.13602675E8, -3.1487485E8, -3.16147025E8, -3.174192E8, -3.18691375E8, -3.1996355E8, -3.21235725E8, -3.225079E8, -3.23780075E8, -3.2505225E8 ], [ -3.46997625E8, -3.4840525E8, -3.49812875E8, -3.512205E8, -3.52628125E8, -3.5403575E8, -3.55443375E8, -3.56851E8, -3.58258625E8, -3.5966625E8 ], [ -3.80392575E8, -3.8193565E8, -3.83478725E8, -3.850218E8, -3.86564875E8, -3.8810795E8, -3.89651025E8, -3.911941E8, -3.92737175E8, -3.9428025E8 ], [ -4.13787525E8, -4.1546605E8, -4.17144575E8, -4.188231E8, -4.20501625E8, -4.2218015E8, -4.23858675E8, -4.255372E8, -4.27215725E8, -4.2889425E8 ], [ -4.47182475E8, -4.4899645E8, -4.50810425E8, -4.526244E8, -4.54438375E8, -4.5625235E8, -4.58066325E8, -4.598803E8, -4.61694275E8, -4.6350825E8 ], [ -4.80577425E8, -4.8252685E8, -4.84476275E8, -4.864257E8, -4.88375125E8, -4.9032455E8, -4.92273975E8, -4.942234E8, -4.96172825E8, -4.9812225E8 ], [ -5.13972375E8, -5.1605725E8, -5.18142125E8, -5.20227E8, -5.22311875E8, -5.2439675E8, -5.26481625E8, -5.285665E8, -5.30651375E8, -5.3273625E8 ], [ -5.47367325E8, -5.4958765E8, -5.51807975E8, -5.540283E8, -5.56248625E8, -5.5846895E8, -5.60689275E8, -5.629096E8, -5.65129925E8, -5.6735025E8 ] ]
}
""";

    public static final Map<String, String> INPUTS;
    public static final Map<String, String> EXPECTED;

    static {
        Map<String, String> in = new HashMap<>();
        in.put("complex_nested.json", COMPLEX_NESTED);
        in.put("cpoilottest.json", CPOILOT_TEST);
        in.put("dim_mismatch.json", DIM_MISMATCH);
        in.put("example10x10.json", EXAMPLE_10X10_INPUT);
        // Additional examples expected by the tests
        in.put("invalid_operator.json", """
{
  "operator": "X",
  "operands": [
    [ [1, 2], [3, 4] ]
  ]
}
""");
        in.put("inconsistent_row.json", """
[ [1, 2], [3] ]
""");
        in.put("many_threads.json", COMPLEX_NESTED);

        // --- NEW TEST INPUTS (added for expanded test coverage) ---
        in.put("big_numbers_mul.json", """
{
  "operator": "*",
  "operands": [
    [ [1000000, 2000000], [3000000, 4000000] ],
    [ [5,6], [7,8] ]
  ]
}
""");
        in.put("identity_mul.json", """
{
  "operator": "*",
  "operands": [
    [ [1,0,0],[0,1,0],[0,0,1] ],
    [ [1,2,3],[4,5,6],[7,8,9] ]
  ]
}
""");
        in.put("zero_mul.json", """
{
  "operator": "*",
  "operands": [
    [ [0,0,0],[0,0,0],[0,0,0] ],
    [ [1,2,3],[4,5,6],[7,8,9] ]
  ]
}
""");
        in.put("negative_numbers.json", """
{
  "operator": "*",
  "operands": [
    [ [1,-2],[-3,4] ],
    [ [5,-6],[-7,8] ]
  ]
}
""");
        in.put("single_element.json", """
{
  "operator": "*",
  "operands": [ [ [7] ], [ [3] ] ]
}
""");
        in.put("non_square_mul.json", """
{
  "operator": "*",
  "operands": [
    [ [1,2,3],[4,5,6] ],
    [ [7,8],[9,10],[11,12] ]
  ]
}
""");
        in.put("transpose.json", """
{ "operator": "T", "operands": [ [ [1,2,3],[4,5,6] ] ] }
""");
        in.put("chained_ops.json", """
{
  "operator": "+",
  "operands": [
    {
      "operator": "*",
      "operands": [
        {
          "operator": "+",
          "operands": [ [ [1,1],[1,1] ], [ [2,2],[2,2] ] ]
        },
        [ [1,0],[0,1] ]
      ]
    },
    { "operator": "-", "operands": [ [ [1,1],[1,1] ] ] }
  ]
}
""");
        in.put("overflow_test.json", """
{
  "operator": "*",
  "operands": [ [ [1e150,0],[0,1e150] ], [ [1,2],[3,4] ] ]
}
""");
        in.put("many_threads_large.json", COMPLEX_NESTED);
        in.put("associative_test.json", """
{
  "operator": "+",
  "operands": [
    { "operator": "*", "operands": [ { "operator": "*", "operands": [ [ [1,2],[3,4] ], [ [5,6],[7,8] ] ] }, [ [9,10],[11,12] ] ] },
    { "operator": "-", "operands": [ { "operator": "*", "operands": [ [ [1,2],[3,4] ], { "operator": "*", "operands": [ [ [5,6],[7,8] ], [ [9,10],[11,12] ] ] } ] } ] }
  ]
}
""");
        in.put("transpose_double.json", """
{
  "operator": "*",
  "operands": [
    [ [1,2],[3,4] ],
    { "operator": "T", "operands": [ { "operator": "T", "operands": [ [ [5,6],[7,8] ] ] } ] }
  ]
}
""");
        in.put("identity_large.json", """
{
  "operator": "*",
  "operands": [
    { "operator": "T", "operands": [ [ [1,0,0,0,0],[0,1,0,0,0],[0,0,1,0,0],[0,0,0,1,0],[0,0,0,0,1] ] ] },
    [ [1,2,3,4,5],[6,7,8,9,10],[11,12,13,14,15],[16,17,18,19,20],[21,22,23,24,25] ]
  ]
}
""");
        in.put("invalid_operator2.json", """
{ "operator": "?", "operands": [ [ [1] ] ] }
""");
        in.put("inconsistent_row2.json", """
[ [1,2,3], [4,5] ]
""");
        in.put("dim_mismatch2.json", """
{ "operator": "*", "operands": [ [ [1,2] ], [ [1,2] ] ] }
""");
        in.put("transpose_eq.json", """
{ "operator": "*", "operands": [ [ [1,0],[0,1] ], { "operator": "T", "operands": [ [ [3,4],[5,6] ] ] } ] }
""");
        in.put("assoc_valid.json", """
{ "operator": "+", "operands": [ { "operator": "*", "operands": [ [ [1,0],[0,1] ], [ [2,0],[0,2] ] ] }, { "operator": "-", "operands": [ [ [2,0],[0,2] ] ] } ] }
""");
        in.put("large_identity5.json", """
{ "operator": "*", "operands": [ { "operator":"T", "operands": [ [ [1,0,0,0,0],[0,1,0,0,0],[0,0,1,0,0],[0,0,0,1,0],[0,0,0,0,1] ] ] }, [ [2,0,0,0,0],[0,2,0,0,0],[0,0,2,0,0],[0,0,0,2,0],[0,0,0,0,2] ] ] }
""");

        Map<String, String> ex = new HashMap<>();
        ex.put("cpoilottest.json", """
{ "result": [ [19.0, 22.0], [43.0, 50.0] ] }
""");
        // Historically there was a typo in the test expecting "copilotexpected.json" — provide it too
        ex.put("copilotexpected.json", """
{ "result": [ [19.0, 22.0], [43.0, 50.0] ] }
""");
        ex.put("example10x10.json", EXAMPLE_10X10_EXPECTED);

        // --- NEW EXPECTED OUTPUTS FOR ADDED TESTS ---
        ex.put("big_numbers_mul.json", """
{ "result": [ [ 19000000.0, 22000000.0 ], [ 43000000.0, 50000000.0 ] ] }
""");
        ex.put("identity_mul.json", """
{ "result": [ [1.0,2.0,3.0],[4.0,5.0,6.0],[7.0,8.0,9.0] ] }
""");
        ex.put("zero_mul.json", """
{ "result": [ [0.0,0.0,0.0],[0.0,0.0,0.0],[0.0,0.0,0.0] ] }
""");
        ex.put("negative_numbers.json", """
{ "result": [ [19.0, -22.0], [ -43.0, 50.0 ] ] }
""");
        ex.put("single_element.json", """
{ "result": [ [21.0] ] }
""");
        ex.put("non_square_mul.json", """
{ "result": [ [58.0,64.0],[139.0,154.0] ] }
""");
        ex.put("transpose.json", """
{ "result": [ [1.0,4.0],[2.0,5.0],[3.0,6.0] ] }
""");
        ex.put("chained_ops.json", """
{ "result": [ [2.0,2.0],[2.0,2.0] ] }
""");
        ex.put("overflow_test.json", """
{ "result": [ [1.0E150, 2.0E150], [2.9999999999999998E150, 4.0E150] ] }
""");
        ex.put("many_threads_large.json", """
{ "result": [ [ -2.46812775E8, -2.4781405E8 ], [ -2.48815325E8, -2.498166E8 ] ] }
""");
        ex.put("associative_test.json", """
{ "result": [ [ 0.0, 0.0 ], [ 0.0, 0.0 ] ] }
""");
        ex.put("transpose_double.json", """
{ "result": [ [ 19.0, 22.0 ], [ 43.0, 50.0 ] ] }
""");
        ex.put("identity_large.json", """
{ "result": [ [1.0,2.0,3.0,4.0,5.0],[6.0,7.0,8.0,9.0,10.0],[11.0,12.0,13.0,14.0,15.0],[16.0,17.0,18.0,19.0,20.0],[21.0,22.0,23.0,24.0,25.0] ] }
""");
        ex.put("invalid_operator2.json", """
{ "error": "Unknown operator: ?" }
""");
        ex.put("inconsistent_row2.json", """
{ "error": "Inconsistent row sizes in matrix." }
""");
        ex.put("dim_mismatch2.json", """
{ "error": "One or more tasks failed during execution" }
""");
        ex.put("transpose_eq.json", """
{ "result": [ [3.0,5.0],[4.0,6.0] ] }
""");
        ex.put("assoc_valid.json", """
{ "result": [ [0.0,0.0],[0.0,0.0] ] }
""");
        ex.put("large_identity5.json", """
{ "result": [ [2.0,0.0,0.0,0.0,0.0],[0.0,2.0,0.0,0.0,0.0],[0.0,0.0,2.0,0.0,0.0],[0.0,0.0,0.0,2.0,0.0],[0.0,0.0,0.0,0.0,2.0] ] }
""");

        INPUTS = Collections.unmodifiableMap(in);
        EXPECTED = Collections.unmodifiableMap(ex);
    }

    private AllExamplesData() {}
}