package com.unpsjb.machineturing.service;

import java.util.ArrayList;
import java.util.List;


import java.awt.Color;
import com.unpsjb.machineturing.model.State;
import com.unpsjb.machineturing.model.TuringTape;


public class MachineTuring {
   private TuringTape tape;
    private int a, b, c;
    private int bitLength;
    private List<String> generatedValues;
    private boolean cycleDetected;
    private String currentState;
    private List<String> executionLog;
    
    // Posiciones en la cinta
    private int seedStart;
    private int workStart;
    private int resultStart;
    private int compareStart;

    private int phaseCounter; // Para saber en qué fase estamos (A, B o C)

    private State state;
    private State nextState;
    private State returnState;
    
    private char tempChar;
    private int counter;
    private int readPos1, readPos2, writePos;
    private int currentIteration;
    private String currentOp;

   public MachineTuring(String seed, int a, int b, int c){
        this.tape = new TuringTape(seed);
        this.a = a;
        this.b = b;
        this.c = c;
        this.bitLength = seed.length();
        this.generatedValues = new ArrayList<>();
        this.executionLog = new ArrayList<>();
        
        this.seedStart = 1;
        this.workStart = seedStart + bitLength + 1;
        this.resultStart = workStart + bitLength + 1;
        this.compareStart = resultStart + bitLength + 1;
        
        this.state = State.INIT;
        this.counter = 0;
        this.phaseCounter = 0;
        this.currentIteration = 0;
        this.cycleDetected = false;
        
        generatedValues.add(seed);
        executionLog.add("x₀ = " + seed);
        currentState = "Inicializado con semilla: " + seed;
   }

   public boolean step() {
        if (state == State.HALT) {
            return false;
        }
        
        tape.clearColors();
        
        switch (state) {
            case INIT:
                currentState = "Iniciando iteración " + (currentIteration + 1);
                executionLog.add("--- Iteración " + (currentIteration + 1) + " ---");
                state = State.COPY_READ;
                counter = 0;
                
                if(currentIteration == 0){
                    readPos1 = seedStart;
                } else {
                    readPos1 = seedStart;
                    for(int i = 0; i < currentIteration; i++){
                        readPos1 = tape.findNextBlank(readPos1) + 1;
                    }
                }
                // Escribir despues del último blank
                writePos = tape.findNextBlank(readPos1 + bitLength) + 1;

                tape.setHeadPosition(readPos1);
                return true;
                
            case COPY_READ:
                tape.setHeadPosition(readPos1 + counter);
                tempChar = tape.read();
                tape.setColorAt(tape.getHeadPosition(), Color.YELLOW);
                currentState = "Copiando bit: " + tempChar + " de posición " + (readPos1 + counter);
                state = State.COPY_WRITE;
                return true;
                
            case COPY_WRITE:
                tape.setHeadPosition(writePos + counter);
                tape.write(tempChar);
                tape.setColorAt(tape.getHeadPosition(), Color.GREEN);
                currentState = "Escribiendo bit: " + tempChar;
                counter++;
                
                if (counter < bitLength) {
                    state = State.COPY_READ;
                    tape.setHeadPosition(readPos1 + counter);
                } else {
                    executionLog.add("Copia completa: " + tape.readSection(writePos, bitLength));
                    // Comenzar con x << a
                    readPos1 = writePos;
                    writePos = tape.findNextBlank(readPos1 + bitLength) + 1;
                    
                    state = State.SHIFT_LEFT_A_DELETE;
                    counter = 0;
                    currentOp = "x << " + a;
                    phaseCounter = 1; // Fase A
                }
                return true;
                
            case COPY_MOVE_READ:
                tape.setHeadPosition(readPos1 + counter);
                state = State.COPY_READ;
                return true;
                
            case COPY_MOVE_WRITE:
                tape.setHeadPosition(writePos + counter);
                state = State.COPY_WRITE;
                return true;
                
            case SHIFT_LEFT_A_DELETE:
                currentState = "Shift left A: copiando desplazamiento a nueva posición";
                
                // Mover todos los bits a la izquierda
                for (int i = 0; i < bitLength - a; i++) {
                    char bit = tape.getCells().get(readPos1 + a + i).getSymbol();
                    tape.getCells().get(writePos + i).setSymbol(bit);
                    tape.setColorAt(writePos + i, Color.ORANGE);
                }
                
                counter = 0;
                state = State.SHIFT_LEFT_A_ADD_ZERO;
                return true;
                
            case SHIFT_LEFT_A_ADD_ZERO:
                if (counter < a) {
                    tape.setHeadPosition(writePos + bitLength - a + counter);
                    tape.write('0');
                    tape.setColorAt(tape.getHeadPosition(), Color.CYAN);
                    currentState = "Shift left A: agregando cero en pisición " + tape.getHeadPosition();
                    counter++;
                    return true;
                } else {
                    String shifted = tape.readSection(writePos, bitLength);
                    executionLog.add(currentOp + " = " + shifted);
                    
                    // Preparar XOR con A
                    readPos2 = writePos;

                    writePos = tape.findNextBlank(readPos2 + bitLength) + 1;

                    counter = 0;
                    state = State.XOR_A_COMPARE;
                    currentState = "Preparando XOR A";
                    return true;
                }
                
            case XOR_A_COMPARE:
                if (counter < bitLength) {
                    char bit1 = tape.getCells().get(readPos1 + counter).getSymbol();
                    char bit2 = tape.getCells().get(readPos2 + counter).getSymbol();
                    char result = (bit1 == bit2) ? '0' : '1';
                    
                    tape.setHeadPosition(readPos1 + counter);
                    tape.setColorAt(tape.getHeadPosition(), Color.YELLOW);
                    tape.setColorAt(readPos2 + counter, Color.ORANGE);
                    
                    currentState = "XOR A: " + bit1 + " ⊕ " + bit2 + " = " + result;
                    
                    tape.getCells().get(writePos + counter).setSymbol(result);
                    tape.setColorAt(writePos + counter, Color.GREEN);
                    
                    counter++;
                    return true;
                } else {
                    String xorResult = tape.readSection(writePos, bitLength);
                    executionLog.add("x XOR (x << a) = " + xorResult);
                    
                    // Actualizar x para siguiente operación
                    readPos1 = writePos;

                    writePos = tape.findNextBlank(readPos1 + bitLength) + 1;
                    
                    // Continuar con x >> b
                    state = State.SHIFT_RIGHT_B_DELETE;
                    counter = 0;
                    currentOp = "x >> " + b;
                    phaseCounter = 2; // Fase B
                    return true;
                }
                
            case SHIFT_RIGHT_B_DELETE:
                currentState = "Shift right B: copiando desplazamiento a nueva posición";
                
                // Mover todos los bits a la derecha
                for (int i = bitLength - 1; i >= b; i--) {
                    char bit = tape.getCells().get(readPos1 + i - b).getSymbol();
                    tape.getCells().get(writePos + i).setSymbol(bit);
                    tape.setColorAt(writePos + i,Color.ORANGE);
                }
                
                counter = 0;
                state = State.SHIFT_RIGHT_B_ADD_ZERO;
                return true;
                
            case SHIFT_RIGHT_B_ADD_ZERO:
                if (counter < b) {
                    tape.setHeadPosition(writePos + counter);
                    tape.write('0');
                    tape.setColorAt(tape.getHeadPosition(), Color.CYAN);
                    currentState = "Shift right B: agregando cero al inicio";
                    counter++;
                    return true;
                } else {
                    String shifted = tape.readSection(writePos, bitLength);
                    executionLog.add(currentOp + " = " + shifted);
                    
                    // Preparar XOR con B
                    readPos2 = writePos;
                    writePos = tape.findNextBlank(readPos2 + bitLength) + 1;
                    
                    counter = 0;
                    state = State.XOR_B_COMPARE;
                    currentState = "Preparando XOR B";
                    return true;
                }
                
            case XOR_B_COMPARE:
                if (counter < bitLength) {
                    char bit1 = tape.getCells().get(readPos1 + counter).getSymbol();
                    char bit2 = tape.getCells().get(readPos2 + counter).getSymbol();
                    char result = (bit1 == bit2) ? '0' : '1';
                    

                    tape.setColorAt(tape.getHeadPosition(), Color.YELLOW);
                    tape.setColorAt(readPos2 + counter, Color.ORANGE);
                    
                    currentState = "XOR B: " + bit1 + " ⊕ " + bit2 + " = " + result;
                    
                    tape.getCells().get(writePos + counter).setSymbol(result);
                    tape.setColorAt(writePos + counter, Color.GREEN);
                    
                    counter++;
                    return true;
                } else {
                    String xorResult = tape.readSection(writePos, bitLength);
                    executionLog.add("x XOR (x >> b) = " + xorResult);
                    
                    // Actualizar x para siguiente operación
                    readPos1 = writePos;
                    writePos = tape.findNextBlank(readPos1 + bitLength) + 1;
                    
                    // Continuar con x << c
                    state = State.SHIFT_LEFT_C_DELETE;
                    counter = 0;
                    currentOp = "x << " + c;
                    phaseCounter = 3; // Fase C
                    return true;
                }
                
            case SHIFT_LEFT_C_DELETE:
                currentState = "Shift left C: copiando desplazamiento a nueva posición";
                
                // Mover todos los bits a la izquierda
                for (int i = 0; i < bitLength - c; i++) {
                    char bit = tape.getCells().get(readPos1 + c + i).getSymbol();
                    tape.getCells().get(writePos + i).setSymbol(bit);
                    tape.setColorAt(writePos + i, Color.ORANGE);
                }
                
                counter = 0;
                state = State.SHIFT_LEFT_C_ADD_ZERO;
                return true;
                
            case SHIFT_LEFT_C_ADD_ZERO:
                if (counter < c) {
                    tape.setHeadPosition(writePos + bitLength - c + counter);
                    tape.write('0');
                    tape.setColorAt(tape.getHeadPosition(), Color.CYAN);
                    currentState = "Shift left C: agregando cero";
                    counter++;
                    return true;
                } else {
                    String shifted = tape.readSection(writePos, bitLength);
                    executionLog.add(currentOp + " = " + shifted);
                    
                    // Preparar XOR final con C
                    readPos2 = writePos;
                    writePos = tape.findNextBlank(readPos2 + bitLength) + 1;
                    counter = 0;
                    state = State.XOR_C_COMPARE;
                    currentState = "Preparando XOR C (final)";
                    return true;
                }
                
            case XOR_C_COMPARE:
                if (counter < bitLength) {
                    char bit1 = tape.getCells().get(readPos1 + counter).getSymbol();
                    char bit2 = tape.getCells().get(readPos2 + counter).getSymbol();
                    char result = (bit1 == bit2) ? '0' : '1';
                    
                    
                    tape.setColorAt(tape.getHeadPosition(), Color.YELLOW);
                    tape.setColorAt(readPos2 + counter, Color.ORANGE);
                    
                    currentState = "XOR C (final): " + bit1 + " ⊕ " + bit2 + " = " + result;
                    
                    tape.getCells().get(writePos + counter).setSymbol(result);
                    tape.setColorAt(writePos + counter, Color.GREEN);
                    
                    counter++;
                    return true;
                } else {
                    String xorResult = tape.readSection(writePos, bitLength);
                    executionLog.add("x XOR (x << c) = " + xorResult + " (RESULTADO FINAL x " + (currentIteration + 1) + ")");
                    
                    currentIteration++;
                    state = State.COMPARE_VALUES;
                    counter = 0;
                    
                    // write tiene el resultaado final
                    readPos2 = writePos;
                    
                    return true;
                }
                
            case COMPARE_VALUES:
                if (counter < generatedValues.size()) {
                    int comparePos = seedStart;
                    for(int i = 0; i < counter; i++){
                        comparePos = tape.findNextBlank(comparePos) + 1;
                        // Salta todas las operaciones intermedias
                        while (comparePos < tape.getCells().size() && 
                               tape.getCells().get(comparePos).getSymbol() != '▲' &&
                               !isValidBinaryStart(comparePos)) {
                                comparePos ++;
                               }
                    }

                    String current = tape.readSection(readPos2,bitLength);
                    String previous = generatedValues.get(counter);

                    tape.setColorAt(readPos2,Color.MAGENTA);
                    if(comparePos < tape.getCells().size()){
                        tape.setColorAt(comparePos,Color.CYAN);
                    }

                    currentState = "Comparando X " + (currentIteration) + ": " + current + "vs X " + counter + ": " + previous;
                    executionLog.add("Comparando con x " + counter + " = " + previous);
                                        
                    if (current.equals(previous)) {
                        cycleDetected = true;
                        state = State.CYCLE_DETECTED;
                        executionLog.add("¡CICLO DETECTADO!");
                        executionLog.add("Período = " + generatedValues.size());
                        executionLog.add("Valor repetido: " + current + " (x" + counter + ")");
                        return true;
                    }
                    
                    counter++;
                    return true;
                } else {
                    String newValue = tape.readSection(readPos2, bitLength);
                    generatedValues.add(newValue);
                    executionLog.add("x" + currentIteration + " = " + newValue + " (nuevo)");
                    executionLog.add("");

                    state = State.INIT;
                    return true;
                }
                
            case CYCLE_DETECTED:
                currentState = "¡CICLO DETECTADO! Período: " + generatedValues.size();
                state = State.HALT;
                return false;
                
            default:
                return false;
        }
    }

    private boolean isValidBinaryStart(int pos) {
        if (pos >= tape.getCells().size()) return false;
        char c = tape.getCells().get(pos).getSymbol();
        return c == '0' || c == '1';
    }

   public TuringTape getTape() {
        return tape;
    }
    
    public boolean isCycleDetected() {
        return cycleDetected;
    }
    
    public int getCycleLength() {
        return generatedValues.size();
    }
    
    public List<String> getSequence() {
        return new ArrayList<>(generatedValues);
    }
    
    public String getCurrentState() {
        return currentState;
    }
    
    public List<String> getExecutionLog() {
        return new ArrayList<>(executionLog);
    }
}
