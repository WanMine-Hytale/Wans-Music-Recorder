package net.wanmine.musicrecorder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.common.semver.Semver;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.common.CommonAssetModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import net.wanmine.musicrecorder.blocks.PlayerBlockComponent;
import net.wanmine.musicrecorder.blocks.RecorderBlockComponent;
import net.wanmine.musicrecorder.utils.FileTypeAdapter;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import ws.schild.jave.Version;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class WansMusicRecorderPlugin extends JavaPlugin {
    private static WansMusicRecorderPlugin instance;
    public static final String RUNTIME_PACK_NAME = "WansMusicRecorderRuntime";

    private final Gson GSON;
    private CommonAssetModule commonAssetModule;

    private Path runtimeAssetsPath;
    private Path songsPath;
    private Path songsEventPath;

    private ComponentType<ChunkStore, RecorderBlockComponent> recorderBlockType;
    private ComponentType<ChunkStore, PlayerBlockComponent> playerBlockType;

    public WansMusicRecorderPlugin(@NonNullDecl JavaPluginInit init) {
        super(init);

        instance = this;

        this.GSON = new GsonBuilder().registerTypeAdapter(File.class, new FileTypeAdapter()).setPrettyPrinting().create();
    }

    @Override
    protected void setup() {
        this.downloadFFMPeg();

        this.runtimeAssetsPath = this.getDataDirectory().getParent().resolve(RUNTIME_PACK_NAME);

        this.songsPath = this.runtimeAssetsPath.resolve("Common/Sounds");

        if (!this.songsPath.toFile().exists()) {
            this.songsPath.toFile().mkdirs();
        }

        this.songsEventPath = this.runtimeAssetsPath.resolve("Server/Audio/SoundEvents");

        if (!this.songsEventPath.toFile().exists()) {
            this.songsEventPath.toFile().mkdirs();
        }

        this.commonAssetModule = CommonAssetModule.get();

        this.registerRuntimePack();

        this.recorderBlockType = this.getChunkStoreRegistry().registerComponent(RecorderBlockComponent.class, "WansMusicRecorderRecorder", RecorderBlockComponent.CODEC);
        this.getChunkStoreRegistry().registerSystem(new RecorderBlockComponent.RecorderRefSystem());

        this.playerBlockType = this.getChunkStoreRegistry().registerComponent(PlayerBlockComponent.class, "WansMusicRecorderPlayer", PlayerBlockComponent.CODEC);
        this.getChunkStoreRegistry().registerSystem(new PlayerBlockComponent.PlayerRefSystem());

        this.getCodecRegistry(Interaction.CODEC).register("OpenRecorder", RecorderBlockComponent.OpenRecorderInteraction.class, RecorderBlockComponent.OpenRecorderInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("DiskRecorder", RecorderBlockComponent.DiskRecorderInteraction.class, RecorderBlockComponent.DiskRecorderInteraction.CODEC);

        this.getCodecRegistry(Interaction.CODEC).register("DiskPlayer", PlayerBlockComponent.DiskPlayerInteraction.class, PlayerBlockComponent.DiskPlayerInteraction.CODEC);
    }

    private void registerRuntimePack() {
        try {
            PluginManifest manifest = PluginManifest.CoreBuilder.corePlugin(WansMusicRecorderPlugin.class)
                    .description("Runtime assets for WansMusicRecorder")
                    .build();
            manifest.setName(RUNTIME_PACK_NAME);
            manifest.setVersion(Semver.fromString("1.0.0"));

            this.getLogger().at(Level.INFO).log("Registering runtime asset pack at: %s", runtimeAssetsPath);
            AssetModule.get().registerPack(RUNTIME_PACK_NAME, runtimeAssetsPath, manifest);

        } catch (Exception e) {
            this.getLogger().at(Level.SEVERE).withCause(e).log("Failed to register runtime asset pack");
        }
    }

    private void downloadFFMPeg() {
        Path destinationFolder = this.getDataDirectory().resolve("FFMPeg");

        destinationFolder.toFile().mkdirs();

        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch");

        boolean isWindows = os.contains("windows");
        boolean isMac = os.contains("mac");

        String artifactId;

        if (os.contains("win")) {
            artifactId = "jave-nativebin-win64";
        } else if (os.contains("linux")) {
            if (arch.equals("aarch64") || arch.contains("arm64")) {
                artifactId = "jave-nativebin-linux-arm64";
            } else {
                artifactId = "jave-nativebin-linux64";
            }
        } else if (os.contains("mac")) {
            if (arch.contains("aarch64") || arch.contains("arm64")) {
                artifactId = "jave-nativebin-osxm1";
            } else {
                artifactId = "jave-nativebin-osx64";
            }
        } else {
            this.getLogger().at(Level.SEVERE).log("Unsupported OS: %s", os);

            return;
        }

        String suffix = isWindows ? ".exe" : (isMac ? "-osx" : "");
        String version = Version.getVersion();

        File ffmpegFile = new File(destinationFolder.toFile(), "ffmpeg-" + arch + "-" + Version.getVersion() + suffix);

        if (ffmpegFile.exists()) {
            return;
        }

        String downloadUrl = String.format("https://repo1.maven.org/maven2/ws/schild/%s/%s/%s-%s.jar", artifactId, version, artifactId, version);
        String internalPath = String.format("ws/schild/jave/nativebin/ffmpeg-%s%s", arch, suffix);

        this.getLogger().at(Level.INFO).log("Downloading ffmpeg binary from %s to %s, internal path: %s", downloadUrl, ffmpegFile.getAbsolutePath(), internalPath);

        URL url;

        try {
            url = new URI(downloadUrl).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            this.getLogger().at(Level.SEVERE).withCause(e).log("Failed to create URL for ffmpeg download");

            return;
        }

        try (InputStream is = url.openStream(); ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(internalPath)) {
                    Files.copy(zis, ffmpegFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    zis.closeEntry();

                    ffmpegFile.setExecutable(true);

                    return;
                }

                zis.closeEntry();
            }

            this.getLogger().at(Level.SEVERE).log("No FFMPeg found!");
        } catch (IOException e) {
            this.getLogger().at(Level.SEVERE).withCause(e).log("Failed to download ffmpeg binary!");
        }
    }

    public CommonAssetModule getCommonAssetModule() {
        return commonAssetModule;
    }

    public Path getSongsPath() {
        return songsPath;
    }

    public Path getSongsEventPath() {
        return songsEventPath;
    }

    public ComponentType<ChunkStore, RecorderBlockComponent> getRecorderBlockType() {
        return recorderBlockType;
    }

    public ComponentType<ChunkStore, PlayerBlockComponent> getPlayerBlockType() {
        return playerBlockType;
    }

    public Gson getGson() {
        return GSON;
    }

    public static WansMusicRecorderPlugin getInstance() {
        return instance;
    }
}