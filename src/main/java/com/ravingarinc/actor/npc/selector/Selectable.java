package com.ravingarinc.actor.npc.selector;

import org.bukkit.entity.Player;

public interface Selectable {

    void onSelect(Player selector) throws SelectionFailException;

    void onUnselect(Player selector) throws SelectionFailException;
}
