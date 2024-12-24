package org.dacs.Client.UIView;

import org.dacs.Common.BankService;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.Naming;
import java.rmi.RemoteException;

public class Register extends JFrame {
    private JPanel panel1;
    private JTextField getEmail;
    private JTextField getUserName;
    private JTextField getPass;
    private JButton register ;
    private BankService server1;
    private BankService server2;


    public void initComponents() {
        setTitle("Register");
        setSize(300, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        add(panel1);
    }
    // Constructor where button listener is defined
    public Register() {

        try {
            server1 = (BankService) Naming.lookup("//localhost:2000/BankService");
            server2 = (BankService) Naming.lookup("//localhost:2004/BankService");
        } catch (Exception e) {
            e.printStackTrace();
        }
        initComponents(); // Initialize components

        register.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Get text from the text fields
                String email = getEmail.getText();
                String username = getUserName.getText();
                String password = getPass.getText();

                // Validate input fields
                if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(Register.this, "Please fill in all fields.", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    // Show success message if fields are filled
                    JOptionPane.showMessageDialog(Register.this, "Registration Success!");
                    try {
                        if (server1.register(username, password, email)) {
                            JOptionPane.showMessageDialog(Register.this, "Registration to Server 1: Success");
                            BankClientView bankClientView = new BankClientView(username, server1, server2);
                            bankClientView.setVisible(true);
                            server1.login(username, password);
                        } else {
                            JOptionPane.showMessageDialog(Register.this, "Registration to Server 1: Failed");
                        }
                    } catch (RemoteException ex) {
                        throw new RuntimeException(ex);
                    }
                    // Open the BankClientView and pass the necessary data


                    // Close the Register screen
                    dispose();
                }
            }
        });
    }
    // Main method to launch the Register frame
    public static void main(String[] args) {
        Register register = new Register();
        register.initComponents();
        register.setVisible(true);
    }
}