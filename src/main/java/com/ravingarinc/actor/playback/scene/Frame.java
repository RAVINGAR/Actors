package com.ravingarinc.actor.playback.scene;

import com.ravingarinc.actor.playback.scene.action.Action;
import com.ravingarinc.api.Vector3;

public record Frame(Vector3 location, float yaw, Action... actions) {
}
