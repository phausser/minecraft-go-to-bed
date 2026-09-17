package dev.pat.gotobed;

enum Speaker {
    PAPA,
    MAMA;

    Speaker next() {
        return this == PAPA ? MAMA : PAPA;
    }

    String yaml() {
        return name().toLowerCase();
    }

    static Speaker fromYaml(String raw) {
        if (raw != null && raw.equalsIgnoreCase("mama")) {
            return MAMA;
        }
        return PAPA;
    }
}
