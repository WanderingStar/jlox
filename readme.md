Following 
https://craftinginterpreters.com

Implemented challenges:
- 6.1: `,` operator
- 6.2: `?:` operator
- 9.3: `break` statement
  - originally thought of using exceptions, but it seemed like cheating,
    so I did it by adding a `broken` flag to the interpreter, but went back and
    used exceptions after seeing how `return` is implemented
  - bonus: `break` statement with labeled loops:
```
for outer (var i=0; i<10; i=i+1) {
    while inner (true) {
        print i;
        if (i>5) break outer;
        i=i+1;
    }
    print "outer";
}
```
- 10.2: anonymous functions
- 11.3: unused variable errors. special treatment for variables named `_`
  (don't complain if they aren't used or if they're redefined, but do complain if they _are_ used)
