package net.wanmine.musicrecorder.music;

import com.hypixel.hytale.logger.HytaleLogger;
import net.wanmine.musicrecorder.WansMusicRecorderPlugin;
import ws.schild.jave.Version;
import ws.schild.jave.process.ProcessLocator;
import ws.schild.jave.process.ProcessWrapper;
import ws.schild.jave.process.ffmpeg.FFMPEGProcess;

import java.io.File;

public class FFMPegLocator implements ProcessLocator {
    private final String path;

    public FFMPegLocator() {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("windows");
        boolean isMac = os.contains("mac");

        HytaleLogger logger = WansMusicRecorderPlugin.getInstance().getLogger();

        File dirFolder = new File(WansMusicRecorderPlugin.getInstance().getDataDirectory().toFile(), "FFMPeg/");

        if (!dirFolder.exists()) {
            path = null;

            return;
        }

        String suffix = isWindows ? ".exe" : (isMac ? "-osx" : "");
        String arch = System.getProperty("os.arch");

        File ffmpegFile = new File(dirFolder, "ffmpeg-" + arch + "-" + Version.getVersion() + suffix);

        if (!ffmpegFile.exists()) {
            path = null;

            return;
        }

        path = ffmpegFile.getAbsolutePath();
    }

    @Override
    public String getExecutablePath() {
        return path;
    }

    @Override
    public ProcessWrapper createExecutor() {
        return new FFMPEGProcess(getExecutablePath());
    }
}
