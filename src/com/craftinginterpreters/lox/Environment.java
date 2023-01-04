package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
    final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();
    Environment() {
        enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }
    Object get(Token name) {
        // TODO: Change this code to make it look better.
        if (values.containsKey(name.lexeme) && values.get(name.lexeme)!=null) {
            return values.get(name.lexeme);
        }
        if (enclosing != null && enclosing.get(name)!=null) return enclosing.get(name);

        throw new RuntimeError(name,
                "Undefined variable '" + name.lexeme + "'.");
    }
    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name,
                "Undefined variable '" + name.lexeme + "'.");
    }

    void define(String name, Object value) {
        values.put(name, value);
    }
}