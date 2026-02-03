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
import net.wanmine.musicrecorder.blocks.RecorderBlockComponent;
import net.wanmine.musicrecorder.utils.FileTypeAdapter;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.io.File;
import java.nio.file.Path;
import java.util.logging.Level;

public class WansMusicRecorderPlugin extends JavaPlugin {
    private static WansMusicRecorderPlugin instance;
    public static final String RUNTIME_PACK_NAME = "WansMusicRecorderRuntime";

    private final Gson GSON;
    private CommonAssetModule commonAssetModule;

    private Path runtimeAssetsPath;
    private Path songsPath;
    private Path songsEventPath;

    private ComponentType<ChunkStore, RecorderBlockComponent> recorderBlockType;

    public WansMusicRecorderPlugin(@NonNullDecl JavaPluginInit init) {
        super(init);

        instance = this;

        this.GSON = new GsonBuilder().registerTypeAdapter(File.class, new FileTypeAdapter()).setPrettyPrinting().create();
    }

    @Override
    protected void setup() {
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

        this.getCodecRegistry(Interaction.CODEC).register("OpenRecorder", RecorderBlockComponent.OpenRecorderInteraction.class, RecorderBlockComponent.OpenRecorderInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("DiskRecorder", RecorderBlockComponent.DiskRecorderInteraction.class, RecorderBlockComponent.DiskRecorderInteraction.CODEC);
    }

    private void registerRuntimePack() {
        try {
            // Create minimal manifest
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

    public Gson getGson() {
        return GSON;
    }

    public static WansMusicRecorderPlugin getInstance() {
        return instance;
    }
}