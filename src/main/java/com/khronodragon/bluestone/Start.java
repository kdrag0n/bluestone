package com.khronodragon.bluestone;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Start {
    static void print(String text) {
        System.out.println(text);
    }

    public static void main(String[] args) {
        print("Welcome to Bluestone!");

        String jsonCode = new String(Files.readAllBytes(Paths.get("auth.json")));

    }
}
