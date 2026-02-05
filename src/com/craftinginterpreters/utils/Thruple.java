package com.craftinginterpreters.utils;

public class Thruple<A, B, C> {
    public A fst;
    public B snd;
    public C trd;

    public Thruple(A fst, B snd, C trd) {
        this.fst = fst;
        this.snd = snd;
        this.trd = trd;
    }
}
