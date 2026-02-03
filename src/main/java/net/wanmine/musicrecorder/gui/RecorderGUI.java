package net.wanmine.musicrecorder.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.wanmine.musicrecorder.blocks.RecorderBlockComponent;
import net.wanmine.musicrecorder.music.Instrument;
import net.wanmine.musicrecorder.music.Note;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.ArrayList;
import java.util.List;

public class RecorderGUI extends InteractiveCustomUIPage<RecorderGUI.RecorderGUIEventData> {
    private final RecorderBlockComponent recorderBlock;

    private Instrument currentInstrument;
    private Note currentNote;
    private int currentPage;
    private final World world;

    public RecorderGUI(@NonNullDecl PlayerRef playerRef, RecorderBlockComponent recorderBlock, World world) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, RecorderGUI.RecorderGUIEventData.CODEC);

        this.recorderBlock = recorderBlock;
        this.currentInstrument = Instrument.PIANO;
        this.currentNote = new Note(0, 0, 0, 1, this.currentInstrument);
        this.currentPage = 0;
        this.world = world;
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder commandBuilder, @NonNullDecl UIEventBuilder eventBuilder, @NonNullDecl Store<EntityStore> store) {
        if (this.recorderBlock.isAnglo()) {
            commandBuilder.append("Pages/WansMusicRecorder/Recorder.ui");
        } else {
            commandBuilder.append("Pages/WansMusicRecorder/FrenchRecorder.ui");
        }

        this.buildNoteGrid(commandBuilder, eventBuilder);
        this.updatePage(commandBuilder, eventBuilder);
        this.updateSongName(commandBuilder, eventBuilder);
        this.updateTempo(commandBuilder, eventBuilder);
        this.updateNoteLength(commandBuilder, eventBuilder);
        this.buildDropDown(commandBuilder, eventBuilder);
        this.updateAnglo(commandBuilder, eventBuilder);

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#ClearBtn",
                new EventData()
                        .append("ClickType", "ClearNotes"),
                false);

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#PlayBtn",
                new EventData()
                        .append("ClickType", "PlaySong"),
                false);

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#RecordBtn",
                new EventData()
                        .append("ClickType", "RecordSong"),
                false);
    }

    private void updateAnglo(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder) {
        commandBuilder.set("#AngToggle.Value", this.recorderBlock.isAnglo());

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged, "#AngToggle",
                new EventData()
                        .append("@AngToggle", "#AngToggle.Value")
                        .append("ClickType", "ChangeAng"),
                false);
    }

    private void buildDropDown(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder) {
        List<DropdownEntryInfo> instruments = new ArrayList<>();

        for (Instrument instrument : Instrument.values()) {
            instruments.add(new DropdownEntryInfo(LocalizableString.fromString(instrument.getDisplayName()), instrument.name()));
        }

        commandBuilder.set("#InstrumentDropdown.Entries", instruments);
        commandBuilder.set("#InstrumentDropdown.Value", this.currentInstrument.name());

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged, "#InstrumentDropdown",
                new EventData()
                        .append("@InstrumentDropdown", "#InstrumentDropdown.Value")
                        .append("ClickType", "UpdateInstrument"),
                false);
    }

    private void buildNoteGrid(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder) {
        for (int position = 0; position < 26; position++) {
            List<Note> notesAtPosition = recorderBlock.getMusicGraph().getNotesAtPosition(position + (26 * this.currentPage));

            for (int octave = 0; octave < 3; octave++) {
                for (int semitone = 0; semitone < 12; semitone++) {
                    Note selectedNote = null;

                    for (Note note : notesAtPosition) {
                        if (note.getOctave() == octave && note.getSemitone() == semitone && note.getInstrument() == this.currentInstrument) {
                            selectedNote = note;

                            break;
                        }
                    }

                    String selector = "#Octave" + (octave + 1) + "Line" + (semitone + 1);

                    if (position == 0) {
                        commandBuilder.clear(selector);
                    }

                    if (selectedNote != null) {
                        if (selectedNote.getLength() == 1) {
                            commandBuilder.append(selector, "Pages/WansMusicRecorder/Buttons/Enabled" + selectedNote.getNoteName() + "NoteButton.ui");

                            this.generateButtonHandlers(eventBuilder, selector, octave, semitone, position, 0, true, true);
                        } else {
                            if (position + (26 * this.currentPage) == selectedNote.getPosition()) {
                                commandBuilder.append(selector, "Pages/WansMusicRecorder/Buttons/Enabled" + selectedNote.getNoteName() + "StartNoteButton.ui");
                            } else if (position + (26 * this.currentPage) < selectedNote.getEndPosition() - 1) {
                                commandBuilder.append(selector, "Pages/WansMusicRecorder/Buttons/Enabled" + selectedNote.getNoteName() + "MiddleNoteButton.ui");
                            } else if (position + (26 * this.currentPage) == selectedNote.getEndPosition() - 1) {
                                commandBuilder.append(selector, "Pages/WansMusicRecorder/Buttons/Enabled" + selectedNote.getNoteName() + "EndNoteButton.ui");
                            }

                            this.generateButtonHandlers(eventBuilder, selector, octave, semitone, selectedNote.getPosition(), position - selectedNote.getPosition(), true, true);
                        }
                    } else {
                        commandBuilder.append(selector, "Pages/WansMusicRecorder/Buttons/DisabledNoteButton.ui");

                        this.generateButtonHandlers(eventBuilder, selector, octave, semitone, position, 0, false, false);
                    }
                }
            }
        }
    }

    private void updateSongName(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder) {
        commandBuilder.set("#SongNameField.Value", this.recorderBlock.getSongName());

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged, "#SongNameField",
                new EventData()
                        .append("@SongNameField", "#SongNameField.Value")
                        .append("ClickType", "UpdateSongName"),
                false);
    }

    private void updateTempo(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder) {
        commandBuilder.set("#TempoField.Value", this.recorderBlock.getMusicGraph().getTempo());

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged, "#TempoField",
                new EventData()
                        .append("@TempoField", "#TempoField.Value")
                        .append("ClickType", "UpdateTempo"),
                false);

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#TempoUpBtn",
                new EventData()
                        .append("ClickType", "DownTempo"),
                false);

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#TempoDownBtn",
                new EventData()
                        .append("ClickType", "UpTempo"),
                false);
    }

    private void updatePage(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder) {
        commandBuilder.set("#PageField.Value", this.currentPage);

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged, "#PageField",
                new EventData()
                        .append("@PageField", "#PageField.Value")
                        .append("ClickType", "UpdatePage"),
                false);

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#PageUpBtn",
                new EventData()
                        .append("ClickType", "DownPage"),
                false);

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#PageDownBtn",
                new EventData()
                        .append("ClickType", "UpPage"),
                false);
    }

    private void updateNoteLength(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder) {
        commandBuilder.set("#NoteLengthField.Value", this.currentNote.getLength());

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged, "#NoteLengthField",
                new EventData()
                        .append("@NoteLengthField", "#NoteLengthField.Value")
                        .append("ClickType", "UpdateNoteLength"),
                false);

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#NoteLengthUpBtn",
                new EventData()
                        .append("ClickType", "DownNoteLength"),
                false);

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#NoteLengthDownBtn",
                new EventData()
                        .append("ClickType", "UpNoteLength"),
                false);
    }

    private void generateButtonHandlers(UIEventBuilder eventBuilder, String selector, int octave, int semitone, int position, int addI, boolean noteFound, boolean hasRClick) {
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, selector + "[" + (position + addI) + "] #NoteButton",
                new EventData()
                        .append("NotePosition", String.valueOf(position))
                        .append("NoteOctave", String.valueOf(octave))
                        .append("NoteSemitone", String.valueOf(semitone))
                        .append("NoteFound", String.valueOf(noteFound))
                        .append("ClickType", "L"),
                false);

        if (!hasRClick) {
            return;
        }

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.RightClicking, selector + "[" + (position + addI) + "] #NoteButton",
                new EventData()
                        .append("NotePosition", String.valueOf(position))
                        .append("NoteOctave", String.valueOf(octave))
                        .append("NoteSemitone", String.valueOf(semitone))
                        .append("NoteFound", String.valueOf(noteFound))
                        .append("ClickType", "R"),
                false);
    }

    @Override
    public void handleDataEvent(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, @NonNullDecl RecorderGUIEventData data) {
        UICommandBuilder commandBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();

        switch (data.getClickType()) {
            case "L" -> {
                if (this.recorderBlock.getMusicGraph().getGridLength() < 26 * (this.currentPage + 1)) {
                    this.recorderBlock.getMusicGraph().setGridLength(26 * (this.currentPage + 1));
                }

                if (data.isNoteFound()) {
                    this.recorderBlock.getMusicGraph().removeNoteAt(data.getNotePosition(), data.getNoteOctave(), data.getNoteSemitone(), this.currentInstrument);
                    this.currentNote = new Note(0, 0, 0, 1, this.currentInstrument);

                    if (!this.recorderBlock.getMusicGraph().hasNotesAfter((26 * this.currentPage) - 1)) {
                        this.recorderBlock.getMusicGraph().setGridLength(26 * this.currentPage);
                    }
                } else {
                    this.currentNote = new Note(data.getNoteOctave(), data.getNoteSemitone(), data.getNotePosition() + (26 * this.currentPage), 1, this.currentInstrument);
                    this.recorderBlock.getMusicGraph().addNote(this.currentNote);
                }

                this.buildNoteGrid(commandBuilder, eventBuilder);
                this.updateNoteLength(commandBuilder, eventBuilder);
            }
            case "R" -> {
                for (Note note : this.recorderBlock.getMusicGraph().getNotesAtPositionIgnoreLength(data.getNotePosition())) {
                    if (note.getInstrument() == this.currentInstrument && note.getOctave() == data.getNoteOctave() && note.getSemitone() == data.getNoteSemitone()) {
                        this.currentNote = note;

                        break;
                    }
                }

                this.updateNoteLength(commandBuilder, eventBuilder);
            }
            case "UpdateSongName" -> {
                this.recorderBlock.setSongName(data.getSongName());

                this.updateSongName(commandBuilder, eventBuilder);
            }
            case "UpdateTempo" -> {
                this.recorderBlock.getMusicGraph().setTempo(Math.clamp(data.getTempo(), 20, 300));

                this.updateTempo(commandBuilder, eventBuilder);
            }
            case "DownTempo" -> {
                this.recorderBlock.getMusicGraph().setTempo(Math.max(this.recorderBlock.getMusicGraph().getTempo() - 1, 20));

                this.updateTempo(commandBuilder, eventBuilder);
            }
            case "UpTempo" -> {
                this.recorderBlock.getMusicGraph().setTempo(Math.min(this.recorderBlock.getMusicGraph().getTempo() + 1, 300));

                this.updateTempo(commandBuilder, eventBuilder);
            }
            case "UpdatePage" -> {
                this.currentPage = Math.max(data.getPage(), 0);

                this.updatePage(commandBuilder, eventBuilder);
                this.buildNoteGrid(commandBuilder, eventBuilder);
            }
            case "DownPage" -> {
                this.currentPage = Math.max(this.currentPage - 1, 0);

                this.updatePage(commandBuilder, eventBuilder);
                this.buildNoteGrid(commandBuilder, eventBuilder);
            }
            case "UpPage" -> {
                this.currentPage += 1;

                this.updatePage(commandBuilder, eventBuilder);
                this.buildNoteGrid(commandBuilder, eventBuilder);
            }
            case "UpdateNoteLength" -> {
                this.recorderBlock.getMusicGraph().removeNote(this.currentNote);

                Note tempNote = new Note(this.currentNote.getOctave(), this.currentNote.getSemitone(), this.currentNote.getPosition(), Math.max(data.getNoteLength(), 1), this.currentInstrument);

                if (this.recorderBlock.getMusicGraph().addNote(tempNote)) {
                    this.currentNote = tempNote;
                } else {
                    this.recorderBlock.getMusicGraph().addNote(this.currentNote);
                }

                this.updateNoteLength(commandBuilder, eventBuilder);
                this.buildNoteGrid(commandBuilder, eventBuilder);
            }
            case "DownNoteLength" -> {
                this.recorderBlock.getMusicGraph().removeNote(this.currentNote);

                Note tempNote = new Note(this.currentNote.getOctave(), this.currentNote.getSemitone(), this.currentNote.getPosition(), Math.max(this.currentNote.getLength() - 1, 1), this.currentInstrument);

                if (this.recorderBlock.getMusicGraph().addNote(tempNote)) {
                    this.currentNote = tempNote;
                } else {
                    this.recorderBlock.getMusicGraph().addNote(this.currentNote);
                }

                this.updateNoteLength(commandBuilder, eventBuilder);
                this.buildNoteGrid(commandBuilder, eventBuilder);
            }
            case "UpNoteLength" -> {
                this.recorderBlock.getMusicGraph().removeNote(this.currentNote);

                Note tempNote = new Note(this.currentNote.getOctave(), this.currentNote.getSemitone(), this.currentNote.getPosition(), this.currentNote.getLength() + 1, this.currentInstrument);

                if (this.recorderBlock.getMusicGraph().addNote(tempNote)) {
                    this.currentNote = tempNote;
                } else {
                    this.recorderBlock.getMusicGraph().addNote(this.currentNote);
                }

                this.updateNoteLength(commandBuilder, eventBuilder);
                this.buildNoteGrid(commandBuilder, eventBuilder);
            }
            case "ClearNotes" -> {
                this.recorderBlock.getMusicGraph().clear();
                this.currentNote = new Note(0, 0, 0, 1, this.currentInstrument);

                this.updateNoteLength(commandBuilder, eventBuilder);
                this.buildNoteGrid(commandBuilder, eventBuilder);
            }
            case "PlaySong" -> this.recorderBlock.registerAndPlay(world.getEntityStore().getStore());
            case "RecordSong" -> {
                ItemStack stack = this.recorderBlock.getDiskContainer().getItemStack((short) 0);

                if (stack == null) {
                    return;
                }

                stack = stack.withMetadata("SavedSong", RecorderBlockComponent.DiskMetadata.CODEC, new RecorderBlockComponent.DiskMetadata(this.recorderBlock.getMusicGraph(), this.recorderBlock.getSongName()));

                this.recorderBlock.getDiskContainer().setItemStackForSlot((short) 0, stack);
            }
            case "ChangeAng" -> {
                this.recorderBlock.setAnglo(data.isAnglo());

                this.rebuild();

                return;
            }
            case "UpdateInstrument" -> {
                this.currentInstrument = Instrument.valueOf(data.getInstrument());

                this.buildDropDown(commandBuilder, eventBuilder);
                this.buildNoteGrid(commandBuilder, eventBuilder);
                this.updateNoteLength(commandBuilder, eventBuilder);
            }
        }

        this.sendUpdate(commandBuilder, eventBuilder, false);
    }

    public static class RecorderGUIEventData {
        private int notePosition;
        private int noteOctave;
        private int noteSemitone;
        private boolean noteFound;
        private String clickType;
        private String songName;
        private int tempo;
        private int noteLength;
        private int page;
        private String instrument;
        private boolean isAnglo;

        public static final BuilderCodec<RecorderGUI.RecorderGUIEventData> CODEC = BuilderCodec.builder(
                        RecorderGUI.RecorderGUIEventData.class, RecorderGUI.RecorderGUIEventData::new
                )
                .append(new KeyedCodec<>("NotePosition", Codec.STRING), (s, v) -> s.notePosition = Integer.parseInt(v), s -> String.valueOf(s.notePosition))
                .add()
                .append(new KeyedCodec<>("NoteOctave", Codec.STRING), (s, v) -> s.noteOctave = Integer.parseInt(v), s -> String.valueOf(s.noteOctave))
                .add()
                .append(new KeyedCodec<>("NoteSemitone", Codec.STRING), (s, v) -> s.noteSemitone = Integer.parseInt(v), s -> String.valueOf(s.noteSemitone))
                .add()
                .append(new KeyedCodec<>("NoteFound", Codec.STRING), (s, v) -> s.noteFound = Boolean.parseBoolean(v), s -> String.valueOf(s.noteFound))
                .add()
                .append(new KeyedCodec<>("ClickType", Codec.STRING), (s, v) -> s.clickType = v, s -> s.clickType)
                .add()
                .append(new KeyedCodec<>("@SongNameField", Codec.STRING), (data, s) -> data.songName = s, data -> data.songName)
                .add()
                .append(new KeyedCodec<>("@TempoField", Codec.INTEGER), (data, s) -> data.tempo = s, data -> data.tempo)
                .add()
                .append(new KeyedCodec<>("@NoteLengthField", Codec.INTEGER), (data, s) -> data.noteLength = s, data -> data.noteLength)
                .add()
                .append(new KeyedCodec<>("@PageField", Codec.INTEGER), (data, s) -> data.page = s, data -> data.page)
                .add()
                .append(new KeyedCodec<>("@InstrumentDropdown", Codec.STRING), (data, s) -> data.instrument = s == null ? "" : s, data -> data.instrument)
                .add()
                .append(new KeyedCodec<>("@AngToggle", Codec.BOOLEAN), (data, s) -> data.isAnglo = s, data -> data.isAnglo)
                .add()
                .build();

        private RecorderGUIEventData() {}

        public int getNotePosition() {
            return notePosition;
        }

        public int getNoteOctave() {
            return noteOctave;
        }

        public int getNoteSemitone() {
            return noteSemitone;
        }

        public boolean isNoteFound() {
            return noteFound;
        }

        public String getClickType() {
            return clickType;
        }

        public String getSongName() {
            return songName;
        }

        public int getTempo() {
            return tempo;
        }

        public int getNoteLength() {
            return noteLength;
        }

        public int getPage() {
            return page;
        }

        public String getInstrument() {
           return instrument;
        }

        public boolean isAnglo() {
           return isAnglo;
        }
    }
}
