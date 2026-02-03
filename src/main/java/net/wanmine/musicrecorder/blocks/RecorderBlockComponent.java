package net.wanmine.musicrecorder.blocks;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.wanmine.musicrecorder.WansMusicRecorderPlugin;
import net.wanmine.musicrecorder.gui.RecorderGUI;
import net.wanmine.musicrecorder.music.MusicGraph;
import net.wanmine.musicrecorder.music.MusicUtils;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RecorderBlockComponent implements Component<ChunkStore> {
    public static final BuilderCodec<RecorderBlockComponent> CODEC = BuilderCodec.builder(
                    RecorderBlockComponent.class,
                    RecorderBlockComponent::new
            )
            .append(new KeyedCodec<>("Disk", ItemContainer.CODEC), (s, v) -> s.diskContainer = (SimpleItemContainer) v, s -> s.diskContainer)
            .add()
            .append(new KeyedCodec<>("MusicGraph", MusicGraph.CODEC), (s, v) -> s.musicGraph = v, s -> s.musicGraph)
            .add()
            .append(new KeyedCodec<>("SongName", Codec.STRING), (s, v) -> s.songName = v, s -> s.songName)
            .add()
            .append(new KeyedCodec<>("IsAnglo", Codec.BOOLEAN), (s, v) -> s.isAnglo = v, s -> s.isAnglo)
            .add()
            .build();

    private SimpleItemContainer diskContainer;
    private MusicGraph musicGraph;
    private String songName;
    private boolean isAnglo;

    public RecorderBlockComponent() {
        this.diskContainer = new SimpleItemContainer((short) 1);
        this.musicGraph = new MusicGraph(3, 120, 26);
        this.songName = "New Song";
        this.isAnglo = false;
    }

    public RecorderBlockComponent(RecorderBlockComponent other) {
        this.diskContainer = other.diskContainer.clone();
        this.musicGraph = other.musicGraph.clone();
        this.songName = other.songName;
        this.isAnglo = other.isAnglo;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public String getSongName() {
        return songName;
    }

    public void registerAndPlay(Store<EntityStore> store) {
        CompletableFuture.supplyAsync(() -> MusicUtils.registerSong(this.musicGraph, this.songName, false), HytaleServer.SCHEDULED_EXECUTOR).thenAccept(outStr -> playSong(outStr, store));
    }

    public static void playSong(String songKey, Store<EntityStore> store) {
        if (songKey.isEmpty()) {
            return;
        }

        int id = SoundEvent.getAssetMap().getIndex(songKey);

        SoundUtil.playSoundEvent2d(id, SoundCategory.SFX, store);

        SoundEvent.getAssetStore().removeAssets(Collections.singletonList(songKey));

        WansMusicRecorderPlugin.getInstance().getSongsEventPath().resolve(songKey + ".json").toFile().delete();
        WansMusicRecorderPlugin.getInstance().getSongsPath().resolve(songKey + ".ogg").toFile().delete();
    }

    public MusicGraph getMusicGraph() {
        return musicGraph;
    }

    public SimpleItemContainer getDiskContainer() {
        return diskContainer;
    }

    public boolean isAnglo() {
        return isAnglo;
    }

    public void setAnglo(boolean isAnglo) {
        this.isAnglo = isAnglo;
    }

    public static ComponentType<ChunkStore, RecorderBlockComponent> getComponentType() {
        return WansMusicRecorderPlugin.getInstance().getRecorderBlockType();
    }

    @NullableDecl
    @Override
    public Component<ChunkStore> clone() {
        return new RecorderBlockComponent(this);
    }

    public static class RecorderRefSystem extends RefSystem<ChunkStore> {
        @Override
        public void onEntityAdded(@NonNullDecl Ref<ChunkStore> ref, @NonNullDecl AddReason reason, @NonNullDecl Store<ChunkStore> store, @NonNullDecl CommandBuffer<ChunkStore> commandBuffer) { }

        @Override
        public void onEntityRemove(@NonNullDecl Ref<ChunkStore> ref, @NonNullDecl RemoveReason reason, @NonNullDecl Store<ChunkStore> store, @NonNullDecl CommandBuffer<ChunkStore> commandBuffer) {
            RecorderBlockComponent instance = commandBuffer.getComponent(ref, RecorderBlockComponent.getComponentType());

            if (instance == null) {
                return;
            }

            if (reason == RemoveReason.REMOVE) {
                World world = store.getExternalData().getWorld();
                Store<EntityStore> worldStore = world.getEntityStore().getStore();
                List<ItemStack> allItemStacks = instance.getDiskContainer().dropAllItemStacks();

                BlockModule.BlockStateInfo info = store.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());

                if (info == null) {
                    return;
                }

                WorldChunk worldChunk = store.getComponent(info.getChunkRef(), WorldChunk.getComponentType());

                if (worldChunk == null) {
                    return;
                }

                int blockIndex = info.getIndex();

                int localX = ChunkUtil.worldCoordFromLocalCoord(worldChunk.getX(), ChunkUtil.xFromBlockInColumn(blockIndex));
                int localY = ChunkUtil.yFromBlockInColumn(blockIndex);
                int localZ = ChunkUtil.worldCoordFromLocalCoord(worldChunk.getZ(), ChunkUtil.zFromBlockInColumn(blockIndex));

                Holder<EntityStore>[] itemEntityHolders = ItemComponent.generateItemDrops(worldStore, allItemStacks, new Vector3d(localX, localY, localZ).add(0.5, 0.0, 0.5), Vector3f.ZERO);

                if (itemEntityHolders.length > 0) {
                    world.execute(() -> worldStore.addEntities(itemEntityHolders, AddReason.SPAWN));
                }
            }
        }

        @NullableDecl
        @Override
        public Query<ChunkStore> getQuery() {
            return RecorderBlockComponent.getComponentType();
        }
    }

    public static class OpenRecorderInteraction extends SimpleBlockInteraction {
        public static final BuilderCodec<RecorderBlockComponent.OpenRecorderInteraction> CODEC = BuilderCodec.builder(RecorderBlockComponent.OpenRecorderInteraction.class, RecorderBlockComponent.OpenRecorderInteraction::new, SimpleBlockInteraction.CODEC)
                .documentation("Opens the recorder page.")
                .build();

        public OpenRecorderInteraction() { }

        @Override
        protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InteractionType type, @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull Vector3i pos, @Nonnull CooldownHandler cooldownHandler) {
            Ref<EntityStore> ref = context.getEntity();
            Store<EntityStore> store = ref.getStore();

            Player playerComponent = commandBuffer.getComponent(ref, Player.getComponentType());
            PlayerRef playerRefComponent = commandBuffer.getComponent(ref, PlayerRef.getComponentType());

            WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));

            if (chunk == null) {
                return;
            }

            Ref<ChunkStore> chunkStore = chunk.getBlockComponentEntity(pos.x, pos.y, pos.z);

            if (chunkStore == null) {
                return;
            }

            RecorderBlockComponent recorderComponent = chunkStore.getStore().getComponent(chunkStore, RecorderBlockComponent.getComponentType());

            if (playerComponent != null && playerRefComponent != null && recorderComponent != null) {
                RecorderGUI gui = new RecorderGUI(playerRefComponent, recorderComponent, world);

                playerComponent.getPageManager().openCustomPage(ref, store, gui);
            }
        }

        @Override
        protected void simulateInteractWithBlock(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull World world, @Nonnull Vector3i targetBlock) { }
    }

    public static class DiskRecorderInteraction extends SimpleBlockInteraction {
        public static final BuilderCodec<RecorderBlockComponent.DiskRecorderInteraction> CODEC = BuilderCodec.builder(RecorderBlockComponent.DiskRecorderInteraction.class, RecorderBlockComponent.DiskRecorderInteraction::new, SimpleBlockInteraction.CODEC)
                .documentation("Do disk operations.")
                .build();

        public DiskRecorderInteraction() { }

        @Override
        protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InteractionType type, @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull Vector3i pos, @Nonnull CooldownHandler cooldownHandler) {
            Ref<EntityStore> ref = context.getEntity();

            Player playerComponent = commandBuffer.getComponent(ref, Player.getComponentType());
            PlayerRef playerRefComponent = commandBuffer.getComponent(ref, PlayerRef.getComponentType());

            WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));

            if (chunk == null) {
                return;
            }

            Ref<ChunkStore> chunkStore = chunk.getBlockComponentEntity(pos.x, pos.y, pos.z);

            if (chunkStore == null) {
                return;
            }

            RecorderBlockComponent recorderComponent = chunkStore.getStore().getComponent(chunkStore, RecorderBlockComponent.getComponentType());

            if (playerComponent != null && playerRefComponent != null && recorderComponent != null) {
                CombinedItemContainer playerContainer = playerComponent.getInventory().getCombinedStorageFirst();

                if (recorderComponent.diskContainer.getItemStack((short) 0) != null) {
                    if (playerContainer.addItemStack(Objects.requireNonNull(recorderComponent.diskContainer.getItemStack((short) 0))).succeeded()) {
                        recorderComponent.diskContainer.setItemStackForSlot((short) 0, ItemStack.EMPTY);

                        world.setBlockInteractionState(pos, Objects.requireNonNull(world.getBlockType(pos)), "Off");
                    }

                    return;
                }

                if (itemInHand == null || !itemInHand.getItem().getData().getRawTags().containsKey("WMRDisk")) {
                    return;
                }

                DiskMetadata diskMetadata = itemInHand.getFromMetadataOrNull("SavedSong", DiskMetadata.CODEC);

                if (diskMetadata != null) {
                    recorderComponent.setSongName(diskMetadata.getSongName());
                    recorderComponent.musicGraph = diskMetadata.getMusicGraph();
                }

                for (short i = 0; i < playerContainer.getCapacity(); i++) {
                    ItemStack invItemStack = playerContainer.getItemStack(i);

                    if (invItemStack != null && invItemStack.equals(itemInHand) && playerContainer.removeItemStack(Objects.requireNonNull(itemInHand.withQuantity(1))).succeeded()) {
                        world.setBlockInteractionState(pos, Objects.requireNonNull(world.getBlockType(pos)), "On");

                        recorderComponent.diskContainer.setItemStackForSlot((short) 0, itemInHand.withQuantity(1));

                        break;
                    }
                }
            }
        }

        @Override
        protected void simulateInteractWithBlock(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull World world, @Nonnull Vector3i targetBlock) { }
    }

    public static class DiskMetadata {
        private MusicGraph musicGraph;
        private String songName;

        public static final BuilderCodec<DiskMetadata> CODEC = BuilderCodec.builder(
                        DiskMetadata.class,
                        DiskMetadata::new
                )
                .append(new KeyedCodec<>("MusicGraph", MusicGraph.CODEC), (o, i) -> o.musicGraph = i, o -> o.musicGraph)
                .add()
                .append(new KeyedCodec<>("SongName", Codec.STRING), (o, i) -> o.songName = i, o -> o.songName)
                .add()
                .build();

        private DiskMetadata() { }

        public DiskMetadata(MusicGraph musicGraph, String songName) {
            this.musicGraph = musicGraph;
            this.songName = songName;
        }

        public MusicGraph getMusicGraph() {
            return musicGraph;
        }

        public String getSongName() {
            return songName;
        }
    }
}
