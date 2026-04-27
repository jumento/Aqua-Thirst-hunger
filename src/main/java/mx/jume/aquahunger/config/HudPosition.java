package mx.jume.aquahunger.config;

public sealed interface HudPosition
        permits HudPosition.Preset, HudPosition.Custom {

    int left();

    int bottom();

    int right();

    boolean centered();

    String name();

    enum Preset implements HudPosition {

        AboveHotbarCentered(0, 140, -1, true),
        BelowHotbarCentered(0, 12, -1, true),
        AboveHotbarLeft(0, 140, -1, false),
        AboveHotbarRight(0, 140, -1, false),
        BelowHotbarLeft(0, 8, -1, false),
        BelowHotbarRight(0, 8, -1, false),
        BottomLeft(-1, 12, -1, false),
        BottomRight(-1, 12, 0, false);

        private final int left;
        private final int bottom;
        private final int right;
        private final boolean centered;

        Preset(int left, int bottom, int right, boolean centered) {
            this.left = left;
            this.bottom = bottom;
            this.right = right;
            this.centered = centered;
        }

        @Override
        public int left() {
            return left;
        }

        @Override
        public int bottom() {
            return bottom;
        }

        @Override
        public int right() {
            return right;
        }

        @Override
        public boolean centered() {
            return centered;
        }
    }

    record Custom(int left, int bottom, int right, boolean centered) implements HudPosition {

        public Custom(int left, int bottom) {
            this(left, bottom, -1, false);
        }

        @Override
        public String name() {
            return "Custom:" + left + ":" + bottom + ":" + right + ":" + centered;
        }
    }

    static HudPosition pluginDefault() {
        return Preset.AboveHotbarCentered;
    }

    static HudPosition valueOf(String name) {
        if (name == null || name.isEmpty())
            return null;

        String[] parts = name.split(":");
        if (parts.length == 1) {
            try {
                return Preset.valueOf(name);
            } catch (IllegalArgumentException e) {
                return null;
            }
        } else if (parts.length >= 3 && parts[0].equals("Custom")) {
            int left = Integer.parseInt(parts[1]);
            int bottom = Integer.parseInt(parts[2]);
            int right = parts.length > 3 ? Integer.parseInt(parts[3]) : -1;
            boolean centered = parts.length > 4 ? Boolean.parseBoolean(parts[4]) : false;
            return new Custom(left, bottom, right, centered);
        } else {
            return null;
        }
    }
}
