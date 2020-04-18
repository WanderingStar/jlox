package net.chthonic.lox;

/* Exceptions were my first thought about how to implement break, but
   it seemed like cheating. But if we're going to do that for return...
 */
class Break extends RuntimeException {
    final String label;

    Break(String label) {
        super(null, null, false, false);
        this.label = label;
    }
}