package com.missionmatch.shared.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class MoneyTest {

    @Test
    fun `rejects a negative amount`() {
        // Given
        val negativeAmount = BigDecimal.valueOf(-1)

        // When
        // Then
        assertThatThrownBy { Money(negativeAmount) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `compares two amounts in the same currency`() {
        // Given
        val lower = Money.of(400.0)
        val higher = Money.of(550.0)

        // When
        val comparison = lower.compareTo(higher)

        // Then
        assertThat(comparison).isNegative()
    }

    @Test
    fun `refuses to compare amounts in different currencies`() {
        // Given
        val euros = Money.of(400.0, "EUR")
        val dollars = Money.of(400.0, "USD")

        // When
        // Then
        assertThatThrownBy { euros.compareTo(dollars) }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
