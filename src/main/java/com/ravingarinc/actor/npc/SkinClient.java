package com.ravingarinc.actor.npc;

import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.Module;
import com.ravingarinc.actor.api.ModuleLoadException;
import com.ravingarinc.actor.file.ConfigManager;
import org.mineskin.MineskinClient;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkinClient extends Module {
    private final MineskinClient skinClient;
    private final File skinFolder;

    private final Map<UUID, ActorSkin> cachedSkins;


    public SkinClient(final RavinPlugin plugin) {
        super(SkinClient.class, plugin, ConfigManager.class);
        this.skinClient = new MineskinClient("Actors", "d643ce8090a196feb834e39b410ae17b87ac7a2c514bd328e0f90fce61aa3461");
        this.skinFolder = new File(plugin.getDataFolder() + "/skins");
        this.cachedSkins = new ConcurrentHashMap<>();
    }

    @Override
    protected void load() throws ModuleLoadException {
        if (!skinFolder.exists()) {
            skinFolder.mkdir();
        }

    }

    public ActorSkin getSkin(final UUID uuid) {

    }


    /**
     * Get a list of the names of the current files inside the skins folder.
     *
     * @return The list of skins.
     */
    public List<String> getSkins() {

    }

    @Override
    public void cancel() {

    }
}
