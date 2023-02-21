package com.ravingarinc.actor.playback.api;

import com.comphenix.protocol.events.PacketContainer;
import com.ravingarinc.actor.playback.PathingManager;
import com.ravingarinc.api.Vector3;
import org.bukkit.entity.Player;

public interface Movement {

    Vector3 location(Player viewer);

    /**
     * Gets the exact location for this frame.
     *
     * @return The location of this frame
     */
    Vector3 point();

    void increment();

    int max();

    void reset();

    int iteration();

    byte yaw();

    byte pitch();

    PacketContainer[] getPackets(final PathingManager manager);

    default double square(final double num) {
        return num * num;
    }

    default byte degreesToByte(final float degree) {
        return (byte) (degree * 256.0F / 360.0F);
    }
}
