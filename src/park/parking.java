package park;

import javax.swing.*;  
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;


abstract class Vehicle {
    protected String ownerName, model, plate, entryTime;
    protected int floor, slot;
    abstract String getType();
    public String getPlate() { return plate; }
    public String getEntryTime() { return entryTime; }
    public int getFloor() { return floor; }
    public int getSlot() { return slot; }
}

class Car extends Vehicle {
    Car(String o, String m, String p, String et, int f, int s) {
        this.ownerName = o; this.model = m; this.plate = p;
        this.entryTime = et; this.floor = f; this.slot = s;
    }
    String getType() { return "Car"; }
}

class Motorcycle extends Vehicle {
    Motorcycle(String o, String m, String p, String et, int f, int s) {
        this.ownerName = o; this.model = m; this.plate = p;
        this.entryTime = et; this.floor = f; this.slot = s;
    }
    String getType() { return "Motorcycle"; }
}

class Commercial extends Vehicle {
    Commercial(String o, String m, String p, String et, int f, int s) {
        this.ownerName = o; this.model = m; this.plate = p;
        this.entryTime = et; this.floor = f; this.slot = s;
    }
    String getType() { return "Commercial"; }
}


public class parking extends JFrame {
    private ParkingSystem logic;
    private DefaultTableModel tableModel;
    private JTable parkingTable;

    public parking() {
        try {
            int floors = Integer.parseInt(JOptionPane.showInputDialog("Enter Floor Number:"));
            int slots = Integer.parseInt(JOptionPane.showInputDialog("Enter Slot Number per Floor:"));
            logic = new ParkingSystem(floors, slots);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Invalid input! Defaulting to 2x2.");
            logic = new ParkingSystem(2, 2);
        }

        setTitle("Automated Parking System");
        setSize(800, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel btnPanel = new JPanel();
        JButton btnPark = new JButton("Park Vehicle");
        JButton btnExit = new JButton("Exit & Calculate Price");
        btnPanel.add(btnPark);
        btnPanel.add(btnExit);
        add(btnPanel, BorderLayout.NORTH);

        String[] columns = {"Floor", "Slot", "Status / Plate"};
        tableModel = new DefaultTableModel(columns, 0);
        parkingTable = new JTable(tableModel);
        updateTable();
        add(new JScrollPane(parkingTable), BorderLayout.CENTER);

        btnPark.addActionListener(e -> showParkDialog());
        btnExit.addActionListener(e -> showExitDialog());
        
        setLocationRelativeTo(null);
    }

    private void updateTable() {
        tableModel.setRowCount(0);
        for (int i = 0; i < logic.getParkSlots().size(); i++) {
            for (int j = 0; j < logic.getParkSlots().get(i).size(); j++) {
                String plate = logic.getParkSlots().get(i).get(j);
                tableModel.addRow(new Object[]{i, j, (plate == null ? "Available" : plate)});
            }
        }
    }

    private void showParkDialog() {
        JTextField plateF = new JTextField();
        JTextField ownerF = new JTextField();
        JTextField timeF = new JTextField("12:00");
        String[] types = {"Car", "Motorcycle", "Commercial"};  
        JComboBox<String> typeBox = new JComboBox<>(types);

        Object[] message = {
            "Plate:", plateF,
            "Owner:", ownerF,
            "Type:", typeBox,
            "Entry Time (HH:MM):", timeF
        };

        int option = JOptionPane.showConfirmDialog(null, message, "Park a Vehicle", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                int f = Integer.parseInt(JOptionPane.showInputDialog("Floor:"));
                int s = Integer.parseInt(JOptionPane.showInputDialog("Slot:"));
                String selectedType = (String) typeBox.getSelectedItem();
                
                Vehicle v;
                if(selectedType.equals("Car")) v = new Car(ownerF.getText(), "Model", plateF.getText(), timeF.getText(), f, s);
                else if(selectedType.equals("Motorcycle")) v = new Motorcycle(ownerF.getText(), "Model", plateF.getText(), timeF.getText(), f, s);
                else v = new Commercial(ownerF.getText(), "Model", plateF.getText(), timeF.getText(), f, s);

                if (logic.addVehicle(v)) {
                    JOptionPane.showMessageDialog(this, "Successfully Parked!");
                    updateTable();
                } else {
                    JOptionPane.showMessageDialog(this, "Slot Occupied or Out of Bounds!");
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error: Invalid Input.");
            }
        }
    }

    private void showExitDialog() {
        String plate = JOptionPane.showInputDialog("Enter Plate to Remove:");
        if (plate == null || plate.isEmpty()) return;

        String exitT = JOptionPane.showInputDialog("Enter Exit Time (HH:MM):");
        if (exitT == null || exitT.isEmpty()) return;
        
        int price = logic.removeVehicle(plate, exitT);
        if (price >= 0) {
            
            JOptionPane.showMessageDialog(this, 
                "Exiting is completed successfully.\n" +
                "Vehicle Plate: " + plate + "\n" +
                "Total Price: " + price + " TL", 
                "Exit Success", JOptionPane.INFORMATION_MESSAGE);
            updateTable();
        } else {
            JOptionPane.showMessageDialog(this, "The vehicle wasn't found!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new parking().setVisible(true));
    }
}


class ParkingSystem {
    private List<List<String>> parkSlots = new ArrayList<>(); 
    private Map<String, Vehicle> parkedVehicles = new HashMap<>(); 

    public ParkingSystem(int f, int s) {
        for (int i = 0; i < f; i++) {
            parkSlots.add(new ArrayList<>(Collections.nCopies(s, null)));
        }
    }

    public boolean addVehicle(Vehicle v) {
        if (v.getFloor() < parkSlots.size() && v.getSlot() < parkSlots.get(v.getFloor()).size()) {
            if (parkSlots.get(v.getFloor()).get(v.getSlot()) == null) {
                parkSlots.get(v.getFloor()).set(v.getSlot(), v.getPlate());
                parkedVehicles.put(v.getPlate(), v);
                return true;
            }
        }
        return false;
    }

    public int removeVehicle(String plate, String exitT) {
        Vehicle v = parkedVehicles.remove(plate);
        if (v != null) {
            parkSlots.get(v.getFloor()).set(v.getSlot(), null);
            return calculatePrice(v.getEntryTime(), exitT, v.getType());
        }
        return -1;
    }

    private int calculatePrice(String entry, String exit, String type) {
        try {
           
            String[] entryParts = entry.split(":");
            String[] exitParts = exit.split(":");
            int entryH = Integer.parseInt(entryParts[0]);
            int entryM = Integer.parseInt(entryParts[1]);
            int exitH = Integer.parseInt(exitParts[0]);
            int exitM = Integer.parseInt(exitParts[1]);

            
            int totalMinutes = (exitH * 60 + exitM) - (entryH * 60 + entryM);
            int hours = (totalMinutes + 59) / 60; 
            if (hours <= 0) hours = 1;

            
            int rate;
            switch (type) {
                case "Motorcycle": rate = 40; break;
                case "Car": rate = 60; break;
                case "Commercial": rate = 80; break;
                default: rate = 0;
            }
            return hours * rate; 
        } catch (Exception e) { 
            return 60; 
        }
    }

    public List<List<String>> getParkSlots() { return parkSlots; }
}