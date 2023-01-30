package com.ravingarinc.actor.playback.scene;

import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.playback.api.Frame;
import org.bukkit.entity.Player;

public class SceneFrame implements Frame {
    @Override
    public Vector3 location(final Player viewer) {
        return null;
    }
}
