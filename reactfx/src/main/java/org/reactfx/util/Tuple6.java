package org.reactfx.util;

import static org.reactfx.util.Tuples.*;

import java.util.Objects;

public class Tuple6<A, B, C, D, E, F> {
    public final A _1;
    public final B _2;
    public final C _3;
    public final D _4;
    public final E _5;
    public final F _6;

    Tuple6(A a, B b, C c, D d, E e, F f) {
        _1 = a;
        _2 = b;
        _3 = c;
        _4 = d;
        _5 = e;
        _6 = f;
    }

    public A get1() { return _1; }
    public B get2() { return _2; }
    public C get3() { return _3; }
    public D get4() { return _4; }
    public E get5() { return _5; }
    public F get6() { return _6; }

    public Tuple6<A, B, C, D, E, F> update1(A a) {
        return t(a, _2, _3, _4, _5, _6);
    }

    public Tuple6<A, B, C, D, E, F> update2(B b) {
        return t(_1, b, _3, _4, _5, _6);
    }

    public Tuple6<A, B, C, D, E, F> update3(C c) {
        return t(_1, _2, c, _4, _5, _6);
    }

    public Tuple6<A, B, C, D, E, F> update4(D d) {
        return t(_1, _2, _3, d, _5, _6);
    }

    public Tuple6<A, B, C, D, E, F> update5(E e) {
        return t(_1, _2, _3, _4, e, _6);
    }

    public Tuple6<A, B, C, D, E, F> update6(F f) {
        return t(_1, _2, _3, _4, _5, f);
    }

    public <T> T map(HexaFunction<? super A, ? super B, ? super C, ? super D, ? super E, ? super F, ? extends T> f) {
        return f.apply(_1, _2, _3, _4, _5, _6);
    }

    public boolean test(HexaPredicate<? super A, ? super B, ? super C, ? super D, ? super E, ? super F> f) {
        return f.test(_1, _2, _3, _4, _5, _6);
    }

    public void exec(HexaConsumer<? super A, ? super B, ? super C, ? super D, ? super E, ? super F> f) {
        f.accept(_1, _2, _3, _4, _5, _6);
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof Tuple6) {
            Tuple6<?, ?, ?, ?, ?, ?> that = (Tuple6<?, ?, ?, ?, ?, ?>) other;
            return Objects.equals(this._1, that._1)
                    && Objects.equals(this._2, that._2)
                    && Objects.equals(this._3, that._3)
                    && Objects.equals(this._4, that._4)
                    && Objects.equals(this._5, that._5)
                    && Objects.equals(this._6, that._6);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(_1, _2, _3, _4, _5, _6);
    }

    @Override
    public String toString() {
        return "("
                + Objects.toString(_1) + ", "
                + Objects.toString(_2) + ", "
                + Objects.toString(_3) + ", "
                + Objects.toString(_4) + ", "
                + Objects.toString(_5) + ", "
                + Objects.toString(_6)
                + ")";
    }
}
