#!/usr/bin/env python3

import subprocess
import sys
from collections import defaultdict


def run_one(lox):
    expected = ""
    with open(lox) as loxfile:
        for line in loxfile.readlines():
            parts = line.split('// expect: ')
            if len(parts) == 2:
                expected += parts[1]

    print(f"Running {lox}... ", end="", flush=True)
    completed = subprocess.run(["/Users/aneel/Library/Java/JavaVirtualMachines/openjdk-14/Contents/Home/bin/java",
                   "-classpath", "out/production/jlox",
                   "net.chthonic.lox.Lox",
                   lox], capture_output=True, text=True);
    output = completed.stdout + completed.stderr
    if output == expected:
        print("PASS")
        return "PASS"
    else:
        print("FAIL")
        print("EXPECTED --------")
        print(expected)
        print('OUTPUT ----------')
        print(output)
        return "FAIL"



if __name__ == '__main__':
    results = defaultdict(int)
    for lox in sys.argv[1:]:
        results[run_one(lox)] += 1
    [print(f"{k}\t{v}") for (k, v) in results.items()]