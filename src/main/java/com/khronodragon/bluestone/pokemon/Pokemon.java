package com.khronodragon.bluestone.pokemon;

import com.google.gson.annotations.SerializedName;

public class Pokemon {
    private int nationalId;
    private String name;
    private String resourceUri;
    private Ability[] abilities;
    private EggGroup[] eggGroups;
    private Evolution[] evolutions;
    private Description[] descriptions;
    private Move[] moves;
    private Type[] types;
    private int catchRate;
    private String species;
    private int hp;
    private int attack;
    private int defense;
    @SerializedName("sp_atk")
    private int specialAttack;
    @SerializedName("sp_def")
    private int specialDefense;
    private int speed;
    private int total;
    private int eggCycles;
    @SerializedName("ev_yield")
    private Integer effortValueYield;
    private int exp;

    public int getNationalId() {
        return nationalId;
    }

    public String getName() {
        return name;
    }

    public String getResourceUri() {
        return resourceUri;
    }

    public Ability[] getAbilities() {
        return abilities;
    }

    public EggGroup[] getEggGroups() {
        return eggGroups;
    }

    public Evolution[] getEvolutions() {
        return evolutions;
    }

    public Description[] getDescriptions() {
        return descriptions;
    }

    public Move[] getMoves() {
        return moves;
    }

    public Type[] getTypes() {
        return types;
    }

    public int getCatchRate() {
        return catchRate;
    }

    public String getSpecies() {
        return species;
    }

    public int getHp() {
        return hp;
    }

    public int getAttack() {
        return attack;
    }

    public int getDefense() {
        return defense;
    }

    public int getSpecialAttack() {
        return specialAttack;
    }

    public int getSpecialDefense() {
        return specialDefense;
    }

    public int getSpeed() {
        return speed;
    }

    public int getTotal() {
        return total;
    }

    public int getEggCycles() {
        return eggCycles;
    }

    public int getEffortValueYield() {
        return effortValueYield;
    }

    public int getExp() {
        return exp;
    }

    public String getGrowthRate() {
        return growthRate;
    }

    public int getHeight() {
        return height;
    }

    public int getWeight() {
        return weight;
    }

    public int getHappiness() {
        return happiness;
    }

    public String getMaleFemaleRatio() {
        return maleFemaleRatio;
    }

    public Sprite[] getSprites() {
        return sprites;
    }

    private String growthRate;
    private int height; // string
    private int weight; // string
    private int happiness;
    private String maleFemaleRatio;
    private Sprite[] sprites;
}
