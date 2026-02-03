package net.wanmine.musicrecorder.music;

/**
 * Builder class for creating MusicGraph instances with a fluent API.
 * Provides convenient methods for adding notes and configuring the graph.
 * Each octave contains 12 chromatic semitones (C, C#, D, D#, E, F, F#, G, G#, A, A#, B).
 */
public class MusicGraphBuilder {
    private final MusicGraph graph;
    private Instrument currentInstrument;

    // Helper constants for note names to semitone values
    public static final int C = 0;
    public static final int C_SHARP = 1;
    public static final int D = 2;
    public static final int D_SHARP = 3;
    public static final int E = 4;
    public static final int F = 5;
    public static final int F_SHARP = 6;
    public static final int G = 7;
    public static final int G_SHARP = 8;
    public static final int A = 9;
    public static final int A_SHARP = 10;
    public static final int B = 11;

    public MusicGraphBuilder() {
        this.graph = new MusicGraph();
        this.currentInstrument = Instrument.PIANO;
    }

    public MusicGraphBuilder(int maxOctaves, int tempo, int gridLength) {
        this.graph = new MusicGraph(maxOctaves, tempo, gridLength);
        this.currentInstrument = Instrument.PIANO;
    }

    /**
     * Sets the tempo of the music graph.
     */
    public MusicGraphBuilder tempo(int bpm) {
        graph.setTempo(bpm);
        return this;
    }

    /**
     * Sets the grid length of the music graph.
     */
    public MusicGraphBuilder gridLength(int length) {
        graph.setGridLength(length);
        return this;
    }

    /**
     * Sets the current instrument for subsequent note additions.
     */
    public MusicGraphBuilder instrument(Instrument instrument) {
        this.currentInstrument = instrument;
        return this;
    }

    /**
     * Adds a note with the current instrument.
     *
     * @param octave The octave (0-2)
     * @param semitone The semitone (0-11): 0=C, 1=C#, 2=D, 3=D#, 4=E, 5=F, 6=F#, 7=G, 8=G#, 9=A, 10=A#, 11=B
     * @param position The position in the grid
     * @return This builder for chaining
     */
    public MusicGraphBuilder note(int octave, int semitone, int position) {
        return note(octave, semitone, position, 1);
    }

    /**
     * Adds a note with the current instrument and custom length.
     *
     * @param octave The octave (0-2)
     * @param semitone The semitone (0-11): 0=C, 1=C#, 2=D, 3=D#, 4=E, 5=F, 6=F#, 7=G, 8=G#, 9=A, 10=A#, 11=B
     * @param position The position in the grid
     * @param length The length of the note
     * @return This builder for chaining
     */
    public MusicGraphBuilder note(int octave, int semitone, int position, int length) {
        graph.addNote(new Note(octave, semitone, position, length, currentInstrument));
        return this;
    }

    /**
     * Adds a note with a specific instrument.
     *
     * @param octave The octave (0-2)
     * @param semitone The semitone (0-11): 0=C, 1=C#, 2=D, 3=D#, 4=E, 5=F, 6=F#, 7=G, 8=G#, 9=A, 10=A#, 11=B
     * @param position The position in the grid
     * @param length The length of the note
     * @param instrument The instrument to use
     * @return This builder for chaining
     */
    public MusicGraphBuilder note(int octave, int semitone, int position, int length, Instrument instrument) {
        graph.addNote(new Note(octave, semitone, position, length, instrument));
        return this;
    }

    /**
     * Adds multiple notes from a pattern string for the current instrument.
     * Pattern format: Each line represents a semitone within octaves
     * 'X' or 'x' = note, '-' or ' ' = empty space
     * <p>
     * For 3 octaves, you'll have 36 rows (12 semitones × 3 octaves)
     * Top row = B in highest octave, bottom row = C in lowest octave
     * <p>
     * Example (simplified, shows only a few rows):
     * ---X--X--X  (B2)
     * -X-X--XX--  (A#2)
     * ---X---X--  (A2)
     * ...
     *
     * @param pattern The pattern string (can be multi-line)
     * @return This builder for chaining
     */
    public MusicGraphBuilder pattern(String pattern) {
        String[] lines = pattern.split("\n");
        int maxRows = graph.getTotalRows(); // octaves × 12

        for (int row = 0; row < lines.length && row < maxRows; row++) {
            String line = lines[row];

            // Calculate octave and semitone from row index
            // Row 0 = highest semitone in highest octave
            int octaveFromTop = row / 12;
            int semitoneFromTop = row % 12;

            int actualOctave = graph.getMaxOctaves() - 1 - octaveFromTop;
            int actualSemitone = 11 - semitoneFromTop; // 11=B down to 0=C

            if (actualOctave < 0) continue;

            for (int pos = 0; pos < line.length() && pos < graph.getGridLength(); pos++) {
                char c = line.charAt(pos);
                if (c == 'X' || c == 'x') {
                    note(actualOctave, actualSemitone, pos, 1);
                }
            }
        }

        return this;
    }

    /**
     * Adds multiple notes from a pattern string with a specific instrument.
     */
    public MusicGraphBuilder pattern(String pattern, Instrument instrument) {
        Instrument previousInstrument = this.currentInstrument;

        this.currentInstrument = instrument;

        pattern(pattern);

        this.currentInstrument = previousInstrument;

        return this;
    }

    /**
     * Adds a sequence of notes at specific positions with equal spacing.
     *
     * @param octave The octave for all notes
     * @param semitone The semitone for all notes
     * @param startPosition Starting position
     * @param spacing Space between notes
     * @param count Number of notes to add
     * @return This builder for chaining
     */
    public MusicGraphBuilder sequence(int octave, int semitone, int startPosition, int spacing, int count) {
        for (int i = 0; i < count; i++) {
            note(octave, semitone, startPosition + i * spacing, 1);
        }

        return this;
    }

    /**
     * Adds a chord (multiple semitones at the same position in the same octave).
     *
     * @param octave The octave
     * @param position The position in the grid
     * @param length The length of the chord
     * @param semitones The semitones to include in the chord
     * @return This builder for chaining
     */
    public MusicGraphBuilder chord(int octave, int position, int length, int... semitones) {
        for (int semitone : semitones) {
            note(octave, semitone, position, length);
        }

        return this;
    }

    /**
     * Copies notes from another MusicGraph.
     */
    public MusicGraphBuilder copyFrom(MusicGraph other) {
        for (Note note : other.getNotes()) {
            graph.addNote(note);
        }

        return this;
    }

    /**
     * Clears all notes from the graph.
     */
    public MusicGraphBuilder clear() {
        graph.clear();

        return this;
    }

    /**
     * Builds and returns the final MusicGraph.
     */
    public MusicGraph build() {
        return graph;
    }
}
