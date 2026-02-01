package mx.jume.aquahunger.config;

public sealed interface HudPosition
        permits HudPosition.Preset, HudPosition.Custom {

    int left();

    int bottom();

    String name();

    enum Preset implements HudPosition {

        AboveHotbarCentered(0, 140),
        BelowHotbarCentered(0, 12),
        AboveHotbarLeft(0, 140),
        AboveHotbarRight(0, 140),
        BelowHotbarLeft(0, 8),
        BelowHotbarRight(0, 8),
        BottomLeft(-1, 12),
        BottomRight(-1, 12);

        private final int left;
        private final int bottom;

        Preset(int left, int bottom) {
            this.left = left;
            this.bottom = bottom;
        }

        @Override
        public int left() {
            return left;
        }

        @Override
        public int bottom() {
            return bottom;
        }
    }

    record Custom(int left, int bottom) implements HudPosition {
        @Override
        public String name() {
            return "Custom:" + left + ":" + bottom;
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
        } else if (parts.length == 3 && parts[0].equals("Custom")) {
            int left = Integer.parseInt(parts[1]);
            int bottom = Integer.parseInt(parts[2]);
            return new Custom(left, bottom);
        } else {
            return null;
        }
    }
}
