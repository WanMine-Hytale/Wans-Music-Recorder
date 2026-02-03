package net.wanmine.musicrecorder.music;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.util.Objects;

public class Note {
    private int octave;
    private int semitone;
    private int position;
    private int length;
    private Instrument instrument;

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

    public Note(int octave, int semitone, int position, int length, Instrument instrument) {
        this.octave = octave;
        this.semitone = semitone;
        this.position = position;
        this.length = length;
        this.instrument = Objects.requireNonNull(instrument, "Instrument cannot be null");
    }

    public int getOctave() {
        return octave;
    }

    public int getSemitone() {
        return semitone;
    }

    public String getNoteName() {
        return NOTE_NAMES[semitone].replace("#", "s");
    }

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

    public int getEndPosition() {
        return position + length;
    }

    public boolean overlaps(Note other) {
        if (this.octave != other.octave || this.semitone != other.semitone || this.instrument != other.instrument) {
            return false;
        }
        return this.position < other.getEndPosition() && other.position < this.getEndPosition();
    }

    public double getFrequency() {
        int midiNoteNumber = (octave + 3) * 12 + semitone;

        return this.instrument.getBaseFrequency() * Math.pow(2.0, (midiNoteNumber - 69) / 12.0);
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
