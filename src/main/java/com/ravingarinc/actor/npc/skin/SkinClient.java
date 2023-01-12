package com.ravingarinc.actor.npc.skin;

import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.Module;
import com.ravingarinc.actor.api.ModuleLoadException;
import com.ravingarinc.actor.api.async.AsyncHandler;
import com.ravingarinc.actor.api.async.Sync;
import com.ravingarinc.actor.api.util.I;
import com.ravingarinc.actor.npc.ActorManager;
import com.ravingarinc.actor.npc.type.PlayerActor;
import com.ravingarinc.actor.storage.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;
import org.mineskin.MineskinClient;
import org.mineskin.SkinOptions;
import org.mineskin.Variant;
import org.mineskin.Visibility;
import org.mineskin.data.Skin;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

public class SkinClient extends Module {
    private final MineskinClient skinClient;
    private final File skinFolder;

    private final Map<String, ActorSkin> cachedSkins;

    private final List<String> validExtensions;

    private final List<String> validFiles;

    private long lastFileRead = 0;

    private SkinRunner runner = null;

    private ActorManager actorManager;


    public SkinClient(final RavinPlugin plugin) {
        super(SkinClient.class, plugin, ConfigManager.class, ActorManager.class);
        this.skinClient = new MineskinClient("Actors", "d643ce8090a196feb834e39b410ae17b87ac7a2c514bd328e0f90fce61aa3461");
        this.skinFolder = new File(plugin.getDataFolder() + "/skins");
        this.cachedSkins = new ConcurrentHashMap<>();
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

        if (!skinFolder.exists()) {
            skinFolder.mkdir();
        }
        final SkinRunner newRunner = new SkinRunner();
        if (runner != null) {
            newRunner.queue.addAll(runner.getRemaining());
        }
        runner = newRunner;
        runner.runTaskTimerAsynchronously(plugin, 0, 5);
    }

    public ActorSkin getSkin(final String name) {
        return cachedSkins.get(name);
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
     * @param sender
     * @param file
     * @param name   Name of the skin. Generally, this must be unique!
     * @throws FileNotFoundException Thrown if file does not exist!
     */
    @Async.Schedule
    public void uploadSkin(final CommandSender sender, final File file, final String name) throws FileNotFoundException {
        final CompletableFuture<Skin> skinFuture = skinClient.generateUpload(file, SkinOptions.create(name, Variant.AUTO, Visibility.PUBLIC));
        AsyncHandler.runAsynchronously(() -> {
            final ActorSkin skin = cachedSkins.computeIfAbsent(name, ActorSkin::new);
            runner.queue(skin, skinFuture, sender);
        });
    }

    public void saveSkin(final CommandSender sender, final String url, final String name) {
        final CompletableFuture<Skin> skinFuture = skinClient.generateUrl(url);
        AsyncHandler.runAsynchronously(() -> {
            final ActorSkin skin = cachedSkins.computeIfAbsent(name, ActorSkin::new);
            runner.queue(skin, skinFuture, sender);
        });
    }


    /**
     * Get a list of the names of the current files inside the skins folder.
     *
     * @return The list of skins.
     */
    public Set<String> getSkins() {
        return cachedSkins.keySet();
    }

    @Override
    public void cancel() {
        runner.cancel();
    }

    private class SkinRunner extends BukkitRunnable {
        private final Queue<SkinUploadRequest> queue;

        public SkinRunner() {
            this.queue = new ConcurrentLinkedQueue<>();
        }

        @Sync.AsyncOnly
        public void queue(final ActorSkin skin, final CompletableFuture<Skin> future, final CommandSender sender) {
            this.queue.add(new SkinUploadRequest(skin, future, sender));
        }

        public List<SkinUploadRequest> getRemaining() {
            return queue.stream().toList();
        }

        @Override
        public void run() {
            while (!queue.isEmpty()) {
                queue.poll().waitForResult();
            }
        }
    }

    private class SkinUploadRequest {
        private final ActorSkin actorSkin;
        private final CompletableFuture<Skin> future;
        private final CommandSender sender;

        private SkinUploadRequest(final ActorSkin actorSkin, final CompletableFuture<Skin> future, @Nullable final CommandSender sender) {
            this.actorSkin = actorSkin;
            this.future = future;
            this.sender = sender;
        }

        @Blocking
        public void waitForResult() {
            try {
                actorSkin.setValues(future.get(30, TimeUnit.SECONDS));
                AsyncHandler.runSynchronously(() -> sender.sendMessage(ChatColor.GREEN + "Successfully uploaded skin named '" + actorSkin.getName() + "'!"));
                actorSkin.apply(actorManager);
                return;
            } catch (final InterruptedException e) {
                I.log(Level.SEVERE, "SkinUploadRequest with CompletableFuture was interrupted!", e);
            } catch (final ExecutionException | IllegalArgumentException e) {
                I.log(Level.SEVERE, "SkinUploadRequest encountered exception while waiting for CompletableFuture!", e);
            } catch (final TimeoutException e) {
                I.log(Level.WARNING, "SkinUploadRequest with CompletableFuture has timed out!", e);
                if (sender != null) {
                    AsyncHandler.runSynchronously(() -> sender.sendMessage(ChatColor.RED + "Could not upload the skin named '" + actorSkin.getName() + "' as the request timed out! Try again later!"));
                }
            }
            actorSkin.discard(actorManager);
            cachedSkins.remove(actorSkin.getName());
        }
    }
}
