package org.dacs.Client.UIView;

import org.dacs.Common.BankService;

import javax.swing.*;
import java.awt.*;
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

        initComponents();
        addListeners();
    }

    private void initComponents() {
        // Panel setup
        otpPanel = new JPanel();
        otpPanel.setLayout(new GridLayout(3, 1, 10, 10));

        // Instruction label
        instructionLabel = new JLabel("Enter the OTP sent to your email:");
        instructionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        otpPanel.add(instructionLabel);

        // OTP input field
        otpField = new JTextField();
        otpPanel.add(otpField);

        // Verify button
        verifyButton = new JButton("Verify");
        otpPanel.add(verifyButton);

        // Add panel to the frame
        add(otpPanel);
    }

    private void addListeners() {
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
}
