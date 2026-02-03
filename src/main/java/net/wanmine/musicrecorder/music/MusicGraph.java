package net.wanmine.musicrecorder.music;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import java.util.*;

public class MusicGraph {
    private final List<Note> notes;
    private int tempo;
    private int maxOctaves;
    private int gridLength;

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

    public MusicGraph() {
        this(3, 120, 32);
    }

    public MusicGraph(int maxOctaves, int tempo, int gridLength) {
        this.notes = new ArrayList<>();
        this.maxOctaves = maxOctaves;
        this.tempo = tempo;
        this.gridLength = gridLength;
    }

    public MusicGraph(MusicGraph other) {
        this.notes = new ArrayList<>(other.notes);
        this.tempo = other.tempo;
        this.maxOctaves = other.maxOctaves;
        this.gridLength = other.gridLength;
    }

    public boolean addNote(Note note) {
        if (note.getOctave() >= maxOctaves) {
            return false;
        }

        if (note.getEndPosition() > gridLength) {
            return false;
        }

        for (Note existing : notes) {
            if (existing.overlaps(note)) {
                return false;
            }
        }

        notes.add(note);

        return true;
    }

    public boolean removeNote(Note note) {
        return notes.remove(note);
    }

    public boolean removeNoteAt(int position, int octave, int semitone, Instrument instrument) {
        return notes.removeIf(note ->
            note.getPosition() == position &&
            note.getOctave() == octave &&
            note.getSemitone() == semitone &&
            note.getInstrument() == instrument
        );
    }

    public void clear() {
        notes.clear();
    }

    public List<Note> getNotes() {
        return Collections.unmodifiableList(notes);
    }

    public List<Note> getNotesForInstrument(Instrument instrument) {
        return notes.stream()
                .filter(note -> note.getInstrument() == instrument)
                .toList();
    }

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

        notes.removeIf(note -> note.getEndPosition() > gridLength);
    }

    public double getGridSpaceDuration() {
        double beatsPerSecond = tempo / 60.0;
        double sixteenthNotesPerSecond = beatsPerSecond * 4;

        return 1.0 / sixteenthNotesPerSecond;
    }

    public double getTotalDuration() {
        int lastPosition = notes.stream().mapToInt(Note::getEndPosition).max().orElse(gridLength);

        return lastPosition * getGridSpaceDuration();
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
