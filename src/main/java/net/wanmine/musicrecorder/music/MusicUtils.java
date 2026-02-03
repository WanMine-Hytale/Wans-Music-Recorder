package net.wanmine.musicrecorder.music;

import com.hypixel.hytale.assetstore.AssetLoadResult;
import com.hypixel.hytale.assetstore.AssetUpdateQuery;
import com.hypixel.hytale.server.core.asset.common.CommonAssetModule;
import com.hypixel.hytale.server.core.asset.common.CommonAssetRegistry;
import com.hypixel.hytale.server.core.asset.common.asset.FileCommonAsset;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import net.wanmine.musicrecorder.WansMusicRecorderPlugin;
import ws.schild.jave.EncoderException;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class MusicUtils {
    // Private constructor to prevent instantiation
    private MusicUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String registerSong(MusicGraph musicGraph, String songName, boolean hasUUID) {
        if (songName.isEmpty()) {
            return "";
        }

        CommonAssetModule commonAssetModule = WansMusicRecorderPlugin.getInstance().getCommonAssetModule();

        if (commonAssetModule == null) {
            WansMusicRecorderPlugin.getInstance().getLogger().at(Level.WARNING).log("CommonAssetModule not available; Cannot register song.");

            return "";
        }

        if (musicGraph == null) {
            WansMusicRecorderPlugin.getInstance().getLogger().at(Level.WARNING).log("Music graph is null; Cannot register song.");

            return "";
        }

        String finalSongName = hasUUID ? songName + ".ogg" : songName.replace(" ", "_") + "_" + UUID.randomUUID() + ".ogg";
        File songFile = WansMusicRecorderPlugin.getInstance().getSongsPath().resolve(finalSongName).toFile();

        String assetName = "Sounds/" + finalSongName;

        if (!CommonAssetRegistry.hasCommonAsset(assetName)) {
            try {
                OggGenerator.generateOgg(musicGraph, WansMusicRecorderPlugin.getInstance().getSongsPath(), finalSongName.replace(".ogg", ""));
            } catch (IOException | EncoderException e) {
                WansMusicRecorderPlugin.getInstance().getLogger().at(Level.WARNING).withCause(e).log("Failed to generate song: %s", songName);

                return "";
            }

            if (!songFile.exists()) {
                WansMusicRecorderPlugin.getInstance().getLogger().at(Level.WARNING).log("Song file does not exist; Cannot register song.");

                return "";
            }

            // Loading happens here
            try {
                byte[] bytes = Files.readAllBytes(songFile.toPath());

                commonAssetModule.addCommonAsset(WansMusicRecorderPlugin.RUNTIME_PACK_NAME, new FileCommonAsset(songFile.toPath(), assetName, bytes));
            } catch (IOException e) {
                WansMusicRecorderPlugin.getInstance().getLogger().at(Level.WARNING).withCause(e).log("Failed to register sound asset %s", assetName);
            }
        }

        return registerSongEvent(finalSongName.replace(".ogg", ".json"), songFile);
    }

    public static String registerSongEvent(String songName, File songFilePath) {
        File songEventFile = WansMusicRecorderPlugin.getInstance().getSongsEventPath().resolve(songName).toFile();

        if (songEventFile.exists()) {
            return songName.replace(".json", "");
        }

        Map<String, Object> layer = new HashMap<>();
        layer.put("Files", Collections.singletonList("Sounds/" + songFilePath.toPath().getFileName().toString()));
        layer.put("Volume", 10);

        Map<String, Object> soundEvent = new HashMap<>();
        soundEvent.put("StartAttenuationDistance", 10);
        soundEvent.put("MaxDistance", 60);
        soundEvent.put("Volume", 10);
        soundEvent.put("Parent", "SFX_Attn_Quiet");
        soundEvent.put("Pitch", 1.0);
        soundEvent.put("Layers", Collections.singletonList(layer));

        try (Writer writer = Files.newBufferedWriter(songEventFile.toPath())) {
            WansMusicRecorderPlugin.getInstance().getGson().toJson(soundEvent, writer);
        } catch (IOException e) {
            WansMusicRecorderPlugin.getInstance().getLogger().at(Level.SEVERE).withCause(e).log("Failed to write SoundEvent at %s", songEventFile);
        }

        // Loading happens here
        try {
            AssetLoadResult<String, SoundEvent> result = SoundEvent.getAssetStore().loadAssetsFromPaths(WansMusicRecorderPlugin.RUNTIME_PACK_NAME, Collections.singletonList(songEventFile.toPath()), AssetUpdateQuery.DEFAULT, true);

            if (result.hasFailed()) {
                WansMusicRecorderPlugin.getInstance().getLogger().at(Level.WARNING).log("Some SoundEvent assets failed to load for %s", songName);
            } else {
                return result.getLoadedAssets().values().iterator().next().getId();
            }
        } catch (Exception e) {
            WansMusicRecorderPlugin.getInstance().getLogger().at(Level.WARNING).withCause(e).log("Failed to load SoundEvent assets for %s", songName);
        }

        return "";
    }
}
