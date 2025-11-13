package com.unpsjb.machineturing.model;
/**
 * Estados de la máquina
 */
public enum State {
    INIT, 
        COPY_READ, COPY_WRITE, COPY_MOVE_READ, COPY_MOVE_WRITE,
        SHIFT_LEFT_A_DELETE, SHIFT_LEFT_A_ADD_ZERO,
        XOR_A_COMPARE,
        SHIFT_RIGHT_B_DELETE, SHIFT_RIGHT_B_ADD_ZERO,
        XOR_B_COMPARE,
        SHIFT_LEFT_C_DELETE, SHIFT_LEFT_C_ADD_ZERO,
        XOR_C_COMPARE,
        COMPARE_VALUES,
        NEW_ITERATION, CYCLE_DETECTED, HALT
}
