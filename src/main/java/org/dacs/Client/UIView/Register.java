package org.dacs.Client.UIView;

import org.dacs.Common.BankService;
import javax.swing.*;
import java.awt.*;
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
        // Khởi tạo panel1
        panel1 = new JPanel();
        panel1.setLayout(new GridLayout(4, 2)); // Sử dụng GridLayout cho bố cục các thành phần

        // Thêm các thành phần GUI vào panel1
        panel1.add(new JLabel("Email:"));
        getEmail = new JTextField();
        panel1.add(getEmail);

        panel1.add(new JLabel("Username:"));
        getUserName = new JTextField();
        panel1.add(getUserName);

        panel1.add(new JLabel("Password:"));
        getPass = new JPasswordField();
        panel1.add(getPass);

        register = new JButton("Register");
        panel1.add(register);

        // Thêm panel1 vào JFrame
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
//                            BankClientView bankClientView = new BankClientView(username, server1, server2);
//                            bankClientView.setVisible(true);
//                            server1.login(username, password);
                            server1.sendOTP(username,email, "OTP Verification", "Your OTP is: ");
                            OTPVerification otpVerification = new OTPVerification(username, password, email, server1, server2);
                            otpVerification.setVisible(true);
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