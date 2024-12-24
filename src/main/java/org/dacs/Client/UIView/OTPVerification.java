package org.dacs.Client.UIView;

import org.dacs.Common.BankService;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;

public class OTPVerification extends JFrame {
    private JPanel otpPanel;
    private JTextField otpField;
    private JButton verifyButton;
    private JLabel instructionLabel;

    private String username;
    private String password;
    private String email;
    private BankService server1;
    private BankService server2;

    public OTPVerification(String username, String password, String email, BankService server1, BankService server2) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.server1 = server1;
        this.server2 = server2;

        setTitle("OTP Verification");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        add(otpPanel);

        verifyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String otp = otpField.getText();

                if (otp.isEmpty()) {
                    JOptionPane.showMessageDialog(OTPVerification.this, "Please enter the OTP.", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    try {
                        boolean isVerified = server1.verifyOTP(username, otp);

                        if (isVerified) {
                            JOptionPane.showMessageDialog(OTPVerification.this, "OTP Verified Successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                            BankClientView bankClientView = new BankClientView(username, server1, server2);
                            bankClientView.setVisible(true);
                            dispose();
                        } else {
                            JOptionPane.showMessageDialog(OTPVerification.this, "Invalid OTP. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (RemoteException ex) {
                        JOptionPane.showMessageDialog(OTPVerification.this, "An error occurred while verifying the OTP. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    }
                }
            }
        });
    }
//
//    public static void main(String[] args) {
//        // Example usage
//        try {
//            BankService server1 = (BankService) java.rmi.Naming.lookup("//localhost:2000/BankService");
//            BankService server2 = (BankService) java.rmi.Naming.lookup("//localhost:2004/BankService");
//
//            // For testing purposes, replace with actual user data
//            new OTPVerification("testUser", "password", "test@example.com", server1, server2).setVisible(true);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
