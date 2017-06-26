package com.khronodragon.bluestone;

import java.lang.reflect.Method;

public class ExtraEvent {
    private Method method;
    private boolean threaded;
    private Cog parent;

    ExtraEvent(Method method, boolean needsThread, Cog parent) {
        this.method = method;
        this.threaded = needsThread;
        this.parent = parent;
    }

    public Method getMethod() {
        return method;
    }

    public boolean isThreaded() {
        return threaded;
    }

    public Cog getParent() {
        return parent;
    }
}
