package com.khronodragon.bluestone.util;

import javax.script.ScriptException;

public interface ScriptExceptionThrower {
    void exec() throws ScriptException;
}
