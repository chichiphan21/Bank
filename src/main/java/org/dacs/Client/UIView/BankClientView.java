package org.dacs.Client.UIView;

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
    private JTextField recipientField; // Thêm trường người nhận
    private JButton withdrawButton;
    private JButton transferButton; // Nút chuyển tiền
    private JButton logoutButton;
    private BankService server1;
    private BankService server2;
    private String username;
    private JTextArea transactionArea;

    public BankClientView(String username, BankService server1, BankService server2) {
        this.username = username;
        this.server1 = server1;
        this.server2 = server2;

        setTitle("Bank Client");
        setSize(400, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Username Label
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel("Username:"), gbc);

        usernameLabel = new JLabel(username);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(usernameLabel, gbc);

        // Balance Label
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel("Balance:"), gbc);

        balanceLabel = new JLabel();
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(balanceLabel, gbc);

        // Amount Field
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel("Amount:"), gbc);

        amountField = new JTextField(10);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(amountField, gbc);

        // Description Field
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel("Description:"), gbc);

        descriptionField = new JTextField(10);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(descriptionField, gbc);

        // Recipient Field
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel("Recipient:"), gbc);

        recipientField = new JTextField(10);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(recipientField, gbc);

        // Withdraw Button
        withdrawButton = new JButton("Withdraw");
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridwidth = 2;
        panel.add(withdrawButton, gbc);

        // Transfer Button
        transferButton = new JButton("Transfer");
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridwidth = 2;
        panel.add(transferButton, gbc);

        // Transaction Area
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        transactionArea = new JTextArea();
        transactionArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(transactionArea);
        scrollPane.setPreferredSize(new Dimension(350, 150));
        panel.add(scrollPane, gbc);

        // Logout Button
        logoutButton = new JButton("Logout");
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        panel.add(logoutButton, gbc);

        add(panel);

        updateBalance();
        loadTransactionHistory();

        withdrawButton.addActionListener(e -> handleWithdraw());
        transferButton.addActionListener(e -> handleTransfer()); // Hành động chuyển tiền
        logoutButton.addActionListener(e -> handleLogout());
    }

    private void handleWithdraw() {
        String amountText = amountField.getText().trim();
        String description = descriptionField.getText().trim();

        if (amountText.isEmpty() || description.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Please enter both amount and description.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
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

    private void handleTransfer() {
        String amountText = amountField.getText().trim();
        String description = descriptionField.getText().trim();
        String recipient = recipientField.getText().trim();

        if (amountText.isEmpty() || description.isEmpty() || recipient.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Please fill in all fields.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            TransactionResult result = server1.transfer(username, recipient, amount, description);
            if (result.isSuccess()) {
                JOptionPane.showMessageDialog(null, "Transfer Successful\n"
                        + "Amount Transferred: " + result.getAmountChanged() + "\n"
                        + "New Balance: " + result.getNewBalance() + "\n"
                        + "Recipient: " + recipient + "\n"
                        + "Description: " + result.getDescription() + "\n"
                        + "Timestamp: " + result.getTimestamp());
                updateBalance();
                loadTransactionHistory();
            } else {
                JOptionPane.showMessageDialog(null, "Transfer Failed: " + result.getDescription());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred during transfer.");
        }
    }

    private void handleLogout() {
        try {
            server1.logout(username);
            JOptionPane.showMessageDialog(null, "Logged out successfully");
            new LoginScreen().setVisible(true);
            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred during logout.");
        }
    }

    private void updateBalance() {
        try {
            double balance = server1.getBalance(username);
            balanceLabel.setText(String.format("%.2f", balance));
        } catch (Exception e) {
            e.printStackTrace();
            balanceLabel.setText("Error");
        }
    }

    private void loadTransactionHistory() {
        try {
            List<String> transactions = server1.getTransactionHistory(username);
            transactionArea.setText("");
            for (String txn : transactions) {
                transactionArea.append(txn + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
            transactionArea.setText("Unable to load transactions.");
        }
    }
}
