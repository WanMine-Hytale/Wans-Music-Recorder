package net.wanmine.musicrecorder.music;

public enum Instrument {
    PIANO("Piano"),
    DRUM("Drum"),
    STRINGS("Strings"),
    SYNTH("Synth");

    private final String displayName;

    Instrument(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getBaseFrequency() {
        return switch (this) {
            case PIANO, STRINGS, SYNTH -> 440.0;
            case DRUM -> 200.0;
        };
    }

    public WaveformType getWaveformType() {
        return switch (this) {
            case PIANO -> WaveformType.TRIANGLE;
            case DRUM, SYNTH -> WaveformType.SQUARE;
            case STRINGS -> WaveformType.SINE;
        };
    }

    public enum WaveformType {
        SINE,
        SQUARE,
        TRIANGLE,
        SAWTOOTH,
        NOISE
    }
}
