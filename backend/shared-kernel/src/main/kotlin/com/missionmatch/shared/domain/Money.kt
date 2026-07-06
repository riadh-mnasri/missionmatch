package com.missionmatch.shared.domain

import java.math.BigDecimal

data class Money(val amount: BigDecimal, val currency: String = "EUR") {

    init {
        require(amount >= BigDecimal.ZERO) { "Money amount cannot be negative" }
        require(currency.length == 3) { "Currency must be a 3-letter ISO code" }
    }

    operator fun compareTo(other: Money): Int {
        require(currency == other.currency) { "Cannot compare amounts in different currencies" }
        return amount.compareTo(other.amount)
    }

    companion object {
        fun of(amount: Double, currency: String = "EUR"): Money = Money(BigDecimal.valueOf(amount), currency)
    }
}
