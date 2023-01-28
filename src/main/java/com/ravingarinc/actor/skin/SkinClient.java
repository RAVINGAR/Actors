package com.ravingarinc.actor.skin;

import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.BiMap;
import com.ravingarinc.actor.api.Module;
import com.ravingarinc.actor.api.ModuleLoadException;
import com.ravingarinc.actor.api.async.AsyncHandler;
import com.ravingarinc.actor.api.async.AsynchronousException;
import com.ravingarinc.actor.api.async.BlockingRunner;
import com.ravingarinc.actor.api.async.CompletableRunnable;
import com.ravingarinc.actor.api.async.Sync;
import com.ravingarinc.actor.api.util.I;
import com.ravingarinc.actor.npc.ActorManager;
import com.ravingarinc.actor.npc.type.PlayerActor;
import com.ravingarinc.actor.storage.ConfigManager;
import com.ravingarinc.actor.storage.sql.SkinDatabase;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mineskin.MineskinClient;
import org.mineskin.SkinOptions;
import org.mineskin.Variant;
import org.mineskin.Visibility;
import org.mineskin.data.Skin;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

public class SkinClient extends Module {
    private final MineskinClient skinClient;
    private final File skinFolder;

    private final BiMap<String, UUID, ActorSkin> cachedSkins;

    private final List<String> validExtensions;

    private final List<String> validFiles;

    private long lastFileRead = 0;

    private BlockingRunner<CompletableRunnable<Skin>> runner = null;

    private ActorManager actorManager;

    private SkinDatabase database;


    public SkinClient(final RavinPlugin plugin) {
        super(SkinClient.class, plugin, ConfigManager.class, ActorManager.class);
        this.skinClient = new MineskinClient("Actors", "d643ce8090a196feb834e39b410ae17b87ac7a2c514bd328e0f90fce61aa3461");
        this.skinFolder = new File(plugin.getDataFolder() + "/skins");
        this.cachedSkins = new BiMap<>(String.class, UUID.class);
        this.validFiles = new ArrayList<>();
        this.validExtensions = new ArrayList<>();
        validExtensions.add(".png");
        validExtensions.add(".jpeg");
        validExtensions.add(".jpg");
    }

    public List<String> getValidExtensions() {
        return Collections.unmodifiableList(validExtensions);
    }

    public File getSkinFolder() {
        return skinFolder;
    }

    public List<String> getValidFileNames() {
        final long currentTime = System.currentTimeMillis();
        if (currentTime > lastFileRead + 10000) {
            lastFileRead = currentTime;
            validFiles.clear();
            final File[] files = skinFolder.listFiles(pathname -> {
                for (final String extension : validExtensions) {
                    if (pathname.getName().endsWith(extension)) {
                        return true;
                    }
                }
                return false;
            });
            if (files == null) {
                validFiles.add("<file>");
            } else {
                for (final File file : files) {
                    validFiles.add(file.getName());
                }
            }
        }
        return Collections.unmodifiableList(validFiles);
    }

    @Override
    protected void load() throws ModuleLoadException {
        this.actorManager = plugin.getModule(ActorManager.class);
        this.database = plugin.getModule(SkinDatabase.class);
        if (!skinFolder.exists()) {
            skinFolder.mkdir();
        }
        final BlockingRunner<CompletableRunnable<Skin>> newRunner = new BlockingRunner<>(new LinkedBlockingQueue<>());
        if (runner != null) {
            newRunner.queueAll(runner.getRemaining());
        }
        runner = newRunner;
        runner.runTaskAsynchronously(plugin);
    }

    @SuppressWarnings("all")
    @Nullable
    public ActorSkin getSkin(final String name) {
        return cachedSkins.get(name);
    }

    @SuppressWarnings("all")
    @Nullable
    public ActorSkin getSkin(final UUID uuid) {
        return cachedSkins.get(uuid);
    }

    /**
     * Iterate all skins and unlink the actor from them if it is linked.
     *
     * @param actor The actor
     */
    @Sync.AsyncOnly
    public void unlinkActorAll(final PlayerActor actor) {
        cachedSkins.values().forEach(skin -> skin.unlinkActor(actor));
    }

    /**
     * Upload a new skin, to either a new ActorSkin or applying it to an already existing ActorSkin object. This queues
     * the task on an asynchronous thread.
     *
     * @param sender The sender
     * @param file   The file
     * @param name   Name of the skin.
     * @throws FileNotFoundException Thrown if file does not exist!
     */
    @Async.Schedule
    public void uploadSkin(final CommandSender sender, final File file, final String name) throws FileNotFoundException {
        final CompletableFuture<Skin> skinFuture = skinClient.generateUpload(file, SkinOptions.create(name, Variant.AUTO, Visibility.PUBLIC));
        AsyncHandler.runAsynchronously(() -> {
            final ActorSkin skin = getOrCreate(name);
            queue(skin, skinFuture, sender);
        });
    }

    public void saveSkin(final CommandSender sender, final String url, final String name) {
        final CompletableFuture<Skin> skinFuture = skinClient.generateUrl(url);
        AsyncHandler.runAsynchronously(() -> {
            final ActorSkin skin = getOrCreate(name);
            queue(skin, skinFuture, sender);
        });
    }

    /**
     * Attempts to get a skin from the existing skin cache otherwise
     *
     * @param name
     * @return
     */
    public ActorSkin getOrCreate(final String name) {
        ActorSkin skin = getSkin(name);
        if (skin == null) {
            skin = createSkin(name, UUID.randomUUID(), null, null);
        }
        return skin;
    }

    /**
     * Creates a new skin with the given values and then adds it to the skin cache.
     * When calling this method, it is assumed that a skin does NOT already exist at the given name or UUID
     *
     * @param name      The name
     * @param uuid      The uuid
     * @param value     The value
     * @param signature The signature
     * @return The new skin
     */
    public ActorSkin createSkin(@NotNull final String name, @NotNull final UUID uuid, @Nullable final String value, @Nullable final String signature) {
        final ActorSkin skin = new ActorSkin(name, uuid, value, signature);
        cachedSkins.put(name, uuid, skin);
        return skin;
    }

    @Sync.AsyncOnly
    public void updateSkin(@NotNull final String name, @Nullable final UUID uuid, @Nullable final String value, @Nullable final String signature) {
        ActorSkin skin = getSkin(name);
        if (skin == null && uuid != null) {
            skin = getSkin(uuid);
        }
        if (skin == null) {
            throw new IllegalArgumentException("Could not find skin with name '" + name + "' nor with UUID '" + uuid + "'!");
        }

    }

    public void updateSkin(@NotNull final ActorSkin skin, @Nullable final String name, @Nullable final String value, @Nullable final String signature) {
        if (skin.updateName(name)) {
            cachedSkins.removeBoth(name, skin.getUUID());
            cachedSkins.put(name, skin.getUUID(), skin);
        }
        // It is assumed that value and signature are always updated together
        if (skin.updateTexture(value, signature)) {
            skin.apply(actorManager);
        }
    }

    @Sync.AsyncOnly
    public void queue(final ActorSkin skin, final CompletableFuture<Skin> future, final CommandSender sender) {
        try {
            this.runner.queue(new CompletableRunnable<>(future, (result) -> {
                if (result == null) {
                    skin.discard(actorManager);
                    cachedSkins.remove(skin.getUUID());
                    if (sender != null) {
                        AsyncHandler.runSynchronously(() -> sender.sendMessage(ChatColor.RED + "Could not upload the skin named '" + skin.getName() + "' as the request timed out! Try again later!"));
                    }
                } else {
                    skin.updateTexture(result.data.texture.value, result.data.texture.signature);
                    skin.apply(actorManager);
                    database.saveSkin(skin);

                    if (sender != null) {
                        AsyncHandler.runSynchronously(() -> sender.sendMessage(ChatColor.GREEN + "Successfully uploaded skin named '" + skin.getName() + "'!"));
                    }
                }
            }, 15000L));
        }
        catch(AsynchronousException e) {
            I.log(Level.SEVERE, e.getMessage(), e);
        }
    }


    /**
     * Get a list of the names of the current files inside the skins folder.
     *
     * @return The list of skins.
     */
    public Set<String> getSkinNames() {
        return new HashSet<>(cachedSkins.firstKeys());
    }

    public Collection<ActorSkin> getSkins() {
        return Collections.unmodifiableCollection(cachedSkins.values());
    }

    @Override
    public void cancel() {
        runner.cancel(true);
        try {
            CompletableRunnable<Skin> runnable = new CompletableRunnable<>(
                    CompletableFuture.completedFuture(null),
                    (v) -> runner.getCancelTask().run());
            runner.queue(runnable);
            AsyncHandler.waitForFuture(runnable);
        }
        catch(AsynchronousException e) {
            I.log(Level.SEVERE, "Could not shut down SkinClient module!", e);
        }

        cachedSkins.clear();
    }
}
