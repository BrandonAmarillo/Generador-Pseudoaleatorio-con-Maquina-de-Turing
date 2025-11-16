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

    private int shiftCounter; // Para contar cuántos shifts individuales hamos hechos
    private int shiftAmount; // Cuántos shifts totales necesitamos hacer
    
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
        this.shiftCounter = 0;
        this.shiftAmount = 0;
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

                // Leer desde la última posición escrita (o semilla inicial)
                if(currentIteration == 0){
                    readPos1 = seedStart;
                } else {
                    // Busca la última posición escrita
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
                currentState = "Escribiendo bit: " + tempChar + " posición " + (writePos + counter);
                counter++;
                
                if (counter < bitLength) {
                    state = State.COPY_READ;
                    tape.setHeadPosition(readPos1 + counter);
                } else {
                    executionLog.add("Copia completa: " + tape.readSection(writePos, bitLength));
                    // Comenzar con x << a
                    readPos1 = writePos;
                    
                    state = State.SHIFT_LEFT_A_START;
                    counter = 0;
                    shiftCounter = 0;
                    shiftAmount = a;
                    currentOp = "x << " + a;
                    phaseCounter = 1; // Fase A
                }
                return true;
                
            case SHIFT_LEFT_A_START:
                if(shiftCounter < shiftAmount){
                    // Iniciar un shift individual
                    currentState = "Shift left A: (" + (shiftCounter + 1) + "/" + shiftAmount + "): Iniciando";
                    executionLog.add("Shift left " + (shiftCounter + 1) + "/" + shiftAmount);

                    counter = 0;
                    tape.setHeadPosition(readPos1); // Posición más a la izquierda
                    state = State.SHIFT_LEFT_A_READ;
                } else {
                    // Terminamos todos los shifts
                    String shifted = tape.readSection(readPos1, bitLength);
                    executionLog.add(currentOp + " = " + shifted);

                    //Preparar XOR: copiar a nueva posición
                    writePos = tape.findNextBlank(readPos1 + bitLength) + 1;

                    //Copiar el resultado del shift a writePos para XOR
                    for(int i = 0; i < bitLength; i++) {
                        tape.getCells().get(writePos + i).setSymbol(tape.getCells().get(readPos1 + i).getSymbol());
                    }

                    readPos2 = writePos; // (x << a)
                    // readPos1 tiene x original (necesitamos recuperarlo)
                    // Buscar x original (la primera sección después del blank inicial)
                    if(currentIteration == 0){
                        readPos1 = seedStart;
                    } else {
                        readPos1 = seedStart;
                        for(int i = 0; i < currentIteration; i++){
                            readPos1 = tape.findNextBlank(readPos1) + 1;
                        }
                    }
                    writePos = tape.findNextBlank(readPos2 + bitLength) + 1;
                    counter = 0;
                    state = State.XOR_A_COMPARE;
                    currentState = "Preparando XOR A";
                }
                
                return true;
                
            case SHIFT_LEFT_A_READ:
                if (counter < bitLength - 1) {
                    // Leer el bit siguiente (a + 1 posición adelante)
                    tape.setHeadPosition(readPos1 + counter + 1);
                    tempChar = tape.read();
                    tape.setColorAt(tape.getHeadPosition(), Color.YELLOW);
                    currentState = "Shift left A: agregando cero en pisición " + tape.getHeadPosition();
                    state = State.SHIFT_LEFT_A_WRITE;
                } else {
                    counter = 0;
                    state = State.SHIFT_LEFT_A_FILL_ZERO;
                }
                return true;
                
            case SHIFT_LEFT_A_WRITE:
                // Escribir (pisar) en la posición actual
                tape.setHeadPosition(readPos1 + counter);
                tape.write(tempChar);
                tape.setColorAt(tape.getHeadPosition(), Color.ORANGE);
                currentState = "Pisando posición " + (readPos1 + counter) + " con: " + tempChar;
                counter++;
                state = State.SHIFT_LEFT_A_READ;
                return true;
            case SHIFT_LEFT_A_FILL_ZERO:
                // Llenar el útimo bit con 0
                tape.setHeadPosition(readPos1 + bitLength - 1);
                if (tape.read() != '▲') {
                    tape.write('0');
                    tape.setColorAt(tape.getHeadPosition(), Color.CYAN);
                    currentState = "Rellenando con 0 en posición " + (readPos1 + bitLength - 1);
                }
                
                shiftCounter++;
                state = State.SHIFT_LEFT_A_START; // Repetir si hay más shifts
                return true;
            
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
                    
                    // Continuar con x >> b
                    state = State.SHIFT_RIGHT_B_START;
                    counter = 0;
                    shiftCounter = 0;
                    shiftAmount = b;
                    currentOp = "x >> " + b;
                    return true;
                }
                
            case SHIFT_RIGHT_B_START:
                if(shiftCounter < shiftAmount) {
                    // Iniciar un shift individual a la derecha
                    currentState = "Shift right B (" + (shiftCounter + 1) + "/" + shiftAmount + "): Iniciando";
                    executionLog.add("Shift right " + (shiftCounter + 1) + "/" + shiftAmount);
                    counter = bitLength - 1; // Empezar desde la derecha
                    tape.setHeadPosition(readPos1 + bitLength - 1);
                    state = State.SHIFT_RIGHT_B_READ;
                } else {
                    // Terminar todos los shifts
                    String shifted = tape.readSection(readPos1, bitLength);
                    executionLog.add(currentOp + " = " + shifted);

                    //Preparar XOR B: copiar a nueva posición
                    writePos = tape.findNextBlank(readPos1 + bitLength) + 1;

                    for(int i = 0; i <bitLength; i++){
                        tape.getCells()
                        .get(writePos + i)
                        .setSymbol(tape
                            .getCells()
                            .get(readPos1 + i)
                            .getSymbol()
                        );
                    }

                    readPos2 = writePos; // (x >> b)
                    // readPos1 tiene el resultado anterior del XOR A
                    writePos = tape.findNextBlank(readPos2 + bitLength) + 1;
                    counter = 0;
                    state = State.XOR_B_COMPARE;
                    currentState = "Preparando XOR B";
                }

                return true;
            case SHIFT_RIGHT_B_READ:
                if (counter > 0) {
                    // Leer el bit anterior
                    tape.setHeadPosition(readPos1 + counter - 1);
                    tempChar = tape.read();
                    tape.setColorAt(tape.getHeadPosition(), Color.YELLOW);
                    currentState = "Leyendo bit: " + tempChar + " de pos " + (readPos1 + counter - 1);
                    state = State.SHIFT_RIGHT_B_WRITE;
                } else {
                    // Ya movimos todos los bits, rellenar con cero al inicio
                    counter = 0;
                    state = State.SHIFT_RIGHT_B_FILL_ZERO;
                }
                return true;
                
            case SHIFT_RIGHT_B_WRITE:
                // Escribir (pisar) en la posición actual
                tape.setHeadPosition(readPos1 + counter);
                tape.write(tempChar);
                tape.setColorAt(tape.getHeadPosition(), Color.ORANGE);
                currentState = "Pisando posición " + (readPos1 + counter) + " con: " + tempChar;
                counter--;
                state = State.SHIFT_RIGHT_B_READ;
                return true; 
                     
            case SHIFT_RIGHT_B_FILL_ZERO:
                // Llenar el primer bit con 0
                tape.setHeadPosition(readPos1);
                tape.write('0');
                tape.setColorAt(tape.getHeadPosition(), Color.CYAN);
                currentState = "Rellenando con 0 en posición " + readPos1;
                
                shiftCounter++;
                state = State.SHIFT_RIGHT_B_START; // Repetir si hay más shifts
                return true;
                
            case XOR_B_COMPARE:
                if (counter < bitLength) {
                    char bit1 = tape.getCells().get(readPos1 + counter).getSymbol();
                    char bit2 = tape.getCells().get(readPos2 + counter).getSymbol();
                    char result = (bit1 == bit2) ? '0' : '1';
                    
                    tape.setColorAt(readPos1 + counter, Color.YELLOW);
                    tape.setColorAt(readPos2 + counter, Color.ORANGE);
                    
                    currentState = "XOR B: " + bit1 + " ⊕ " + bit2 + " = " + result;
                    
                    tape.getCells().get(writePos + counter).setSymbol(result);
                    tape.setColorAt(writePos + counter, Color.GREEN);
                    
                    counter++;
                    return true;
                } else {
                    String xorResult = tape.readSection(writePos, bitLength);
                    executionLog.add("x XOR (x >> b) = " + xorResult);
                    
                    // El resultado es el nuevo x
                    readPos1 = writePos;
                    
                    // Continuar con x << c
                    state = State.SHIFT_LEFT_C_START;
                    counter = 0;
                    shiftCounter = 0;
                    shiftAmount = c;
                    currentOp = "x << " + c;
                    return true;
                }
                
            case SHIFT_LEFT_C_START:
                if (shiftCounter < shiftAmount) {
                    // Iniciar un shift individual
                    currentState = "Shift left C (" + (shiftCounter + 1) + "/" + shiftAmount + "): Iniciando";
                    executionLog.add("Shift left " + (shiftCounter + 1) + "/" + shiftAmount);
                    counter = 0;
                    tape.setHeadPosition(readPos1);
                    state = State.SHIFT_LEFT_C_READ;
                } else {
                    // Terminamos todos los shifts
                    String shifted = tape.readSection(readPos1, bitLength);
                    executionLog.add(currentOp + " = " + shifted);
                    
                    // Preparar XOR final
                    writePos = tape.findNextBlank(readPos1 + bitLength) + 1;
                    
                    for (int i = 0; i < bitLength; i++) {
                        tape.getCells().get(writePos + i).setSymbol(tape.getCells().get(readPos1 + i).getSymbol());
                    }
                    
                    readPos2 = writePos;  // (x << c)
                    writePos = tape.findNextBlank(readPos2 + bitLength) + 1;
                    counter = 0;
                    state = State.XOR_C_COMPARE;
                    currentState = "Preparando XOR C (final)";
                }
                return true;
                
            case SHIFT_LEFT_C_READ:
                if (counter < bitLength - 1) {
                    tape.setHeadPosition(readPos1 + counter + 1);
                    tempChar = tape.read();
                    tape.setColorAt(tape.getHeadPosition(), Color.YELLOW);
                    currentState = "Leyendo bit: " + tempChar + " de pos " + (readPos1 + counter + 1);
                    state = State.SHIFT_LEFT_C_WRITE;
                } else {
                    counter = 0;
                    state = State.SHIFT_LEFT_C_FILL_ZERO;
                }
                return true;
                
            case SHIFT_LEFT_C_WRITE:
                tape.setHeadPosition(readPos1 + counter);
                tape.write(tempChar);
                tape.setColorAt(tape.getHeadPosition(), Color.ORANGE);
                currentState = "Pisando posición " + (readPos1 + counter) + " con: " + tempChar;
                counter++;
                state = State.SHIFT_LEFT_C_READ;
                return true;
                
            case SHIFT_LEFT_C_FILL_ZERO:
                tape.setHeadPosition(readPos1 + bitLength - 1);
                if (tape.read() != '▲') {
                    tape.write('0');
                    tape.setColorAt(tape.getHeadPosition(), Color.CYAN);
                    currentState = "Rellenando con 0 en posición " + (readPos1 + bitLength - 1);
                }
                
                shiftCounter++;
                state = State.SHIFT_LEFT_C_START;
                return true;
                
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
                    
                    // Este es el resultado final de esta iteración
                    // Ahora debemos comparar con TODOS los valores generados anteriormente
                    currentIteration++;
                    state = State.COMPARE_VALUES;
                    counter = 0;
                    
                    // write tiene el resultaado final
                    readPos2 = writePos; // El nuevo valor a comparar
                    
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
