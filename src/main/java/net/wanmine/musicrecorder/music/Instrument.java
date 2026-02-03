package net.wanmine.musicrecorder.music;

/**
 * Available instruments for music recording.
 * Each instrument has 3 octaves.
 */
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

    /**
     * Gets the base frequency for this instrument.
     * Used for generating audio.
     */
    public double getBaseFrequency() {
        return switch (this) {
            case PIANO, STRINGS, SYNTH -> 440.0; // A4
            case DRUM -> 200.0;  // Lower frequency for drums
        };
    }

    /**
     * Gets the waveform type for this instrument.
     */
    public WaveformType getWaveformType() {
        return switch (this) {
            case PIANO -> WaveformType.TRIANGLE;
            case DRUM -> WaveformType.NOISE;
            case STRINGS -> WaveformType.SAWTOOTH;
            case SYNTH -> WaveformType.SQUARE;
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
