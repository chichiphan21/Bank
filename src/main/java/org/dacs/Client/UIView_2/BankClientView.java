package org.dacs.Client.UIView_2;

import org.dacs.Common.BankService;
import org.dacs.Common.TransactionResult;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class BankClientView extends JFrame {
    private JLabel usernameLabel;
    private JLabel balanceLabel;
    private JTextField amountField;
    private JTextField descriptionField;
    private JButton withdrawButton;
    private JButton logoutButton;
    private JTextArea transactionHistoryArea;
    private BankService server1;
    private BankService server2;
    private String username;

    public BankClientView(String username, BankService server1, BankService server2) {
        this.username = username;
        this.server1 = server1;
        this.server2 = server2;
        // Set up frame
        setTitle("Bank Client");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(7, 2, 5, 5));

        // Username and balance labels
        panel.add(new JLabel("Username:"));
        usernameLabel = new JLabel(username);
        panel.add(usernameLabel);

        panel.add(new JLabel("Balance:"));
        balanceLabel = new JLabel();
        panel.add(balanceLabel);

        // Amount and description fields
        panel.add(new JLabel("Amount:"));
        amountField = new JTextField();
        panel.add(amountField);

        panel.add(new JLabel("Description:"));
        descriptionField = new JTextField();
        panel.add(descriptionField);

        // Withdraw and logout buttons
        withdrawButton = new JButton("Withdraw");
        panel.add(withdrawButton);

        logoutButton = new JButton("Logout");
        panel.add(logoutButton);

        // Transaction history area
        transactionHistoryArea = new JTextArea();
        transactionHistoryArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(transactionHistoryArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Add components to frame
        add(panel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Load initial data
        updateBalance();
        loadTransactionHistory();

        withdrawButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String amountText = amountField.getText().trim();
                String description = descriptionField.getText().trim();

                if (amountText.isEmpty() || description.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Please enter both amount and description.");
                    return;
                }

                double amount;
                try {
                    amount = Double.parseDouble(amountText);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "Please enter a valid amount.");
                    return;
                }

                try {
                    TransactionResult result = server1.withdraw(username, amount, description);
                    if (result.isSuccess()) {
                        JOptionPane.showMessageDialog(null, "Transaction Successful\n"
                                + "Amount Changed: " + result.getAmountChanged() + "\n"
                                + "New Balance: " + result.getNewBalance() + "\n"
                                + "Description: " + result.getDescription() + "\n"
                                + "Timestamp: " + result.getTimestamp());
                        updateBalance();
                        loadTransactionHistory();
                    } else {
                        JOptionPane.showMessageDialog(null, "Transaction Failed: " + result.getDescription());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "An error occurred during withdrawal.");
                }
            }
        });



        // Logout Button Action Listener
        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    server1.logout(username);
                    JOptionPane.showMessageDialog(null, "Logged out successfully");
                    new LoginScreen().setVisible(true);
                    dispose();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "An error occurred during logout.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private void updateBalance() {
        try {
            double balance = server1.getBalance(username);
            balanceLabel.setText(String.valueOf(balance));
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to retrieve balance.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadTransactionHistory() {
        try {
            List<String> transactionHistory = server1.getTransactionHistory(username);
            transactionHistoryArea.setText(""); // Clear previous history
            for (String transaction : transactionHistory) {
                transactionHistoryArea.append(transaction + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to retrieve transaction history.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
