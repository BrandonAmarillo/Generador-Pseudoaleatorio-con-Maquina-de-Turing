package com.unpsjb.machineturing.model;

import java.util.ArrayList;
import java.util.List;

import java.awt.Color;

/**
 * Clase que representa la cinta
 */
public class TuringTape {
    
    private List<TapeCell> cells;
    private int headPosition;
    private static final char BLANK = '▲';
    
    public TuringTape(String seed) {
        cells = new ArrayList<>();
        cells.add(new TapeCell(BLANK));
        
        for (char c : seed.toCharArray()) {
            cells.add(new TapeCell(c));
        }
        
        // Agregar blancos al final
        for (int i = 0; i < 50; i++) {
            cells.add(new TapeCell(BLANK));
        }
        
        headPosition = 1;
        updateHead();
    }

    public void moveRight() {
        if (headPosition < cells.size() - 1) {
            headPosition++;
            updateHead();
        }
    }
    
    public void moveLeft() {
        if (headPosition > 0) {
            headPosition--;
            updateHead();
        }
    }
    
    public char read() {
        return cells.get(headPosition).getSymbol();
    }
    
    public void write(char symbol) {
        cells.get(headPosition).setSymbol(symbol);
    }
    
    public int getHeadPosition() {
        return headPosition;
    }
    
    public void setHeadPosition(int pos) {
        headPosition = pos;
        updateHead();
    }
    
    private void updateHead() {
        for (int i = 0; i < cells.size(); i++) {
            cells.get(i).setHead(i == headPosition);
        }
    }
    
    public void setColorAt(int pos, Color color) {
        if (pos >= 0 && pos < cells.size()) {
            cells.get(pos).setColor(color);
        }
    }
    
    public void clearColors() {
        for (TapeCell cell : cells) {
            cell.setColor(Color.WHITE);
        }
    }
    
    public List<TapeCell> getCells() {
        return cells;
    }
    
    public int findNextBlank(int startPos) {
        for (int i = startPos; i < cells.size(); i++) {
            if (cells.get(i).getSymbol() == BLANK) {
                return i;
            }
        }
        return cells.size() - 1;
    }
    
    public String readSection(int start, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < start + length && i < cells.size(); i++) {
            char c = cells.get(i).getSymbol();
            if (c != BLANK) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
   
    
    
}
