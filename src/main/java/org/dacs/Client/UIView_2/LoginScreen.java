package org.dacs.Client.UIView_2;

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
    private BankService server1;
    private BankService server2;

    public LoginScreen() {
        try {
            server1 = (BankService) Naming.lookup("//localhost:2004/BankService");
            server2 = (BankService) Naming.lookup("//localhost:1099/BankService");
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

        add(panel);

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());

                try {
                    if (server1.login(username, password)) {
                        JOptionPane.showMessageDialog(null, "Login to Server 1: Success");
                        new BankClientView(username, server1, server2).setVisible(true);
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(null, "Login to Server 1: Failed");
                    }
                } catch (Exception ex) {
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