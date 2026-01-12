package com.foobuilders

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

final class SanityTest {
  @Test
  def sanity(): Unit = {
    assertEquals(2, 1 + 1)
  }
}
