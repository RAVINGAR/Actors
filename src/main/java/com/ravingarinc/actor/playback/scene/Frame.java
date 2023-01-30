package com.ravingarinc.actor.playback.scene;

import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.playback.scene.action.Action;

public record Frame(Vector3 location, float yaw, Action... actions) {
}
