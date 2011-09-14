package net.slightlymagic.maxmtg;

import net.slightlymagic.braids.util.lambda.Lambda1;

/** This class represents an action (lambda) and some arguments to make a call at a later time */ 
public class Closure1<R, A1> {
    private final Lambda1<R, A1> method;
    private final A1 argument;
    
    public Closure1(Lambda1<R, A1> lambda, A1 object) {
        method = lambda;
        argument = object;
    }
    public R apply() { return method.apply(argument); }
}
