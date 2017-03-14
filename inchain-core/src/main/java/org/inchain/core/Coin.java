package org.inchain.core;

import java.io.Serializable;
import java.math.BigDecimal;

import org.inchain.utils.Utils;

/**
 * INS 单位
 * @author ln
 *
 */
public final class Coin implements Comparable<Coin>, Serializable {

	private static final long serialVersionUID = 6978149202334427537L;

	public static final int SMALLEST_UNIT_EXPONENT = 8;

    private static final long COIN_VALUE = (long) Math.pow(10, SMALLEST_UNIT_EXPONENT);

    /**
     * 10亿总量
     */
    public static final Coin MAX = Coin.valueOf(1000000000).multiply(COIN_VALUE);
    
    /**
     * 0个 INS
     */
    public static final Coin ZERO = Coin.valueOf(0);

    /**
     * 1个 INS
     */
    public static final Coin COIN = Coin.valueOf(COIN_VALUE);

    /**
     * 0.01 INS
     */
    public static final Coin CENT = COIN.divide(100);

    /**
     * 0.001 INS
     */
    public static final Coin MILLICOIN = COIN.divide(1000);

    /**
     * 0.000001 INS
     */
    public static final Coin MICROCOIN = MILLICOIN.divide(1000);

    /**
     * 数量
     */
    public final long value;

    private Coin(final long coin) {
        this.value = coin;
    }

    public static Coin valueOf(final long coin) {
        return new Coin(coin);
    }

    public int smallestUnitExponent() {
        return SMALLEST_UNIT_EXPONENT;
    }

    public long getValue() {
        return value;
    }

    public static Coin valueOf(final int coins, final int cents) {
    	Utils.checkState(cents < 100);
        Utils.checkState(cents >= 0);
        Utils.checkState(coins >= 0);
        final Coin coin = COIN.multiply(coins).add(CENT.multiply(cents));
        return coin;
    }

    public static Coin parseCoin(final String str) {
        try {
            long value = new BigDecimal(str).movePointRight(SMALLEST_UNIT_EXPONENT).toBigIntegerExact().longValue();
            return Coin.valueOf(value);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Coin add(final Coin value) {
        return new Coin(this.value + value.value);
    }

    /** Alias for add */
    public Coin plus(final Coin value) {
        return add(value);
    }

    public Coin subtract(final Coin value) {
        return new Coin(this.value - value.value);
    }

    /** Alias for subtract */
    public Coin minus(final Coin value) {
        return subtract(value);
    }

    public Coin multiply(final long factor) {
        return new Coin(this.value * factor);
    }

    /** Alias for multiply */
    public Coin times(final long factor) {
        return multiply(factor);
    }

    /** Alias for multiply */
    public Coin times(final int factor) {
        return multiply(factor);
    }

    public Coin divide(final long divisor) {
        return new Coin(this.value / divisor);
    }

    /** Alias for divide */
    public Coin div(final long divisor) {
        return divide(divisor);
    }

    /** Alias for divide */
    public Coin div(final int divisor) {
        return divide(divisor);
    }

    public Coin[] divideAndRemainder(final long divisor) {
        return new Coin[] { new Coin(this.value / divisor), new Coin(this.value % divisor) };
    }

    public long divide(final Coin divisor) {
        return this.value / divisor.value;
    }
    
    /**
     * Returns true if and only if this instance represents a monetary value greater than zero,
     * otherwise false.
     */
    public boolean isPositive() {
        return signum() == 1;
    }

    /**
     * Returns true if and only if this instance represents a monetary value less than zero,
     * otherwise false.
     */
    public boolean isNegative() {
        return signum() == -1;
    }

    /**
     * Returns true if and only if this instance represents zero monetary value,
     * otherwise false.
     */
    public boolean isZero() {
        return signum() == 0;
    }

    /**
     * Returns true if the monetary value represented by this instance is greater than that
     * of the given other Coin, otherwise false.
     */
    public boolean isGreaterThan(Coin other) {
        return compareTo(other) > 0;
    }

    /**
     * Returns true if the monetary value represented by this instance is less than that
     * of the given other Coin, otherwise false.
     */
    public boolean isLessThan(Coin other) {
        return compareTo(other) < 0;
    }

    public Coin shiftLeft(final int n) {
        return new Coin(this.value << n);
    }

    public Coin shiftRight(final int n) {
        return new Coin(this.value >> n);
    }

    public int signum() {
        if (this.value == 0)
            return 0;
        return this.value < 0 ? -1 : 1;
    }

    public Coin negate() {
        return new Coin(-this.value);
    }

    /**
     * Returns the number of satoshis of this monetary value. It's deprecated in favour of accessing {@link #value}
     * directly.
     */
    public long longValue() {
        return this.value;
    }

    public String toText() {
		return new BigDecimal(value).divide(BigDecimal.valueOf(Coin.COIN.value)).toPlainString();
    }
    
    @Override
    public String toString() {
        return Long.toString(value);
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return this.value == ((Coin)o).value;
    }

    @Override
    public int hashCode() {
        return (int) this.value;
    }

    @Override
    public int compareTo(final Coin other) {
        return Long.compare(this.value, other.value);
    }
}
