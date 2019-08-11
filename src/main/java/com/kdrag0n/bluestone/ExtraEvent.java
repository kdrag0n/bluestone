package com.kdrag0n.bluestone;

import java.lang.reflect.Method;

class ExtraEvent {
    private Method method;
    private Cog parent;

    ExtraEvent(Method method, Cog parent) {
        this.method = method;
        this.parent = parent;
    }

    /*package-private*/ Method getMethod() {
        return method;
    }

    /*package-private*/ Cog getParent() {
        return parent;
    }
}
