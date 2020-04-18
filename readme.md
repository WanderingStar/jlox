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
