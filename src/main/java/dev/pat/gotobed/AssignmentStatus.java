package dev.pat.gotobed;

enum AssignmentStatus {
    SCHEDULED,
    ACTIVE;

    String yaml() {
        return name().toLowerCase();
    }

    static AssignmentStatus fromYaml(String raw) {
        if (raw != null && raw.equalsIgnoreCase("active")) {
            return ACTIVE;
        }
        return SCHEDULED;
    }
}
