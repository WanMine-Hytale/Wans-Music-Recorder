package net.wanmine.musicrecorder.music;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.util.Objects;

/**
 * Represents a single note in the music graph.
 * Each octave contains 12 chromatic semitones (all notes including sharps/flats).
 */
public class Note {
    private int octave; // 0-2 (3 octaves)
    private int semitone; // 0-11 (12 chromatic semitones per octave)
    private int position; // Position in the timeline
    private int length; // Length in grid spaces (minimum 1)
    private Instrument instrument;

    // Note names for each semitone (using sharps)
    // 0=C, 1=C#, 2=D, 3=D#, 4=E, 5=F, 6=F#, 7=G, 8=G#, 9=A, 10=A#, 11=B
    private static final String[] NOTE_NAMES = {
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };

    public static final BuilderCodec<Note> CODEC = BuilderCodec.builder(
                    Note.class,
                    Note::new
            )
            .append(new KeyedCodec<>("Octave", Codec.INTEGER), (note, value) -> note.octave = value, note -> note.octave)
            .add()
            .append(new KeyedCodec<>("Semitone", Codec.INTEGER), (note, value) -> note.semitone = value, note -> note.semitone)
            .add()
            .append(new KeyedCodec<>("Position", Codec.INTEGER), (note, value) -> note.position = value, note -> note.position)
            .add()
            .append(new KeyedCodec<>("Length", Codec.INTEGER), (note, value) -> note.length = value, note -> note.length)
            .add()
            .append(new KeyedCodec<>("Instrument", Codec.STRING), (note, value) -> note.instrument = Instrument.valueOf(value), note -> note.instrument.name())
            .add()
            .build();

    public Note() {
        this.octave = 0;
        this.semitone = 0;
        this.position = 0;
        this.length = 1;
        this.instrument = Instrument.PIANO;
    }

    /**
     * Creates a note with a specific semitone value.
     *
     * @param octave The octave (0-2)
     * @param semitone The chromatic semitone (0-11): 0=C, 1=C#, 2=D, 3=D#, 4=E, 5=F, 6=F#, 7=G, 8=G#, 9=A, 10=A#, 11=B
     * @param position The position in the timeline
     * @param length The length in grid spaces
     * @param instrument The instrument
     */
    public Note(int octave, int semitone, int position, int length, Instrument instrument) {
        if (octave < 0 || octave >= 3) {
            throw new IllegalArgumentException("Octave must be between 0 and 2 (inclusive)");
        }

        if (semitone < 0 || semitone >= 12) {
            throw new IllegalArgumentException("Semitone must be between 0 and 11 (12 chromatic semitones per octave)");
        }

        if (position < 0) {
            throw new IllegalArgumentException("Position must be non-negative");
        }

        if (length < 1) {
            throw new IllegalArgumentException("Length must be at least 1");
        }

        this.octave = octave;
        this.semitone = semitone;
        this.position = position;
        this.length = length;
        this.instrument = Objects.requireNonNull(instrument, "Instrument cannot be null");
    }

    public int getOctave() {
        return octave;
    }

    /**
     * Gets the semitone value (0-11).
     * 0=C, 1=C#, 2=D, 3=D#, 4=E, 5=F, 6=F#, 7=G, 8=G#, 9=A, 10=A#, 11=B
     */
    public int getSemitone() {
        return semitone;
    }

    /**
     * Gets the note name (C, C#, D, D#, E, F, F#, G, G#, A, A#, B).
     */
    public String getNoteName() {
        return NOTE_NAMES[semitone];
    }

    /**
     * Gets the full note name including octave (e.g., "C2", "A#1", "D#0").
     */
    public String getFullNoteName() {
        return NOTE_NAMES[semitone] + octave;
    }

    public int getPosition() {
        return position;
    }

    public int getLength() {
        return length;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    /**
     * Returns the end position of this note (exclusive).
     */
    public int getEndPosition() {
        return position + length;
    }

    /**
     * Checks if this note overlaps with another note in the same row (octave+semitone).
     */
    public boolean overlaps(Note other) {
        if (this.octave != other.octave || this.semitone != other.semitone || this.instrument != other.instrument) {
            return false;
        }
        return this.position < other.getEndPosition() && other.position < this.getEndPosition();
    }

    /**
     * Gets the frequency of this note in Hz.
     * Uses equal temperament tuning (A4 = 440 Hz).
     * Formula: f = 440 * 2^((n-69)/12) where n is MIDI note number
     *
     * Octave mapping:
     * - Octave 0 -> MIDI octave 3 (C3 = MIDI 48)
     * - Octave 1 -> MIDI octave 4 (C4 = MIDI 60, A4 = 440Hz) [CENTER]
     * - Octave 2 -> MIDI octave 5 (C5 = MIDI 72)
     */
    public double getFrequency() {
        // Calculate MIDI note number with remapped octaves
        // Octave 0 maps to MIDI octave 3, octave 1 to MIDI octave 4, octave 2 to MIDI octave 5
        int midiNoteNumber = (octave + 3) * 12 + semitone;

        // Calculate frequency using equal temperament
        // A4 (MIDI 69) = 440 Hz
        return 440.0 * Math.pow(2.0, (midiNoteNumber - 69) / 12.0);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note note = (Note) o;
        return octave == note.octave &&
               semitone == note.semitone &&
               position == note.position &&
               length == note.length &&
               instrument == note.instrument;
    }

    @Override
    public int hashCode() {
        return Objects.hash(octave, semitone, position, length, instrument);
    }

    @Override
    public String toString() {
        return "Note{" +
               "note=" + getFullNoteName() +
               ", position=" + position +
               ", length=" + length +
               ", instrument=" + instrument +
               ", freq=" + String.format("%.2f", getFrequency()) + "Hz" +
               '}';
    }
}
