package com.ravingarinc.actor.npc.selector;

public class SelectionFailException extends Exception {

    public SelectionFailException(String reason) {
        super(reason);
    }

    public SelectionFailException(String reason, Throwable throwable) {
        super(reason, throwable);
    }
}
