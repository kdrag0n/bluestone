package com.khronodragon.bluestone.util;

import groovy.lang.GroovyShell;

import javax.script.*;
import java.io.Reader;

public class GroovyScriptEngine implements ScriptEngine {
    private final GroovyShell shell = new GroovyShell();

    @Override
    public Object eval(String script) throws ScriptException {
        try {
            return shell.evaluate(script);
        } catch (Exception e) {
            throw new ScriptException(e);
        }
    }

    @Override
    public Object eval(Reader script) throws ScriptException {
        try {
            return shell.evaluate(script);
        } catch (Exception e) {
            throw new ScriptException(e);
        }
    }

    @Override
    public void put(String key, Object value) {
        shell.setVariable(key, value);
    }

    @Override
    public Object eval(String script, ScriptContext context) {
        return null;
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) {
        return null;
    }

    @Override
    public Object eval(Reader reader, Bindings n) {
        return null;
    }

    @Override
    public Object eval(String script, Bindings n) {
        return null;
    }

    @Override
    public ScriptContext getContext() {
        return null;
    }

    @Override
    public Object get(String key) {
        return null;
    }

    @Override
    public void setContext(ScriptContext context) {

    }

    @Override
    public void setBindings(Bindings bindings, int scope) {

    }

    @Override
    public ScriptEngineFactory getFactory() {
        return null;
    }

    @Override
    public Bindings createBindings() {
        return null;
    }

    @Override
    public Bindings getBindings(int scope) {
        return null;
    }
}
