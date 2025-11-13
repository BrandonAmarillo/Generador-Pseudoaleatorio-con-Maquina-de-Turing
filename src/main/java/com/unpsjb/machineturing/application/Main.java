package com.unpsjb.machineturing.application;

import javax.swing.SwingUtilities;

import com.unpsjb.machineturing.ui.TuringGUI;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TuringGUI gui = new TuringGUI();
            gui.setVisible(true);
        });
    }
}

