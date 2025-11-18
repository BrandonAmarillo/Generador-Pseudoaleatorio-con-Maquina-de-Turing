package com.unpsjb.machineturing.service;

import java.util.ArrayList;
import java.util.List;


import java.awt.Color;
import com.unpsjb.machineturing.model.State;
import com.unpsjb.machineturing.model.TuringTape;

// Máquina de Turing XOR-Shift
public class MachineTuring {
   private TuringTape tape;
    private int a, b, c;
    private int bitLength;
    private List<String> generatedValues;
    private boolean cycleDetected;
    private String currentState;
    private List<String> executionLog;

    private State state;
    private char tempChar;
    private int counter;
    private int readPos1, readPos2, writePos;
    private int currentIteration;
    private String currentOp;

    //Control de flujo de operaciones
    private int phaseStep;
    private int shiftCounter;
    private int shiftAmount;
    private boolean isShiftLeft;
    private int xorBitPos;
    private char markedBit;
    private int xorStartPos, xorComparePos;
    private int copySourcePos, copyTargetPos; // PAra copiar con marcadores
    private int blanksSeen; // Contador de ▲s vistos durante la búsqueda


   public MachineTuring(String seed, int a, int b, int c){
        this.tape = new TuringTape(seed);
        this.a = a;
        this.b = b;
        this.c = c;
        this.bitLength = seed.length();
        this.generatedValues = new ArrayList<>();
        this.executionLog = new ArrayList<>();
        
        this.state = State.INIT;
        this.counter = 0;
        this.currentIteration = 0;
        this.cycleDetected = false;
        this.phaseStep = 0;
        
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
                
                // Buscar última posición escrita
                if (currentIteration == 0) {
                    readPos1 = 1; // Semilla inicial
                } else {
                    readPos1 = 1;
                    for (int i = 0; i < currentIteration; i++) {
                        readPos1 = tape.findNextBlank(readPos1) + 1;
                    }
                }
                
                copySourcePos = readPos1;
                
                copyTargetPos = tape.findNextBlank(readPos1 + bitLength) + 1;
                counter = 0;
                phaseStep = 0;
                executionLog.add("Copia inicial con marcadores");
                state = State.PRE_SHIFT_COPY_MARK;
                return true;
                
            case COPY_READ:
                tape.setHeadPosition(readPos1 + counter);
                tempChar = tape.read();
                tape.setColorAt(tape.getHeadPosition(), Color.YELLOW);
                currentState = "Copiando: Leyendo '" + tempChar + "' de pos " + (readPos1 + counter);
                state = State.COPY_WRITE;
                return true;
                
            case COPY_WRITE:
                tape.setHeadPosition(writePos + counter);
                tape.write(tempChar);
                tape.setColorAt(tape.getHeadPosition(), Color.GREEN);
                currentState = "Copiando: Escribiendo '" + tempChar + "' en pos " + (writePos + counter);
                counter++;
                
                if (counter < bitLength) {
                    state = State.COPY_READ;
                } else {
                    String copied = tape.readSection(writePos, bitLength);
                    
                    if (phaseStep == 0) {
                        // Copia inicial completa, ahora copiar con marcadores antes de shift A
                        executionLog.add("Copia inicial: " + copied);
                        readPos1 = writePos; // Segunda posición (la que vamos a shiftear)
                        copySourcePos = writePos;
                        copyTargetPos = tape.findNextBlank(writePos + bitLength) + 1;
                        counter = 0;
                        phaseStep = 1;
                        state = State.PRE_SHIFT_COPY_MARK;
                    } else {
                        executionLog.add("ERROR: COPY_WRITE en fase inesperada");
                    }
                }
                return true;
                
            case PRE_SHIFT_COPY_MARK:
                if (counter < bitLength) {
                    tape.setHeadPosition(copySourcePos + counter);
                    markedBit = tape.read();
                    
                    if (markedBit == '▲' || markedBit == '#') {
                        // Terminamos la copia con marcadores, restaurar valores
                        counter = 0;
                        tape.setHeadPosition(copySourcePos);
                        state = State.PRE_SHIFT_COPY_RESTORE;
                        return true;
                    }
                    
                    tape.write('#');
                    tape.setColorAt(tape.getHeadPosition(), Color.RED);
                    currentState = "Pre-shift: Marcando bit " + counter + " ('" + markedBit + "') con # en pos " + tape.getHeadPosition();
                    blanksSeen = 0; // Reiniciar contador de blancos
                    state = State.PRE_SHIFT_COPY_FIND_TARGET;
                } else {
                    counter = 0;
                    tape.setHeadPosition(copySourcePos);
                    state = State.PRE_SHIFT_COPY_RESTORE;
                }
                return true;
                
            case PRE_SHIFT_COPY_FIND_TARGET:
                // Avanzar paso a paso hasta llegar a copyTargetPos + counter
                tape.moveRight();
                int currentPos = tape.getHeadPosition();
                int targetWritePos = copyTargetPos + counter;
                
                currentState = "Pre-shift: Avanzando hacia pos " + targetWritePos + ", actual: " + currentPos;
                tape.setColorAt(currentPos, Color.CYAN);
                
                if (currentPos >= targetWritePos) {
                    // Llegamos a la posición de escritura
                    tape.setHeadPosition(targetWritePos);
                    currentState = "Pre-shift: Llegamos a posición de escritura " + targetWritePos;
                    state = State.PRE_SHIFT_COPY_WRITE_BIT;
                }
                return true;
                
            case PRE_SHIFT_COPY_WRITE_BIT:
                // Escribir el bit marcado en esta posición ▲
                if (tape.read() == '▲') {
                    tape.write(markedBit);
                    tape.setColorAt(tape.getHeadPosition(), Color.GREEN);
                    currentState = "Pre-shift: Escribiendo '" + markedBit + "' en pos " + tape.getHeadPosition();
                    state = State.PRE_SHIFT_COPY_RETURN;
                } else {
                    executionLog.add("ERROR: No se encontró ▲ para escribir en pos " + tape.getHeadPosition());
                    state = State.PRE_SHIFT_COPY_RETURN;
                }
                return true;
                
            case PRE_SHIFT_COPY_RETURN:
                // Retroceder hasta encontrar #
                tape.moveLeft();
                currentState = "Pre-shift: Retrocediendo a # desde pos " + tape.getHeadPosition();
                tape.setColorAt(tape.getHeadPosition(), Color.PINK);
                
                if (tape.read() == '#') {
                    // Restaurar el valor original
                    tape.write(markedBit);
                    tape.setColorAt(tape.getHeadPosition(), Color.ORANGE);
                    currentState = "Pre-shift: Restaurando '" + markedBit + "' en pos " + tape.getHeadPosition();
                    counter++;
                    state = State.PRE_SHIFT_COPY_MARK;
                }
                return true;
                
            case PRE_SHIFT_COPY_RESTORE:
                // Restaurar todos los # con sus valores originales
                if (counter < bitLength) {
                    tape.setHeadPosition(copySourcePos + counter);
                    
                    if (tape.read() == '#') {
                        // Este no debería tener #, algo salió mal
                        executionLog.add("ERROR: Encontrado # en restauración en pos " + tape.getHeadPosition());
                    }
                    
                    counter++;
                    return true;
                } else {
                    // Todos los bits restaurados
                    String copiedForShift = tape.readSection(copyTargetPos, bitLength);
                    
                    if (phaseStep == 0) {
                        // Copia inicial completa, ahora copiar con marcadores para shift A
                        executionLog.add("Copia inicial: " + copiedForShift);
                        readPos1 = copyTargetPos; // Segunda posición (la que vamos a shiftear)
                        copySourcePos = copyTargetPos;
                        copyTargetPos = tape.findNextBlank(copyTargetPos + bitLength) + 1;
                        counter = 0;
                        phaseStep = 1;
                        executionLog.add("Preparando copia para shift A");
                        state = State.PRE_SHIFT_COPY_MARK;
                    } else {
                        // Copia pre-shift completa
                        executionLog.add("Copia pre-shift completa: " + copiedForShift);
                        
                        readPos1 = copySourcePos; // La segunda posición (la que vamos a shiftear)
                        readPos2 = copyTargetPos; // La tercera posición (para comparar en XOR)
                        
                        // Determinar qué shift hacer según la fase
                        if (phaseStep == 1) {
                            prepareShift(a, true, "x << " + a);
                        } else if (phaseStep == 2) {
                            prepareShift(b, false, "x >> " + b);
                        } else if (phaseStep == 3) {
                            prepareShift(c, true, "x << " + c);
                        }
                    }
                }
                return true;
                
            case SHIFT_POSITION:
                // Moverse al inicio o final de la sección a shiftear según tipo de shift
                if (isShiftLeft) {
                    tape.setHeadPosition(readPos1);
                    currentState = "Posicionándose al inicio para shift left";
                } else {
                    tape.setHeadPosition(readPos1 + bitLength - 1);
                    currentState = "Posicionándose al final para shift right";
                }
                state = State.SHIFT_START;
                return true;
                
            case SHIFT_START:
                if (shiftCounter < shiftAmount) {
                    currentState = "Shift (" + (shiftCounter + 1) + "/" + shiftAmount + "): Iniciando";
                    counter = isShiftLeft ? 0 : (bitLength - 1);
                    tape.setHeadPosition(readPos1 + counter);
                    state = State.SHIFT_READ;
                } else {
                    String shifted = tape.readSection(readPos1, bitLength);
                    executionLog.add(currentOp + " = " + shifted);
                    
                    // Iniciar XOR
                    xorBitPos = 0;
                    xorStartPos = readPos1;
                    xorComparePos = readPos2;
                    tape.setHeadPosition(xorStartPos);
                    state = State.XOR_MARK;
                }
                return true;
                
            case SHIFT_READ:
                int nextPos = isShiftLeft ? (counter + 1) : (counter - 1);
                boolean shouldFillZero = isShiftLeft ? (nextPos >= bitLength) : (nextPos < 0);
                
                if (!shouldFillZero) {
                    tape.setHeadPosition(readPos1 + nextPos);
                    tempChar = tape.read();
                    tape.setColorAt(tape.getHeadPosition(), Color.YELLOW);
                    currentState = "Shift: Leyendo '" + tempChar + "' de pos " + (readPos1 + nextPos);
                    state = State.SHIFT_WRITE;
                } else {
                    state = State.SHIFT_FILL_ZERO;
                }
                return true;
                
            case SHIFT_WRITE:
                tape.setHeadPosition(readPos1 + counter);
                tape.write(tempChar);
                tape.setColorAt(tape.getHeadPosition(), Color.ORANGE);
                currentState = "Shift: Pisando pos " + (readPos1 + counter) + " con '" + tempChar + "'";
                counter = isShiftLeft ? (counter + 1) : (counter - 1);
                state = State.SHIFT_READ;
                return true;
                
            case SHIFT_FILL_ZERO:
                int zeroPos = isShiftLeft ? (readPos1 + bitLength - 1) : readPos1;
                tape.setHeadPosition(zeroPos);
                tape.write('0');
                tape.setColorAt(tape.getHeadPosition(), Color.CYAN);
                currentState = "Shift: Rellenando con 0 en pos " + zeroPos;
                shiftCounter++;
                state = State.SHIFT_START;
                return true;
                
            case XOR_MARK:
                if (xorBitPos < bitLength) {
                    tape.setHeadPosition(xorStartPos + xorBitPos);
                    markedBit = tape.read();
                    
                    if (markedBit == '▲' || markedBit == '#') {
                        finishXOR();
                        return true;
                    }
                    
                    tape.write('#');
                    tape.setColorAt(tape.getHeadPosition(), Color.RED);
                    currentState = "XOR: Marcando bit " + xorBitPos + " ('" + markedBit + "') con #";
                    state = State.XOR_FIND_BLANK;
                } else {
                    finishXOR();
                }
                return true;
                
            case XOR_FIND_BLANK:
                tape.moveRight();
                currentState = "XOR: Buscando ▲ en pos " + tape.getHeadPosition();
                
                if (tape.read() == '▲') {
                    tape.setColorAt(tape.getHeadPosition(), Color.CYAN);
                    state = State.XOR_FIND_COMPARE;
                }
                return true;
                
            case XOR_FIND_COMPARE:
                tape.moveRight();
                char c = tape.read();
                currentState = "XOR: Buscando bit de comparación en pos " + tape.getHeadPosition();
                
                if (c != '▲') {
                    tempChar = c;
                    tape.setColorAt(tape.getHeadPosition(), Color.YELLOW);
                    state = State.XOR_COMPARE_BIT;
                }
                return true;
                
            case XOR_COMPARE_BIT:
                tape.write('▲');
                tape.setColorAt(tape.getHeadPosition(), Color.PINK);
                currentState = "XOR: Reemplazando '" + tempChar + "' con ▲";
                state = State.XOR_RETURN;
                return true;
                
            case XOR_RETURN:
                tape.moveLeft();
                currentState = "XOR: Retrocediendo a # desde pos " + tape.getHeadPosition();
                
                if (tape.read() == '#') {
                    char result = (markedBit == tempChar) ? '0' : '1';
                    tape.write(result);
                    tape.setColorAt(tape.getHeadPosition(), Color.GREEN);
                    currentState = "XOR: " + markedBit + " ⊕ " + tempChar + " = " + result;
                    xorBitPos++;
                    state = State.XOR_MARK;
                }
                return true;
                
            case COMPARE_INIT:
                if (counter < generatedValues.size()) {
                    String current = tape.readSection(readPos1, bitLength);
                    String previous = generatedValues.get(counter);
                    
                    currentState = "Comparando x" + currentIteration + " con x" + counter;
                    executionLog.add("Comparando: " + current + " vs " + previous);
                    
                    tape.setColorAt(readPos1, Color.MAGENTA);
                    
                    if (current.equals(previous)) {
                        cycleDetected = true;
                        state = State.CYCLE_DETECTED;
                        executionLog.add("¡CICLO DETECTADO!");
                        executionLog.add("Período = " + generatedValues.size());
                        executionLog.add("Valor repetido: " + current + " (x" + counter + ")");
                        return true;
                    }
                    
                    counter++;
                } else {
                    String newValue = tape.readSection(readPos1, bitLength);
                    generatedValues.add(newValue);
                    executionLog.add("x" + currentIteration + " = " + newValue + " (nuevo)");
                    executionLog.add("");
                    
                    // COPIAR el resultado final para siguiente iteración
                    copySourcePos = readPos1;
                    copyTargetPos = tape.findNextBlank(readPos1 + bitLength) + 1;
                    counter = 0;
                    state = State.PRE_SHIFT_COPY_MARK; // Reutilizar la copia existente
                    phaseStep = 0; // Reiniciar fase
                    currentIteration++; // Incrementar iteración
                }
                return true;
                
            case CYCLE_DETECTED:
                currentState = "¡CICLO DETECTADO! Período: " + generatedValues.size();
                state = State.HALT;
                return false;
                
            default:
                return false;
        }
    }

    private void prepareShift(int amount, boolean isLeft, String operation) {
        shiftCounter = 0;
        shiftAmount = amount;
        isShiftLeft = isLeft;
        currentOp = operation;
        state = State.SHIFT_POSITION;
    }
    
    private void finishXOR() {
        String xorResult = tape.readSection(xorStartPos, bitLength);
        
        if (phaseStep == 1) {
            // Terminó x XOR (x << a), preparar para x >> b
            executionLog.add("x XOR (x << a) = " + xorResult);
            
            // El resultado está en xorStartPos (segunda posición)
            // Ahora necesitamos copiar con marcadores para x >> b
            copySourcePos = xorStartPos;
            copyTargetPos = tape.findNextBlank(xorStartPos + bitLength) + 1;
            counter = 0;
            phaseStep = 2;
            state = State.PRE_SHIFT_COPY_MARK;
            
        } else if (phaseStep == 2) {
            // Terminó x XOR (x >> b), preparar para x << c
            executionLog.add("x XOR (x >> b) = " + xorResult);
            
            copySourcePos = xorStartPos;
            copyTargetPos = tape.findNextBlank(xorStartPos + bitLength) + 1;
            counter = 0;
            phaseStep = 3;
            state = State.PRE_SHIFT_COPY_MARK;
            
        } else if (phaseStep == 3) {
            // Terminó x XOR (x << c) - RESULTADO FINAL
            executionLog.add("x XOR (x << c) = " + xorResult + " (RESULTADO FINAL)");
            
            readPos1 = xorStartPos;
            counter = 0;
            currentIteration++;
            state = State.COMPARE_INIT;
        }
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
