package app.ghghorg.atsassets.core

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * An exact fraction (spec 4.10): an arbitrary-precision numerator over a positive denominator, reduced by their
 * greatest common divisor. Derived values (price per share, lot cost, shares) are kept this way and rounded only
 * when shown.
 */
class Fraction private constructor(
    val numerator: BigInteger,
    val denominator: BigInteger,
) : Comparable<Fraction> {
    operator fun plus(other: Fraction): Fraction {
        val top = numerator * other.denominator + other.numerator * denominator
        return of(top, denominator * other.denominator)
    }

    operator fun minus(other: Fraction): Fraction {
        val top = numerator * other.denominator - other.numerator * denominator
        return of(top, denominator * other.denominator)
    }

    operator fun times(other: Fraction): Fraction = of(numerator * other.numerator, denominator * other.denominator)

    /** Throws IllegalArgumentException when [other] is zero. */
    operator fun div(other: Fraction): Fraction = of(numerator * other.denominator, denominator * other.numerator)

    override fun compareTo(other: Fraction): Int = (numerator * other.denominator).compareTo(other.numerator * denominator)

    fun signum(): Int = numerator.signum()

    fun abs(): Fraction = if (numerator.signum() < 0) of(numerator.negate(), denominator) else this

    /** Rounded half away from zero to [scale] decimals (4.10: rounding happens only when a value is shown). */
    fun rounded(scale: Int): BigDecimal = BigDecimal(numerator).divide(BigDecimal(denominator), scale, RoundingMode.HALF_UP)

    override fun equals(other: Any?): Boolean = other is Fraction && numerator == other.numerator && denominator == other.denominator

    override fun hashCode(): Int = 31 * numerator.hashCode() + denominator.hashCode()

    override fun toString(): String = "$numerator/$denominator"

    companion object {
        private val HUNDRED = BigInteger.valueOf(100)

        val ZERO: Fraction = of(BigInteger.ZERO, BigInteger.ONE)

        /** Throws IllegalArgumentException when [denominator] is zero. */
        fun of(
            numerator: BigInteger,
            denominator: BigInteger,
        ): Fraction {
            require(denominator.signum() != 0) { "denominator is zero" }
            val gcd = numerator.gcd(denominator)
            val sign = BigInteger.valueOf(denominator.signum().toLong())
            return Fraction(numerator / gcd * sign, denominator / gcd * sign)
        }

        /** An exact decimal, such as a quantity as typed. */
        fun of(decimal: BigDecimal): Fraction = if (decimal.scale() >= 0) {
            of(decimal.unscaledValue(), BigInteger.TEN.pow(decimal.scale()))
        } else {
            of(decimal.toBigIntegerExact(), BigInteger.ONE)
        }

        /** Integer cents as dollars. */
        fun ofCents(cents: Long): Fraction = of(BigInteger.valueOf(cents), HUNDRED)
    }
}
