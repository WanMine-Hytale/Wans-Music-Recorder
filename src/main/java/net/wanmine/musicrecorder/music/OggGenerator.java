package net.wanmine.musicrecorder.music;

import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;

public class OggGenerator {
    private static final int SAMPLE_RATE = 44100;
    private static final int BITS_PER_SAMPLE = 16;
    private static final int CHANNELS = 1;

    private OggGenerator() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void generateOgg(MusicGraph graph, Path outputFilePath, String fileName) throws IOException, EncoderException {
        byte[] audioData = generateAudioData(graph);

        File wavFile = outputFilePath.resolve(fileName + ".wav").toFile();
        File outputFile = outputFilePath.resolve(fileName + ".ogg").toFile();

        writeWavFile(audioData, wavFile);
        writeOggFile(wavFile, outputFile);
    }

    private static byte[] generateAudioData(MusicGraph graph) {
        double duration = graph.getTotalDuration();
        int totalSamples = (int) (duration * SAMPLE_RATE);
        short[] samples = new short[totalSamples];

        for (Note note : graph.getNotes()) {
            generateNoteAudio(note, samples, graph);
        }

        ByteBuffer buffer = ByteBuffer.allocate(samples.length * 2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (short sample : samples) {
            buffer.putShort(sample);
        }
        return buffer.array();
    }

    private static void generateNoteAudio(Note note, short[] samples, MusicGraph graph) {
        double gridSpaceDuration = graph.getGridSpaceDuration();
        int startSample = (int) (note.getPosition() * gridSpaceDuration * SAMPLE_RATE);
        int endSample = (int) (note.getEndPosition() * gridSpaceDuration * SAMPLE_RATE);

        double frequency = note.getFrequency();
        Instrument.WaveformType waveform = note.getInstrument().getWaveformType();

        for (int i = startSample; i < endSample && i < samples.length; i++) {
            double time = (double) (i - startSample) / SAMPLE_RATE;
            double noteDuration = (endSample - startSample) / (double) SAMPLE_RATE;
            double amplitude = calculateEnvelope(time, noteDuration);
            double sample = generateWaveform(waveform, frequency, time) * amplitude;

            samples[i] = (short) Math.clamp(samples[i] + (int) (sample * 16384), Short.MIN_VALUE, Short.MAX_VALUE);
        }
    }

    private static double generateWaveform(Instrument.WaveformType type, double frequency, double time) {
        double phase = 2 * Math.PI * frequency * time;

        return switch (type) {
            case SINE -> Math.sin(phase);

            case SQUARE -> Math.sin(phase) >= 0 ? 1.0 : -1.0;

            case TRIANGLE -> {
                double normalizedPhase = (phase % (2 * Math.PI)) / (2 * Math.PI);
                if (normalizedPhase < 0.5) {
                    yield 4 * normalizedPhase - 1;
                } else {
                    yield -4 * normalizedPhase + 3;
                }
            }

            case SAWTOOTH -> {
                double normalizedPhase = (phase % (2 * Math.PI)) / (2 * Math.PI);
                yield 2 * normalizedPhase - 1;
            }

            case NOISE -> Math.random() * 2 - 1; // White noise for drums
        };
    }

    private static double calculateEnvelope(double time, double duration) {
        double attack = Math.min(0.01, duration * 0.1);
        double decay = Math.min(0.05, duration * 0.2);
        double sustainLevel = 0.7;
        double release = Math.min(0.1, duration * 0.3);

        if (time < attack) {
            return time / attack;
        } else if (time < attack + decay) {
            double decayProgress = (time - attack) / decay;
            return 1.0 - (1.0 - sustainLevel) * decayProgress;
        } else if (time < duration - release) {
            return sustainLevel;
        } else {
            double releaseProgress = (time - (duration - release)) / release;
            return sustainLevel * (1.0 - releaseProgress);
        }
    }

    private static void writeWavFile(byte[] audioData, File outputFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            int dataSize = audioData.length;
            int fileSize = 36 + dataSize;

            baos.write("RIFF".getBytes());
            baos.write(intToBytes(fileSize));
            baos.write("WAVE".getBytes());

            baos.write("fmt ".getBytes());
            baos.write(intToBytes(16));
            baos.write(shortToBytes((short) 1));
            baos.write(shortToBytes((short) CHANNELS));
            baos.write(intToBytes(SAMPLE_RATE));
            baos.write(intToBytes(SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8));
            baos.write(shortToBytes((short) (CHANNELS * BITS_PER_SAMPLE / 8)));
            baos.write(shortToBytes((short) BITS_PER_SAMPLE));

            baos.write("data".getBytes());
            baos.write(intToBytes(dataSize));
            baos.write(audioData);

            fos.write(baos.toByteArray());
        }
    }

    private static void writeOggFile(File wavFile, File outputFile) throws EncoderException {
        AudioAttributes audio = new AudioAttributes();
        audio.setCodec("libvorbis");
        audio.setBitRate(128000);    // 128 kbps
        audio.setChannels(CHANNELS);
        audio.setSamplingRate(SAMPLE_RATE);

        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setOutputFormat("ogg");
        attrs.setAudioAttributes(audio);

        Encoder encoder = new Encoder();
        encoder.encode(new MultimediaObject(wavFile), outputFile, attrs);

        wavFile.delete();
    }

    private static byte[] intToBytes(int value) {
        return new byte[] {
            (byte) (value & 0xFF),
            (byte) ((value >> 8) & 0xFF),
            (byte) ((value >> 16) & 0xFF),
            (byte) ((value >> 24) & 0xFF)
        };
    }

    private static byte[] shortToBytes(short value) {
        return new byte[] {
            (byte) (value & 0xFF),
            (byte) ((value >> 8) & 0xFF)
        };
    }
}
