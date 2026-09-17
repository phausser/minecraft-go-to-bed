package dev.pat.gotobed;

final class ParentStations {

    record Offset(double x, double z) {}

    private ParentStations() {}

    /**
     * Horizontal offset from the player's feet. Minecraft yaw 0 looks south (+Z);
     * Papa stands to the player's left, Mama to the right.
     */
    static Offset papa(float yawDegrees, double distance, double lateral) {
        return offset(yawDegrees, distance, lateral, true);
    }

    static Offset mama(float yawDegrees, double distance, double lateral) {
        return offset(yawDegrees, distance, lateral, false);
    }

    private static Offset offset(float yawDegrees, double distance, double lateral, boolean left) {
        double yaw = Math.toRadians(yawDegrees);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double leftX = forwardZ;
        double leftZ = -forwardX;
        double side = left ? lateral : -lateral;
        return new Offset(forwardX * distance + leftX * side, forwardZ * distance + leftZ * side);
    }
}
