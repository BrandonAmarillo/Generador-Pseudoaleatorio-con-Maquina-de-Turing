package com.unpsjb.machineturing.model;

import java.awt.Color;

//Representación de celda de la cinta
public class TapeCell {
    private char symbol;
    private boolean isHead;
    private Color color;

    public TapeCell(char symbol) {
        this.symbol = symbol;
        this.isHead = false;
        this.color = Color.WHITE;
    }

    public char getSymbol() {
        return symbol;
    }

    public void setSymbol(char symbol) {
        this.symbol = symbol;
    }

    public boolean isHead() {
        return isHead;
    }

    public void setHead(boolean isHead) {
        this.isHead = isHead;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

}