package com.unpsjb.machineturing.ui;

import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;

import com.unpsjb.machineturing.model.TapeCell;
import com.unpsjb.machineturing.model.TuringTape;
import com.unpsjb.machineturing.service.MachineTuring;

// Panel personalizado para dibujar la cinta
class TapePanel extends JPanel {
    private TuringTape tape;
    private static final int CELL_SIZE = 40;
    private static final int CELL_SPACING = 2;
    
    public TapePanel() {
        setPreferredSize(new Dimension(800, 100));
        setBackground(Color.LIGHT_GRAY);
    }
    
    public void setTape(TuringTape tape) {
        this.tape = tape;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (tape == null) return;
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        List<TapeCell> cells = tape.getCells();
        int startX = 10;
        int y = 30;
        
        // Mostrar solo las primeras 60 celdas
        int maxCells = Math.min(60, cells.size());
        
        for (int i = 0; i < maxCells; i++) {
            TapeCell cell = cells.get(i);
            int x = startX + i * (CELL_SIZE + CELL_SPACING);
            
            // Dibujar celda
            g2d.setColor(cell.getColor());
            g2d.fillRect(x, y, CELL_SIZE, CELL_SIZE);
            
            // Borde
            g2d.setColor(cell.isHead() ? Color.RED : Color.BLACK);
            g2d.setStroke(new BasicStroke(cell.isHead() ? 3 : 1));
            g2d.drawRect(x, y, CELL_SIZE, CELL_SIZE);
            
            // Símbolo
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Monospaced", Font.BOLD, 20));
            FontMetrics fm = g2d.getFontMetrics();
            String symbol = String.valueOf(cell.getSymbol());
            int textX = x + (CELL_SIZE - fm.stringWidth(symbol)) / 2;
            int textY = y + ((CELL_SIZE - fm.getHeight()) / 2) + fm.getAscent();
            g2d.drawString(symbol, textX, textY);
        }
    }
}


public class TuringGUI extends JFrame{
   private MachineTuring machine;
   private TapePanel tapePanel;
   private JTextArea logDisplay;
   private JLabel stateLabel;
   private JButton stepButton;
   private JButton runButton;
   private JButton resetButton;
   private JTextField seedField;
   private JTextField aField, bField, cField;
   private Timer autoTimer;
    
    public TuringGUI() {
        setTitle("Máquina de Turing - Generador XOR-Shift");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        
        // Panel de configuración
        JPanel configPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        configPanel.setBorder(BorderFactory.createTitledBorder("Configuración"));
        
        configPanel.add(new JLabel("Semilla binaria (6 bits):"));
        seedField = new JTextField("110010");
        configPanel.add(seedField);
        
        configPanel.add(new JLabel("Parámetro a (shift left):"));
        aField = new JTextField("1");
        configPanel.add(aField);
        
        configPanel.add(new JLabel("Parámetro b (shift right):"));
        bField = new JTextField("3");
        configPanel.add(bField);
        
        configPanel.add(new JLabel("Parámetro c (shift left):"));
        cField = new JTextField("2");
        configPanel.add(cField);
        
        JButton initButton = new JButton("Inicializar Máquina");
        initButton.addActionListener(e -> initializeMachine());
        configPanel.add(initButton);
        
        add(configPanel, BorderLayout.NORTH);
        
        // Panel central para la cinta
        JPanel centerPanel = new JPanel(new BorderLayout());
        
        stateLabel = new JLabel("Estado: No inicializado", SwingConstants.CENTER);
        stateLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        stateLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        centerPanel.add(stateLabel, BorderLayout.NORTH);
        
        tapePanel = new TapePanel();
        JScrollPane tapeScroll = new JScrollPane(tapePanel);
        tapeScroll.setBorder(BorderFactory.createTitledBorder("Cinta de Turing"));
        centerPanel.add(tapeScroll, BorderLayout.CENTER);
        
        add(centerPanel, BorderLayout.CENTER);
        
        // Panel de log
        logDisplay = new JTextArea(8, 60);
        logDisplay.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logDisplay.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logDisplay);
        logScroll.setBorder(BorderFactory.createTitledBorder("Log de Ejecución"));
        add(logScroll, BorderLayout.SOUTH);
        
        // Panel de controles
        JPanel controlPanel = new JPanel(new FlowLayout());
        
        stepButton = new JButton("Paso (Step)");
        stepButton.setEnabled(false);
        stepButton.addActionListener(e -> executeStep());
        controlPanel.add(stepButton);
        
        runButton = new JButton("Ejecutar Automático");
        runButton.setEnabled(false);
        runButton.addActionListener(e -> runAutomatic());
        controlPanel.add(runButton);
        
        resetButton = new JButton("Reiniciar");
        resetButton.setEnabled(false);
        resetButton.addActionListener(e -> reset());
        controlPanel.add(resetButton);
        
        add(controlPanel, BorderLayout.EAST);
        
        // Timer para ejecución automática
        autoTimer = new Timer(300, e -> {
            if (!executeStep()) {
                autoTimer.stop();
                runButton.setText("Ejecutar Automático");
                runButton.setEnabled(false);
            }
        });
        
        setLocationRelativeTo(null);
    }
    
    private void initializeMachine() {
        try {
            String seed = seedField.getText().trim();
            int a = Integer.parseInt(aField.getText().trim());
            int b = Integer.parseInt(bField.getText().trim());
            int c = Integer.parseInt(cField.getText().trim());
            
            if (!seed.matches("[01]{6}")) {
                JOptionPane.showMessageDialog(this, "La semilla debe ser de 6 bits (0s y 1s)");
                return;
            }
            
            machine = new MachineTuring(seed, a, b, c);
            stepButton.setEnabled(true);
            runButton.setEnabled(true);
            resetButton.setEnabled(true);
            
            updateDisplay();
            JOptionPane.showMessageDialog(this, "Máquina inicializada correctamente");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Error en los parámetros numéricos");
        }
    }
    
    private boolean executeStep() {
        if (machine == null) return false;
        
        boolean continued = machine.step();
        updateDisplay();
        
        if (machine.isCycleDetected()) {
            JOptionPane.showMessageDialog(this, 
                "¡Ciclo detectado!\nPeríodo: " + machine.getCycleLength() + " valores",
                "Ciclo Completado", 
                JOptionPane.INFORMATION_MESSAGE);
            stepButton.setEnabled(false);
            runButton.setEnabled(false);
        }
        
        return continued;
    }
    
    private void runAutomatic() {
        if (autoTimer.isRunning()) {
            autoTimer.stop();
            runButton.setText("Ejecutar Automático");
        } else {
            autoTimer.start();
            runButton.setText("Pausar");
        }
    }
    
    private void reset() {
        if (autoTimer.isRunning()) {
            autoTimer.stop();
        }
        machine = null;
        tapePanel.setTape(null);
        logDisplay.setText("");
        stateLabel.setText("Estado: No inicializado");
        stepButton.setEnabled(false);
        runButton.setEnabled(false);
        runButton.setText("Ejecutar Automático");
    }
    
    private void updateDisplay() {
        if (machine == null) return;
        
        // Actualizar cinta visual
        tapePanel.setTape(machine.getTape());
        
        // Actualizar estado
        stateLabel.setText("Estado: " + machine.getCurrentState());
        
        // Actualizar log
        StringBuilder logStr = new StringBuilder();
        List<String> log = machine.getExecutionLog();
        for (String line : log) {
            logStr.append(line).append("\n");
        }
        logDisplay.setText(logStr.toString());
        logDisplay.setCaretPosition(logDisplay.getDocument().getLength());
    }
}
