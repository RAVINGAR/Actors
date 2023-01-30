package com.ravingarinc.actor.pathing.type;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.pathing.PathingManager;

public class Frame {

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
    private final int factor;
    private final int id;
    private float pitch;
    private float postPitch;
    private int iteration;

    public Frame(final int id, final Vector3 inital, final Vector3 terminal, final Vector3 next, final float speed) {
        this.id = id;
        this.initial = inital;
        this.terminal = terminal;
        this.iteration = 0;

        final double dX = (terminal.x - initial.x);
        final double dY = (terminal.y - initial.y);
        final double dZ = (terminal.z - initial.z);

        factor = (int) ((Math.sqrt(square(dX) + square(dY) + square(dZ))) / speed);
        moveDx = (short) (dX * 4096.0 / factor);
        moveDy = (short) (dY * 4096.0 / factor);
        moveDz = (short) (dZ * 4096.0 / factor);
        upper = (int) (factor * 0.6);

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
            yGradient = -(yaw - (180F + postYaw)) / (0.0 - (factor - upper));
        } else if ((yaw < -90F && postYaw > 90F)) {
            yGradient = (yaw + (180F - postYaw)) / (0.0 - (factor - upper));
        } else {
            yGradient = (yaw - postYaw) / (0.0 - (factor - upper));
        }
        pGradient = (pitch - postPitch) / (0.0 - (factor - upper));
    }

    private double square(final double num) {
        return num * num;
    }

    private byte degreesToByte(final float degree) {
        return (byte) (degree * 256.0F / 360.0F);
    }

    /**
     * Gets a copy of the initial point
     *
     * @return The point
     */
    public Vector3 getInitial() {
        return initial.copy();
    }

    /**
     * Gets a copy of the terminal point
     *
     * @return The point
     */
    public Vector3 getTerminal() {
        return terminal.copy();
    }

    public int getFactor() {
        return factor;
    }

    public void increment() {
        iteration++;
    }

    public void resetIteration() {
        iteration = 0;
    }

    public int getIteration() {
        return iteration;
    }

    public PacketContainer[] getPackets(final ProtocolManager manager) {
        return iteration < upper ? normalPacket(manager) : transitionPacket(manager);
    }

    private PacketContainer[] normalPacket(final ProtocolManager manager) {
        final PacketContainer[] array = new PacketContainer[2];
        array[0] = manager.createPacket(PacketType.Play.Server.REL_ENTITY_MOVE_LOOK);
        array[0].getIntegers().write(0, id);
        array[0].getShorts()
                .write(0, moveDx)
                .write(1, moveDy)
                .write(2, moveDz);
        array[0].getBytes()
                .write(0, degreesToByte(yaw))
                .write(1, degreesToByte(pitch));
        array[0].getBooleans().write(0, true);
        array[0].setMeta(PathingManager.PACKET_VALID_META, true);

        array[1] = manager.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
        array[1].getIntegers().write(0, id);
        array[1].getBytes().write(0, degreesToByte(yaw));

        return array;
    }

    private PacketContainer[] transitionPacket(final ProtocolManager manager) {
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
        array[0] = manager.createPacket(PacketType.Play.Server.REL_ENTITY_MOVE_LOOK);
        array[0].getIntegers().write(0, id);
        array[0].getShorts()
                .write(0, moveDx)
                .write(1, moveDy)
                .write(2, moveDz);
        array[0].getBytes()
                .write(0, byteYaw)
                .write(1, bytePitch);
        array[0].getBooleans().write(0, true);
        array[0].setMeta(PathingManager.PACKET_VALID_META, true);

        array[1] = manager.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
        array[1].getIntegers().write(0, id);
        array[1].getBytes().write(0, byteYaw);

        return array;
    }
}
