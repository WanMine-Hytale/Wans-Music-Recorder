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
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.wanmine.musicrecorder.WansMusicRecorderPlugin;
import net.wanmine.musicrecorder.music.MusicGraph;
import net.wanmine.musicrecorder.music.MusicUtils;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PlayerBlockComponent implements Component<ChunkStore> {
    public static final BuilderCodec<PlayerBlockComponent> CODEC = BuilderCodec.builder(
                    PlayerBlockComponent.class,
                    PlayerBlockComponent::new
            )
            .append(new KeyedCodec<>("Disk", ItemContainer.CODEC), (s, v) -> s.diskContainer = (SimpleItemContainer) v, s -> s.diskContainer)
            .add()
            .append(new KeyedCodec<>("MusicGraph", MusicGraph.CODEC), (s, v) -> s.musicGraph = v, s -> s.musicGraph)
            .add()
            .append(new KeyedCodec<>("SongName", Codec.STRING), (s, v) -> s.songName = v, s -> s.songName)
            .add()
            .build();

    private SimpleItemContainer diskContainer;
    private MusicGraph musicGraph;
    private String songName;

    private ScheduledFuture<?> future;

    public PlayerBlockComponent() {
        this.diskContainer = new SimpleItemContainer((short) 1);
        this.musicGraph = new MusicGraph(3, 120, 25);
        this.songName = "";
    }

    public PlayerBlockComponent(PlayerBlockComponent other) {
        this.diskContainer = other.diskContainer.clone();
        this.musicGraph = other.musicGraph.clone();
        this.songName = other.songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public String getSongName() {
        return songName;
    }

    public MusicGraph getMusicGraph() {
        return musicGraph;
    }

    public SimpleItemContainer getDiskContainer() {
        return diskContainer;
    }

    public void registerAndPlay(Store<EntityStore> store, Vector3i pos, String worldName) {
        this.songName = MusicUtils.registerSong(this.musicGraph, this.songName, true);

        playSong(store, pos, worldName);
    }

    public void playSong(Store<EntityStore> store, Vector3i pos, String worldName) {
        if (this.songName.isEmpty()) {
            return;
        }

        int id = SoundEvent.getAssetMap().getIndex(this.songName);

        World world = Universe.get().getWorld(worldName);

        if (world == null) {
            return;
        }

        world.execute(() -> SoundUtil.playSoundEvent3d(id, SoundCategory.SFX, pos.x, pos.y, pos.z, store));
    }

    public static ComponentType<ChunkStore, PlayerBlockComponent> getComponentType() {
        return WansMusicRecorderPlugin.getInstance().getPlayerBlockType();
    }

    @NullableDecl
    @Override
    public Component<ChunkStore> clone() {
        return new PlayerBlockComponent(this);
    }

    public static class PlayerRefSystem extends RefSystem<ChunkStore> {
        @Override
        public void onEntityAdded(@NonNullDecl Ref<ChunkStore> ref, @NonNullDecl AddReason reason, @NonNullDecl Store<ChunkStore> store, @NonNullDecl CommandBuffer<ChunkStore> commandBuffer) {
            BlockModule.BlockStateInfo info = store.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());

            if (info == null) {
                return;
            }

            WorldChunk worldChunk = store.getComponent(info.getChunkRef(), WorldChunk.getComponentType());

            if (worldChunk == null) {
                return;
            }

            PlayerBlockComponent instance = commandBuffer.getComponent(ref, PlayerBlockComponent.getComponentType());
            World world = store.getExternalData().getWorld();

            if (instance != null && instance.getDiskContainer().getItemStack((short) 0) != null && !instance.getSongName().isEmpty() && instance.getMusicGraph() != null) {
                int blockIndex = info.getIndex();

                int localX = ChunkUtil.worldCoordFromLocalCoord(worldChunk.getX(), ChunkUtil.xFromBlockInColumn(blockIndex));
                int localY = ChunkUtil.yFromBlockInColumn(blockIndex);
                int localZ = ChunkUtil.worldCoordFromLocalCoord(worldChunk.getZ(), ChunkUtil.zFromBlockInColumn(blockIndex));

                Store<EntityStore> worldStore = world.getEntityStore().getStore();

                String worldName = world.getName();

                instance.future = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> instance.registerAndPlay(worldStore, new Vector3i(localX, localY, localZ), worldName), 0, Math.round(instance.getMusicGraph().getTotalDuration()), TimeUnit.SECONDS);
            }
        }

        @Override
        public void onEntityRemove(@NonNullDecl Ref<ChunkStore> ref, @NonNullDecl RemoveReason reason, @NonNullDecl Store<ChunkStore> store, @NonNullDecl CommandBuffer<ChunkStore> commandBuffer) {
            PlayerBlockComponent instance = commandBuffer.getComponent(ref, PlayerBlockComponent.getComponentType());

            if (instance == null) {
                return;
            }

            if (reason == RemoveReason.REMOVE) {
                instance.future.cancel(true);

                HytaleServer.SCHEDULED_EXECUTOR.execute(() -> {
                    SoundEvent.getAssetStore().removeAssets(Collections.singletonList(instance.getSongName()));
                    WansMusicRecorderPlugin.getInstance().getSongsEventPath().resolve(instance.getSongName() + ".json").toFile().delete();
                    WansMusicRecorderPlugin.getInstance().getSongsPath().resolve(instance.getSongName() + ".ogg").toFile().delete();
                });

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

    public static class DiskPlayerInteraction extends SimpleBlockInteraction {
        public static final BuilderCodec<PlayerBlockComponent.DiskPlayerInteraction> CODEC = BuilderCodec.builder(PlayerBlockComponent.DiskPlayerInteraction.class, PlayerBlockComponent.DiskPlayerInteraction::new, SimpleBlockInteraction.CODEC)
                .documentation("Do disk operations.")
                .build();

        public DiskPlayerInteraction() { }

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

            PlayerBlockComponent diskPlayerComponent = chunkStore.getStore().getComponent(chunkStore, PlayerBlockComponent.getComponentType());

            if (playerComponent != null && playerRefComponent != null && diskPlayerComponent != null) {
                CombinedItemContainer playerContainer = playerComponent.getInventory().getCombinedStorageFirst();

                if (diskPlayerComponent.getDiskContainer().getItemStack((short) 0) != null) {
                    if (playerContainer.addItemStack(Objects.requireNonNull(diskPlayerComponent.getDiskContainer().getItemStack((short) 0))).succeeded()) {
                        diskPlayerComponent.getDiskContainer().setItemStackForSlot((short) 0, ItemStack.EMPTY);

                        world.setBlockInteractionState(pos, Objects.requireNonNull(world.getBlockType(pos)), "Off");

                        CompletableFuture.runAsync(() -> {
                            if (diskPlayerComponent.future != null) {
                                diskPlayerComponent.future.cancel(true);
                            }

                            SoundEvent.getAssetStore().removeAssets(Collections.singletonList(diskPlayerComponent.getSongName()));
                            WansMusicRecorderPlugin.getInstance().getSongsEventPath().resolve(diskPlayerComponent.getSongName() + ".json").toFile().delete();
                            WansMusicRecorderPlugin.getInstance().getSongsPath().resolve(diskPlayerComponent.getSongName() + ".ogg").toFile().delete();
                        }, HytaleServer.SCHEDULED_EXECUTOR).thenAccept(_ -> {
                            diskPlayerComponent.setSongName("");
                            diskPlayerComponent.musicGraph = new MusicGraph(3, 120, 26);
                        });
                    }

                    return;
                }

                if (itemInHand == null || !itemInHand.getItem().getData().getRawTags().containsKey("WMRDisk")) {
                    return;
                }

                RecorderBlockComponent.DiskMetadata diskMetadata = itemInHand.getFromMetadataOrNull("SavedSong", RecorderBlockComponent.DiskMetadata.CODEC);

                if (diskMetadata != null) {
                    diskPlayerComponent.setSongName(diskMetadata.getSongName().replace(" ", "_") + "_" + UUID.randomUUID());
                    diskPlayerComponent.musicGraph = diskMetadata.getMusicGraph();
                }

                for (short i = 0; i < playerContainer.getCapacity(); i++) {
                    ItemStack invItemStack = playerContainer.getItemStack(i);

                    if (invItemStack != null && invItemStack.equals(itemInHand) && playerContainer.removeItemStack(Objects.requireNonNull(itemInHand.withQuantity(1))).succeeded()) {
                        world.setBlockInteractionState(pos, Objects.requireNonNull(world.getBlockType(pos)), "On");

                        diskPlayerComponent.getDiskContainer().setItemStackForSlot((short) 0, itemInHand.withQuantity(1));

                        Store<EntityStore> worldStore = world.getEntityStore().getStore();

                        String worldName = world.getName();

                        diskPlayerComponent.future = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> diskPlayerComponent.registerAndPlay(worldStore, pos, worldName), 0, Math.round(diskPlayerComponent.getMusicGraph().getTotalDuration()), TimeUnit.SECONDS);

                        break;
                    }
                }
            }
        }

        @Override
        protected void simulateInteractWithBlock(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull World world, @Nonnull Vector3i targetBlock) { }
    }
}
