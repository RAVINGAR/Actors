package com.ravingarinc.actor.playback.api;

import com.ravingarinc.actor.api.util.Vector3;
import org.bukkit.entity.Player;

public interface Frame {

    Vector3 location(Player viewer);

    default double square(final double num) {
        return num * num;
    }

    default byte degreesToByte(final float degree) {
        return (byte) (degree * 256.0F / 360.0F);
    }
}
