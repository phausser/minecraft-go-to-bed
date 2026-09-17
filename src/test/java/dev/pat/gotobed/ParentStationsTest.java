package dev.pat.gotobed;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ParentStationsTest {

    private static final double DISTANCE = 2.0;
    private static final double LATERAL = 0.7;

    @Test
    void facingSouthPapaIsOnTheLeftEast() {
        ParentStations.Offset papa = ParentStations.papa(0f, DISTANCE, LATERAL);
        ParentStations.Offset mama = ParentStations.mama(0f, DISTANCE, LATERAL);
        assertEquals(LATERAL, papa.x(), 1e-9);
        assertEquals(DISTANCE, papa.z(), 1e-9);
        assertEquals(-LATERAL, mama.x(), 1e-9);
        assertEquals(DISTANCE, mama.z(), 1e-9);
    }

    @Test
    void facingWestPapaIsOnTheLeftSouth() {
        ParentStations.Offset papa = ParentStations.papa(90f, DISTANCE, LATERAL);
        ParentStations.Offset mama = ParentStations.mama(90f, DISTANCE, LATERAL);
        assertEquals(-DISTANCE, papa.x(), 1e-9);
        assertEquals(LATERAL, papa.z(), 1e-9);
        assertEquals(-DISTANCE, mama.x(), 1e-9);
        assertEquals(-LATERAL, mama.z(), 1e-9);
    }
}
