package com.ravingarinc.actor.playback.path;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.playback.PathingManager;
import com.ravingarinc.actor.playback.api.Frame;
import org.bukkit.entity.Player;

public class PathFrame implements Frame {

    private final Vector3 initial;
    private final Vector3 terminal;
    private final short moveDx;
    private final short moveDy;
    private final short moveDz;

    private final float yaw;
    private final float postYaw;
    private final double yGradient;
    private final double pGradient;
    private final int upper;
    private final int max;
    private final int id;
    private float pitch;
    private float postPitch;
    private int iteration;

    public PathFrame(final int id, final Vector3 inital, final Vector3 terminal, final Vector3 next, final float speed) {
        this.id = id;
        this.initial = inital;
        this.terminal = terminal;
        this.iteration = 0;

        final double dX = (terminal.x - initial.x);
        final double dY = (terminal.y - initial.y);
        final double dZ = (terminal.z - initial.z);

        max = (int) ((Math.sqrt(square(dX) + square(dY) + square(dZ))) / speed);
        moveDx = (short) (dX * 4096.0 / max);
        moveDy = (short) (dY * 4096.0 / max);
        moveDz = (short) (dZ * 4096.0 / max);
        upper = (int) (max * 0.6);

        final double nX = next.x - terminal.x;
        final double nY = next.y - terminal.y;
        final double nZ = next.z - terminal.z;

        yaw = ((float) ((Math.atan2(dX, dZ) * 180.0D) / Math.PI) * -1F);
        postYaw = ((float) ((Math.atan2(nX, nZ) * 180.0D) / Math.PI) * -1F);

        pitch = dY == 0 ? 0F : (float) ((Math.atan2(Math.sqrt(square(dZ) + square(dX)), dY)) * 180.0D / Math.PI) * -1F;
        postPitch = nY == 0 ? 0F : (float) ((Math.atan2(Math.sqrt(square(nZ) + square(nX)), nY)) * 180.0D / Math.PI) * -1F;

        if (pitch < -90F) {
            pitch += 180F;
        }
        if (postPitch < -90F) {
            postPitch += 180F;
        }

        if ((yaw > 90F && postYaw < -90F)) {
            yGradient = -(yaw - (180F + postYaw)) / (0.0 - (max - upper));
        } else if ((yaw < -90F && postYaw > 90F)) {
            yGradient = (yaw + (180F - postYaw)) / (0.0 - (max - upper));
        } else {
            yGradient = (yaw - postYaw) / (0.0 - (max - upper));
        }
        pGradient = (pitch - postPitch) / (0.0 - (max - upper));
    }

    /**
     * Gets a copy of the initial point
     *
     * @return The point
     */
    protected Vector3 getInitial() {
        return initial.copy();
    }

    /**
     * Gets a copy of the terminal point
     *
     * @return The point
     */
    protected Vector3 getTerminal() {
        return terminal.copy();
    }

    protected int getMax() {
        return max;
    }

    protected void increment() {
        iteration++;
    }

    protected void resetIteration() {
        iteration = 0;
    }

    protected int getIteration() {
        return iteration;
    }

    protected byte getYaw() {
        return degreesToByte(yaw);
    }

    protected byte getPitch() {
        return degreesToByte(pitch);
    }

    @Override
    public Vector3 location(final Player viewer) {
        final int i = getIteration();
        final int factor = getMax();
        if (i == 0) {
            return getInitial();
        } else if (i < factor) {
            final Vector3 initial = getInitial();
            final Vector3 terminal = getTerminal();
            terminal.sub(initial);
            terminal.scale((double) i / factor);
            initial.add(terminal);
            return initial;
        } else {
            return getTerminal();
        }
    }

    protected PacketContainer[] getPackets(final PathingManager manager) {
        return iteration < upper ? normalPacket(manager) : transitionPacket(manager);
    }

    private PacketContainer[] normalPacket(final PathingManager manager) {
        final PacketContainer[] array = new PacketContainer[2];
        array[0] = manager.createPacket(PacketType.Play.Server.REL_ENTITY_MOVE_LOOK, (packet) -> {
            packet.getIntegers().write(0, id);
            packet.getShorts()
                    .write(0, moveDx)
                    .write(1, moveDy)
                    .write(2, moveDz);
            packet.getBytes()
                    .write(0, degreesToByte(yaw))
                    .write(1, degreesToByte(pitch));
            packet.getBooleans().write(0, true);
            packet.setMeta(PathingManager.PACKET_VALID_META, true);
        });

        array[1] = manager.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION, (packet) -> {
            packet.getIntegers().write(0, id);
            packet.getBytes().write(0, degreesToByte(yaw));
        });
        return array;
    }

    private PacketContainer[] transitionPacket(final PathingManager manager) {
        float newYaw;
        if ((yaw > 90F && postYaw < -90F)) {
            newYaw = (float) yGradient * (iteration - upper) + yaw;
            if (newYaw > 180F) {
                newYaw = newYaw - 360F;
            }
        } else if ((yaw < -90F && postYaw > 90F)) {
            newYaw = (float) yGradient * (iteration - upper) + yaw;
            if (newYaw < -180F) {
                newYaw = newYaw + 360F;
            }
        } else {
            newYaw = (float) yGradient * (iteration - upper) + yaw;
        }
        final byte byteYaw = degreesToByte(newYaw);
        final byte bytePitch = degreesToByte((float) pGradient * (iteration - upper) + pitch);

        final PacketContainer[] array = new PacketContainer[2];
        array[0] = manager.createPacket(PacketType.Play.Server.REL_ENTITY_MOVE_LOOK, (packet) -> {
            packet.getIntegers().write(0, id);
            packet.getShorts()
                    .write(0, moveDx)
                    .write(1, moveDy)
                    .write(2, moveDz);
            packet.getBytes()
                    .write(0, byteYaw)
                    .write(1, bytePitch);
            packet.getBooleans().write(0, true);
            packet.setMeta(PathingManager.PACKET_VALID_META, true);
        });

        array[1] = manager.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION, (packet) -> {
            packet.getIntegers().write(0, id);
            packet.getBytes().write(0, byteYaw);
        });

        return array;
    }
}
