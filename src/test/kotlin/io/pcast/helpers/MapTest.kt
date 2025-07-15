package io.pcast.helpers

import kotlin.test.Test
import kotlin.test.assertEquals

internal class MapTest {
    @Test
    fun testBuildMutableMap() {
        // Create a map using buildMutableMap
        val map =
            buildMutableMap {
                put("one", 1)
                put("two", 2)
                put("three", 3)
            }

        // Verify the map contains the expected entries
        assertEquals(3, map.size)
        assertEquals(1, map["one"])
        assertEquals(2, map["two"])
        assertEquals(3, map["three"])

        // Verify the map is mutable by modifying it
        map["four"] = 4
        map.remove("one")

        // Check the modifications worked
        assertEquals(3, map.size)
        assertEquals(null, map["one"])
        assertEquals(4, map["four"])
    }
}
