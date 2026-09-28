package com.smish.wheresmymoney.util

import kotlin.random.Random

/**
 * Millisecond timestamp + a random component. Two devices creating a record in the exact
 * same millisecond would need to also pick the same 0–99999 random number to collide —
 * more than safe enough for personal, few-device use.
 */
object IdGenerator {
    fun newId(): Long = System.currentTimeMillis() * 100_000L + Random.nextInt(100_000)
}
