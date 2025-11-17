package com.unpsjb.machineturing.model;
/**
 * Estados de la máquina
 */
public enum State {
    INIT,
    COPY_READ, 
    COPY_WRITE,
    PRE_SHIFT_COPY_MARK, 
    PRE_SHIFT_COPY_FIND_TARGET,
    PRE_SHIFT_COPY_WRITE_BIT, 
    PRE_SHIFT_COPY_RETURN, 
    PRE_SHIFT_COPY_RESTORE,
    SHIFT_POSITION, 
    SHIFT_START, 
    SHIFT_READ, 
    SHIFT_WRITE, 
    SHIFT_FILL_ZERO,
    XOR_MARK, 
    XOR_FIND_BLANK, 
    XOR_FIND_COMPARE, 
    XOR_COMPARE_BIT, 
    XOR_RETURN,
    COMPARE_INIT, 
    COMPARE_CHECK,
    CYCLE_DETECTED, 
    HALT
}
