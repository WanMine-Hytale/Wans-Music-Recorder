package net.wanmine.musicrecorder.music;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import java.util.*;

/**
 * Represents a music graph with notes organized in a grid.
 * The graph has a maximum of 3 octaves (customizable).
 * Each octave contains 12 chromatic semitones (C, C#, D, D#, E, F, F#, G, G#, A, A#, B).
 * <p>
 * Grid visualization:
 * Each row represents a semitone within an octave.
 * 36 total rows (3 octaves × 12 semitones per octave)
 */
public class MusicGraph {
    private final List<Note> notes;
    private int tempo; // BPM (beats per minute)
    private int maxOctaves;
    private int gridLength; // Total length of the grid

    public static final BuilderCodec<MusicGraph> CODEC = BuilderCodec.builder(
                    MusicGraph.class,
                    MusicGraph::new
            )
            .append(new KeyedCodec<>("MaxOctaves", Codec.INTEGER), (graph, value) -> graph.maxOctaves = value, graph -> graph.maxOctaves)
            .add()
            .append(new KeyedCodec<>("Tempo", Codec.INTEGER), (graph, value) -> graph.tempo = value, graph -> graph.tempo)
            .add()
            .append(new KeyedCodec<>("GridLength", Codec.INTEGER), (graph, value) -> graph.gridLength = value, graph -> graph.gridLength)
            .add()
            .append(new KeyedCodec<>("Notes", new ArrayCodec<>(Note.CODEC, Note[]::new)), (graph, value) -> {
                graph.notes.clear();

                Collections.addAll(graph.notes, value);
            }, graph -> graph.notes.toArray(new Note[0]))
            .add()
            .build();

    /**
     * Creates a new MusicGraph with default 3 octaves.
     */
    public MusicGraph() {
        this(3, 120, 32); // Default: 3 octaves, 120 BPM, 32 grid spaces
    }

    /**
     * Creates a new MusicGraph with specified parameters.
     *
     * @param maxOctaves Maximum number of octaves (default 3)
     * @param tempo Tempo in BPM
     * @param gridLength Length of the grid
     */
    public MusicGraph(int maxOctaves, int tempo, int gridLength) {
        if (maxOctaves < 1) {
            throw new IllegalArgumentException("maxOctaves must be at least 1");
        }
        if (tempo < 20 || tempo > 300) {
            throw new IllegalArgumentException("Tempo must be between 20 and 300 BPM");
        }
        if (gridLength < 1) {
            throw new IllegalArgumentException("gridLength must be at least 1");
        }
        this.notes = new ArrayList<>();
        this.maxOctaves = maxOctaves;
        this.tempo = tempo;
        this.gridLength = gridLength;
    }

    /**
     * Copy constructor for cloning.
     */
    public MusicGraph(MusicGraph other) {
        this.notes = new ArrayList<>(other.notes);
        this.tempo = other.tempo;
        this.maxOctaves = other.maxOctaves;
        this.gridLength = other.gridLength;
    }

    /**
     * Adds a note to the music graph.
     * Validates that the note doesn't exceed the max octaves and grid length.
     *
     * @param note The note to add
     * @return true if added successfully, false otherwise
     */
    public boolean addNote(Note note) {
        if (note.getOctave() >= maxOctaves) {
            return false;
        }

        if (note.getEndPosition() > gridLength) {
            return false;
        }

        // Check for overlaps with existing notes
        for (Note existing : notes) {
            if (existing.overlaps(note)) {
                return false;
            }
        }

        notes.add(note);

        return true;
    }

    /**
     * Removes a note from the music graph.
     */
    public boolean removeNote(Note note) {
        return notes.remove(note);
    }

    /**
     * Removes a note at a specific position, octave, semitone, and instrument.
     */
    public boolean removeNoteAt(int position, int octave, int semitone, Instrument instrument) {
        return notes.removeIf(note ->
            note.getPosition() == position &&
            note.getOctave() == octave &&
            note.getSemitone() == semitone &&
            note.getInstrument() == instrument
        );
    }

    /**
     * Clears all notes from the music graph.
     */
    public void clear() {
        notes.clear();
    }

    /**
     * Gets all notes in the music graph.
     */
    public List<Note> getNotes() {
        return Collections.unmodifiableList(notes);
    }

    /**
     * Gets notes for a specific instrument.
     */
    public List<Note> getNotesForInstrument(Instrument instrument) {
        return notes.stream()
                .filter(note -> note.getInstrument() == instrument)
                .toList();
    }

    /**
     * Gets notes at a specific position.
     */
    public List<Note> getNotesAtPosition(int position) {
        return notes.stream()
                .filter(note -> note.getPosition() <= position && position < note.getEndPosition())
                .toList();
    }

    public List<Note> getNotesAtPositionIgnoreLength(int position) {
        return notes.stream()
                .filter(note -> note.getPosition() == position)
                .toList();
    }

    public boolean hasNotesAfter(int position) {
        return !notes.stream()
                .filter(note -> position < note.getEndPosition())
                .toList().isEmpty();
    }

    /**
     * Gets the total number of rows in the grid (octaves × 12 semitones per octave).
     */
    public int getTotalRows() {
        return maxOctaves * 12;
    }

    public int getTempo() {
        return tempo;
    }

    public void setTempo(int tempo) {
        if (tempo < 20 || tempo > 300) {
            throw new IllegalArgumentException("Tempo must be between 20 and 300 BPM");
        }
        this.tempo = tempo;
    }

    public int getMaxOctaves() {
        return maxOctaves;
    }

    public int getGridLength() {
        return gridLength;
    }

    public void setGridLength(int gridLength) {
        if (gridLength < 1) {
            throw new IllegalArgumentException("gridLength must be at least 1");
        }
        this.gridLength = gridLength;
        // Remove notes that exceed the new grid length
        notes.removeIf(note -> note.getEndPosition() > gridLength);
    }

    /**
     * Calculates the duration of one grid space in seconds based on tempo.
     */
    public double getGridSpaceDuration() {
        // Assuming each grid space is a 16th note
        double beatsPerSecond = tempo / 60.0;
        double sixteenthNotesPerSecond = beatsPerSecond * 4; // 4 sixteenth notes per beat
        return 1.0 / sixteenthNotesPerSecond;
    }

    /**
     * Gets the total duration of the music in seconds.
     */
    public double getTotalDuration() {
        int lastPosition = notes.stream().mapToInt(Note::getEndPosition).max().orElse(gridLength);

        return lastPosition * getGridSpaceDuration();
    }

    /**
     * Creates a visual representation of the music graph for a specific instrument.
     * Shows all semitones (12 per octave) from highest to lowest.
     *
     * Example output for 3 octaves (36 rows total):
     * Octave 2 (highest):
     * B2:  ---X--X--X
     * A#2: -X-X--XX--
     * A2:  ---X---X--
     * G#2: ----------
     * G2:  X---------
     * F#2: ----------
     * F2:  --X-------
     * E2:  ----------
     * D#2: ----------
     * D2:  ----X-----
     * C#2: ----------
     * C2:  ----------
     * ---
     * Octave 1 (middle):
     * ...
     * Octave 0 (lowest):
     * ...
     */
    public String toVisualString(Instrument instrument) {
        StringBuilder sb = new StringBuilder();

        // Note names for display
        String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

        // Iterate from highest octave to lowest
        for (int octave = maxOctaves - 1; octave >= 0; octave--) {
            // Iterate from highest semitone (11 = B) to lowest (0 = C) within octave
            for (int semitone = 11; semitone >= 0; semitone--) {
                // Add note name label
                String noteName = noteNames[semitone];
                sb.append(String.format("%-4s", noteName + octave + ":")); // Fixed width for alignment

                // Build the row
                for (int pos = 0; pos < gridLength; pos++) {
                    boolean hasNote = false;
                    for (Note note : notes) {
                        if (note.getInstrument() == instrument &&
                            note.getOctave() == octave &&
                            note.getSemitone() == semitone &&
                            note.getPosition() <= pos &&
                            pos < note.getEndPosition()) {
                            hasNote = true;
                            break;
                        }
                    }
                    sb.append(hasNote ? 'X' : '-');
                }
                sb.append('\n');
            }

            // Add separator between octaves
            if (octave > 0) {
                sb.append("---\n");
            }
        }
        return sb.toString();
    }

    /**
     * Creates a compact visual representation without labels.
     * Shows all rows from highest to lowest.
     * 36 rows for 3 octaves (12 semitones each)
     */
    public String toCompactVisualString(Instrument instrument) {
        StringBuilder sb = new StringBuilder();

        for (int octave = maxOctaves - 1; octave >= 0; octave--) {
            for (int semitone = 11; semitone >= 0; semitone--) {
                for (int pos = 0; pos < gridLength; pos++) {
                    boolean hasNote = false;
                    for (Note note : notes) {
                        if (note.getInstrument() == instrument &&
                            note.getOctave() == octave &&
                            note.getSemitone() == semitone &&
                            note.getPosition() <= pos &&
                            pos < note.getEndPosition()) {
                            hasNote = true;
                            break;
                        }
                    }
                    sb.append(hasNote ? 'X' : '-');
                }
                if (octave > 0 || semitone > 0) {
                    sb.append('\n');
                }
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "MusicGraph{" +
               "notes=" + notes.size() +
               ", tempo=" + tempo +
               ", maxOctaves=" + maxOctaves +
               ", totalRows=" + getTotalRows() +
               " (12 semitones × " + maxOctaves + " octaves)" +
               ", gridLength=" + gridLength +
               '}';
    }

    @Override
    public MusicGraph clone() {
        return new MusicGraph(this);
    }
}
