package com.kdrag0n.bluestone;

import com.kdrag0n.bluestone.types.Module;

import java.lang.reflect.Method;

class ExtraEvent {
    private Method method;
    private Module parent;

    ExtraEvent(Method method, Module parent) {
        this.method = method;
        this.parent = parent;
    }

    /*package-private*/ Method getMethod() {
        return method;
    }

    /*package-private*/ Module getParent() {
        return parent;
    }
}
