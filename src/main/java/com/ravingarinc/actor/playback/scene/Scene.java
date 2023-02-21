package com.ravingarinc.actor.playback.scene;

import com.comphenix.protocol.events.PacketContainer;
import com.ravingarinc.actor.playback.PathingManager;
import com.ravingarinc.actor.playback.api.Movement;
import com.ravingarinc.api.Vector3;
import org.bukkit.entity.Player;

/**
 * A Scene exists such that it contains multiple InnerFrames, each frame represents a relative position to the.
 */
public class Scene implements Movement {
    @Override
    public Vector3 location(final Player viewer) {
        return null;
    }

    @Override
    public Vector3 point() {
        return null;
    }

    @Override
    public void increment() {

    }

    @Override
    public int max() {
        return 0;
    }

    @Override
    public void reset() {

    }

    @Override
    public int iteration() {
        return 0;
    }

    @Override
    public byte yaw() {
        return 0;
    }

    @Override
    public byte pitch() {
        return 0;
    }

    @Override
    public PacketContainer[] getPackets(final PathingManager manager) {
        return new PacketContainer[0];
    }
}
