package com.craftinginterpreters.lox;

import java.util.InputMismatchException;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import java.lang.annotation.Native;

public class NativeFunctions {
        private final Environment globals;
        private final Random rand = new Random();
        Scanner reader = new Scanner(System.in);
        public NativeFunctions(Environment globals){
            this.globals = globals;
        }
        public void defineClock(){
            globals.define("clock", new LoxCallable() {
                @Override
                public int arity() { return 0; }

                @Override
                public Object call(Interpreter interpreter,
                                   List<Object> arguments) {
                    return (double)System.currentTimeMillis() / 1000.0;
                }

                @Override
                public String toString() { return "<native fn>"; }
            });
        }
        public void defineRandom(){
            globals.define("random", new LoxCallable() {
                @Override
                public int arity() { return 1; }

                @Override
                public Object call(Interpreter interpreter,
                                   List<Object> arguments) {
                    double input = (double)arguments.get(0);
                    return (double)rand.nextInt((int)input);
                }

                @Override
                public String toString() { return "<native fn>"; }
            });
        }
        public void defineUserInput(){
            globals.define("read", new LoxCallable() {
                @Override
                public int arity() { return 0; }

                @Override
                public Object call(Interpreter interpreter,
                                   List<Object> arguments) {
                    return reader.nextDouble();
                }

                @Override
                public String toString() { return "<native fn>"; }
            });
        }
        public void defineUserInputString(){
            globals.define("readString", new LoxCallable() {
                @Override
                public int arity() { return 0; }

                @Override
                public Object call(Interpreter interpreter,
                                   List<Object> arguments) {
                    double input = (double)arguments.get(0);
                    return reader.nextLine();
                }

                @Override
                public String toString() { return "<native fn>"; }
            });
        }
        public void defineUserInputBoolean(){
            globals.define("readBoolean", new LoxCallable() {
                @Override
                public int arity() { return 0; }

                @Override
                public Object call(Interpreter interpreter,
                                   List<Object> arguments) {
                    return reader.nextBoolean();
                }

                @Override
                public String toString() { return "<native fn>"; }
            });
        }
}
