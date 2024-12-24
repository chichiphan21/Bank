package org.dacs.Client.UIView;

import org.dacs.Common.BankService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.Naming;

public class LoginScreen extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private BankService server1;
    private BankService server2;

    public LoginScreen() {
        try {
            server1 = (BankService) Naming.lookup("//localhost:2000/BankService");
            server2 = (BankService) Naming.lookup("//localhost:2004/BankService");
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("Login");
        setSize(300, 150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 2));

        panel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        panel.add(usernameField);

        panel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        loginButton = new JButton("Login");
        panel.add(loginButton);

        registerButton = new JButton("Register");
        panel.add(registerButton);

        add(panel);

        // Login button listener
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());

                try {
                    if (server1.login(username, password)) {
                        JOptionPane.showMessageDialog(null, "Login to Server 1: Success");
                        new BankClientView(username, server1, server2).setVisible(true);
                        dispose(); // Close the login screen after successful login
                    } else {
                        JOptionPane.showMessageDialog(null, "Login failed. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        });

        // Register button listener
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // Open the Register screen
                    new Register().setVisible(true);
                    dispose(); // Close the login screen when the Register screen opens
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new LoginScreen().setVisible(true);
            }
        });
    }
}
