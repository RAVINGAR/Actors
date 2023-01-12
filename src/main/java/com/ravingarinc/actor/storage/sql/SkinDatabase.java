package com.ravingarinc.actor.storage.sql;

import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.ModuleLoadException;
import com.ravingarinc.actor.npc.skin.SkinClient;

import static com.ravingarinc.actor.storage.sql.SQLSchema.Skins;

public class SkinDatabase extends Database {
    private SkinClient client;

    public SkinDatabase(final RavinPlugin plugin) {
        super(Skins.SKINS, Skins.createTable, SkinDatabase.class, plugin, SkinClient.class);
    }

    @Override
    public void load() throws ModuleLoadException {
        super.load();
        client = plugin.getModule(SkinClient.class);
    }

    @Override
    public void cancel() {
        // todo saving here!

        super.cancel();
    }
}
