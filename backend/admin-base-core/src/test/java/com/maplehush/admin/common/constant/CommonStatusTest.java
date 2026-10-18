package com.maplehush.admin.common.constant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CommonStatusTest {
    @Test
    void mapsApiAndDatabaseValues() {
        assertEquals(1, CommonStatus.fromApi("ENABLED").databaseValue());
        assertEquals(0, CommonStatus.fromApi("DISABLED").databaseValue());
        assertEquals("ENABLED", CommonStatus.fromDatabase(1).apiValue());
        assertEquals("DISABLED", CommonStatus.fromDatabase(0).apiValue());
    }

    @Test
    void rejectsUnknownValues() {
        assertThrows(IllegalArgumentException.class, () -> CommonStatus.fromApi("enabled"));
        assertThrows(IllegalArgumentException.class, () -> CommonStatus.fromApi(null));
        assertThrows(IllegalArgumentException.class, () -> CommonStatus.fromDatabase(2));
    }
}
