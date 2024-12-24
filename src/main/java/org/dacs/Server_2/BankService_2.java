package org.dacs.Server_2;

import org.dacs.Common.*;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BankService_2 extends UnicastRemoteObject implements BankService {

    private static final Logger logger = LoggerFactory.getLogger(BankService_2.class);
    private Connection connection;

    public BankService_2() throws RemoteException {
        super();
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection("jdbc:postgresql://localhost:5433/BankServer_2", "postgres", "chi2107");
            logger.info("Connected to BankServer_2 database.");
        } catch (Exception e) {
            logger.error("Failed to connect to the database.", e);
            throw new RemoteException("Failed to connect to the database.", e);
        }
    }

    @Override
    public boolean sendOTP(String to, String subject, String text) throws RemoteException {
        try {
//        Step 1: Create OTP and time created otp and store in database
            String otp = Otp.generateOTP();
            String query = "UPDATE users SET otp = ?, otp_created_at = NOW() WHERE email = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, otp);
                stmt.setString(2, to);
                stmt.executeUpdate();
            } catch (SQLException e) {
                logger.error("Error storing OTP for email: {}", to, e);
                throw new RemoteException("Error storing OTP for email: " + to, e);
            }
//        Step 2: Send OTP to email
            EmailSender.sendEmail(to, subject, text + otp);
            logger.info("OTP sent to email: {}", to);
            syncOtpWithOtherServers(to, otp);
//        Step 3 : Return true if send email success
            return true;
        } catch (Exception e) {
            logger.error("Error sending OTP to email: {}", to, e);
            return false;
        }
    }


    @Override
    public boolean verifyOTP(String username, String otp) throws RemoteException {
        try {
//        Step 1: Check OTP in database
            String otpCreatedAt = "";
            String query = "SELECT otp, otp_created_at FROM users WHERE username = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        otpCreatedAt = rs.getString("otp_created_at");
                        String storedOtp = rs.getString("otp");
                        if (!otp.equals(storedOtp)) {
                            logger.warn("Invalid OTP: {}", otp);
                            return false;
                        }
                    } else {
                        logger.warn("User not found: {}", username);
                        return false;
                    }
                }
            } catch (SQLException e) {
                logger.error("Error verifying OTP for user: {}", username, e);
                throw new RemoteException("Error verifying OTP for user: " + username, e);
            }
            //        Step 2: Check time created otp < 5 minutes
            LocalDateTime otpCreatedTime = LocalDateTime.parse(otpCreatedAt);
            LocalDateTime currentTime = LocalDateTime.now();
            Duration duration = Duration.between(otpCreatedTime, currentTime);
            if (duration.toMinutes() > 5) {
                logger.warn("OTP expired: {}", otp);
                return false;
            }
            //        Step 3: Return true if all true
            return true;
        } catch (Exception e) {
            logger.error("Error verifying OTP: {}", otp, e);
            return false;
        }
    }


    // User Management Methods
    @Override
    public boolean register(String username, String password, String email) throws RemoteException {
        String checkQuery = "SELECT * FROM users WHERE username = ?";
        String insertQuery = "INSERT INTO users (username, password, balance, is_online, email) VALUES (?, ?, ?, ?, ?) RETURNING id";

        try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
            checkStmt.setString(1, username);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    logger.warn("Registration failed: User {} already exists.", username);
                    return false;
                }
            }

            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, hashedPassword);
                insertStmt.setDouble(3, 0.0); // Initial balance
                insertStmt.setBoolean(4, false); // Offline by default
                insertStmt.setString(5, email);
                try (ResultSet rs = insertStmt.executeQuery()) {
                    if (rs.next()) {
                        int newUserId = rs.getInt("id");
                        logger.info("New user created with ID: {}", newUserId);
                    }
                }
            }

            // Sync registration with other servers
            syncRegisterWithOtherServers(username, hashedPassword, email);
            logger.info("User {} registered successfully.", username);
            return true;

        } catch (SQLException e) {
            logger.error("Registration failed for user: {}", username, e);
            throw new RemoteException("Registration failed for user: " + username, e);
        }
    }

    @Override
    public boolean login(String username, String password) throws RemoteException {
        String query = "SELECT password FROM users WHERE username = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHashedPassword = rs.getString("password");
                    if (BCrypt.checkpw(password, storedHashedPassword)) {
                        if (isUserOnline(username)) {
                            logger.warn("Login failed: User {} is already online.", username);
                            return false; // User already online
                        }
                        String updateQuery = "UPDATE users SET is_online = TRUE WHERE username = ?";
                        try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                            updateStmt.setString(1, username);
                            updateStmt.executeUpdate();
                        }
                        syncLoginStatusWithOtherServers(username, true);
                        logger.info("User {} logged in successfully.", username);
                        return true;
                    } else {
                        logger.warn("Login failed: Incorrect password for user {}.", username);
                    }
                } else {
                    logger.warn("Login failed: User {} not found.", username);
                }
            }
        } catch (SQLException e) {
            logger.error("Login failed for user: {}", username, e);
            throw new RemoteException("Login failed for user: " + username, e);
        }
        return false;
    }

    @Override
    public void logout(String username) throws RemoteException {
        String query = "UPDATE users SET is_online = FALSE WHERE username = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                syncLoginStatusWithOtherServers(username, false);
                logger.info("User {} logged out successfully.", username);
            } else {
                logger.warn("Logout failed: User {} not found.", username);
            }
        } catch (SQLException e) {
            logger.error("Logout failed for user: {}", username, e);
            throw new RemoteException("Logout failed for user: " + username, e);
        }
    }

    @Override
    public boolean isUserOnline(String username) throws RemoteException {
        String query = "SELECT is_online FROM users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    boolean isOnline = rs.getBoolean("is_online");
                    logger.info("isUserOnline: User {} is {}", username, isOnline ? "online" : "offline");
                    return isOnline;
                } else {
                    logger.warn("isUserOnline: User {} not found.", username);
                }
            }
        } catch (SQLException e) {
            logger.error("Error checking online status for user: {}", username, e);
            throw new RemoteException("Error checking online status for user: " + username, e);
        }
        return false;
    }

    @Override
    public TransactionResult withdraw(String username, double amount, String description) throws RemoteException {
        String query = "SELECT id, balance FROM users WHERE username = ?";
        String updateBalanceQuery = "UPDATE users SET balance = ? WHERE id = ?";
        String insertTransactionQuery = "INSERT INTO transactions (user_id, amount, description, timestamp) VALUES (?, ?, ?, NOW())";

        try {
            connection.setAutoCommit(false); // Start transaction

            int userId = -1; // Initialize to an invalid value
            double currentBalance = 0.0; // Initialize to a default value

            // Retrieve user information
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        userId = rs.getInt("id");
                        currentBalance = rs.getDouble("balance");
                    } else {
                        connection.rollback(); // Rollback transaction
                        return new TransactionResult(false, currentBalance, amount, "User not found", null);
                    }
                }
            }

            double newBalance = currentBalance + amount;

            // Update balance
            try (PreparedStatement updateStmt = connection.prepareStatement(updateBalanceQuery)) {
                updateStmt.setDouble(1, newBalance);
                updateStmt.setInt(2, userId);
                updateStmt.executeUpdate();
            }

            // Insert transaction record
            try (PreparedStatement insertTxnStmt = connection.prepareStatement(insertTransactionQuery)) {
                insertTxnStmt.setInt(1, userId);
                insertTxnStmt.setDouble(2, amount);
                insertTxnStmt.setString(3, description);
                insertTxnStmt.executeUpdate();
            }

            connection.commit(); // Commit transaction

            // Return successful transaction result
            return new TransactionResult(true, newBalance, amount, description, java.time.LocalDateTime.now().toString());

        } catch (SQLException e) {
            try {
                connection.rollback(); // Rollback on error
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            return new TransactionResult(false, 0.0, amount, "Transaction failed: " + e.getMessage(), null);
        } finally {
            try {
                connection.setAutoCommit(true); // Restore default
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    public TransactionResult transfer(String sender, String receiver, double amount, String description) throws RemoteException {
        String getSenderQuery = "SELECT id, balance FROM users WHERE username = ?";
        String getReceiverQuery = "SELECT id FROM users WHERE username = ?";
        String updateSenderBalanceQuery = "UPDATE users SET balance = ? WHERE id = ?";
        String updateReceiverBalanceQuery = "UPDATE users SET balance = ? WHERE id = ?";
        String insertTransactionQuery = "INSERT INTO transactions (user_id, amount, description, timestamp) VALUES (?, ?, ?, NOW())";

        try {
            connection.setAutoCommit(false); // Bắt đầu transaction

            int senderId;
            double senderBalance;
            int receiverId;

            // Lấy thông tin người gửi
            try (PreparedStatement senderStmt = connection.prepareStatement(getSenderQuery)) {
                senderStmt.setString(1, sender);
                try (ResultSet rs = senderStmt.executeQuery()) {
                    if (rs.next()) {
                        senderId = rs.getInt("id");
                        senderBalance = rs.getDouble("balance");
                    } else {
                        throw new RemoteException("Sender not found: " + sender);
                    }
                }
            }

            // Kiểm tra số dư
            if (senderBalance < amount) {
                throw new RemoteException("Insufficient funds for sender: " + sender);
            }

            // Lấy thông tin người nhận
            try (PreparedStatement receiverStmt = connection.prepareStatement(getReceiverQuery)) {
                receiverStmt.setString(1, receiver);
                try (ResultSet rs = receiverStmt.executeQuery()) {
                    if (rs.next()) {
                        receiverId = rs.getInt("id");
                    } else {
                        throw new RemoteException("Receiver not found: " + receiver);
                    }
                }
            }

            // Cập nhật số dư người gửi
            double newSenderBalance = senderBalance - amount;
            try (PreparedStatement updateSenderStmt = connection.prepareStatement(updateSenderBalanceQuery)) {
                updateSenderStmt.setDouble(1, newSenderBalance);
                updateSenderStmt.setInt(2, senderId);
                updateSenderStmt.executeUpdate();
            }

            // Cập nhật số dư người nhận
            try (PreparedStatement updateReceiverStmt = connection.prepareStatement(updateReceiverBalanceQuery)) {
                updateReceiverStmt.setDouble(1, amount); // Thêm tiền cho người nhận
                updateReceiverStmt.setInt(2, receiverId);
                updateReceiverStmt.executeUpdate();
            }

            // Ghi nhận giao dịch của người gửi
            try (PreparedStatement insertTransactionStmt = connection.prepareStatement(insertTransactionQuery)) {
                insertTransactionStmt.setInt(1, senderId);
                insertTransactionStmt.setDouble(2, -amount); // Âm vì là gửi tiền
                insertTransactionStmt.setString(3, description);
                insertTransactionStmt.executeUpdate();
            }

            // Ghi nhận giao dịch của người nhận
            try (PreparedStatement insertTransactionStmt = connection.prepareStatement(insertTransactionQuery)) {
                insertTransactionStmt.setInt(1, receiverId);
                insertTransactionStmt.setDouble(2, amount); // Dương vì là nhận tiền
                insertTransactionStmt.setString(3, "Received from " + sender + ": " + description);
                insertTransactionStmt.executeUpdate();
            }

            connection.commit(); // Commit transaction

            // Trả về kết quả giao dịch
            return new TransactionResult(
                    true,
                    newSenderBalance,
                    -amount,
                    description,
                    new java.util.Date().toString()
            );
        } catch (Exception e) {
            try {
                connection.rollback(); // Rollback nếu xảy ra lỗi
            } catch (SQLException rollbackEx) {
                throw new RemoteException("Transaction failed and rollback failed", rollbackEx);
            }
            throw new RemoteException("Transfer failed: " + e.getMessage(), e);
        } finally {
            try {
                connection.setAutoCommit(true); // Khôi phục trạng thái auto-commit
            } catch (SQLException e) {
                throw new RemoteException("Failed to reset auto-commit", e);
            }
        }
    }

    @Override
    public double getBalance(String username) throws RemoteException {
        String query = "SELECT balance FROM users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double balance = rs.getDouble("balance");
                    logger.info("getBalance: User {} has balance {}", username, balance);
                    return balance;
                } else {
                    logger.warn("getBalance: User {} not found.", username);
                    throw new RemoteException("User not found: " + username);
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving balance for user: {}", username, e);
            throw new RemoteException("Error retrieving balance for user: " + username, e);
        }
    }

    @Override
    public void setBalance(String username, double newBalance) throws RemoteException {
        String query = "UPDATE users SET balance = ? WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setDouble(1, newBalance);
            stmt.setString(2, username);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                syncBalanceWithOtherServers(username, newBalance);
                logger.info("setBalance: User {} balance set to {}", username, newBalance);
            } else {
                logger.warn("setBalance: User {} not found.", username);
                throw new RemoteException("User not found: " + username);
            }
        } catch (SQLException e) {
            logger.error("Error setting balance for user: {}", username, e);
            throw new RemoteException("Error setting balance for user: " + username, e);
        }
    }

    // Transaction History
    @Override
    public List<String> getTransactionHistory(String username) throws RemoteException {
        List<String> transactions = new ArrayList<>();
        String userQuery = "SELECT id FROM users WHERE username = ?";
        String txnQuery = "SELECT amount, description, timestamp FROM transactions WHERE user_id = ? ORDER BY timestamp DESC";

        try (PreparedStatement userStmt = connection.prepareStatement(userQuery)) {
            userStmt.setString(1, username);
            try (ResultSet userRs = userStmt.executeQuery()) {
                if (userRs.next()) {
                    int userId = userRs.getInt("id");

                    try (PreparedStatement txnStmt = connection.prepareStatement(txnQuery)) {
                        txnStmt.setInt(1, userId);
                        try (ResultSet txnRs = txnStmt.executeQuery()) {
                            while (txnRs.next()) {
                                double amount = txnRs.getDouble("amount");
                                String description = txnRs.getString("description");
                                Timestamp timestamp = txnRs.getTimestamp("timestamp");
                                String txnRecord = String.format("%s: %s %.2f", timestamp.toString(), description, amount);
                                transactions.add(txnRecord);
                            }
                        }
                    }
                    logger.info("getTransactionHistory: Retrieved {} transactions for user {}.", transactions.size(), username);
                } else {
                    logger.warn("getTransactionHistory: User {} not found.", username);
                    throw new RemoteException("User not found: " + username);
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving transaction history for user: {}", username, e);
            throw new RemoteException("Error retrieving transaction history for user: " + username, e);
        }
        return transactions;
    }

    // Synchronization Methods with Flags
    @Override
    public boolean syncRegister(String username, String hashedPassword, String email) throws RemoteException {
        String checkQuery = "SELECT * FROM users WHERE username = ?";
        String insertQuery = "INSERT INTO users (username, password, balance, is_online, email) VALUES (?, ?, ?, ?, ?) RETURNING id";

        try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
            checkStmt.setString(1, username);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    logger.warn("Sync Registration failed: User {} already exists.", username);
                    return false;
                }
            }

            try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, hashedPassword);
                insertStmt.setDouble(3, 0.0); // Initial balance
                insertStmt.setBoolean(4, false); // Offline by default
                insertStmt.setString(5, email);
                try (ResultSet rs = insertStmt.executeQuery()) {
                    if (rs.next()) {
                        int newUserId = rs.getInt("id");
                        logger.info("Sync: New user created with ID: {}", newUserId);
                    }
                }
            }

            logger.info("Sync: User {} registered successfully.", username);
            return true;

        } catch (SQLException e) {
            logger.error("Sync Registration failed for user: {}", username, e);
            throw new RemoteException("Sync Registration failed for user: " + username, e);
        }
    }

    @Override
    public void syncBalance(String username, double newBalance, boolean isSyncCall) throws RemoteException {
        if (isSyncCall) {
            updateBalance(username, newBalance);
            return;
        }
        updateBalance(username, newBalance);
        syncBalanceWithOtherServers(username, newBalance);
    }

    @Override
    public void syncLoginStatus(String username, boolean status, boolean isSyncCall) throws RemoteException {
        if (isSyncCall) {
            updateLoginStatus(username, status);
            return;
        }
        updateLoginStatus(username, status);
        syncLoginStatusWithOtherServers(username, status);
    }

    @Override
    public void syncTransaction(int userId, double amount, String description, boolean isSyncCall) throws RemoteException {
        if (isSyncCall) {
            logTransaction(userId, amount, description);
            return;
        }
        logTransaction(userId, amount, description);
        // Optionally sync with other servers if needed
    }


    @Override
    public void syncOTP(String username, String otp) throws RemoteException {
        String query = "UPDATE users SET otp = ? WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, otp);
            stmt.setString(2, username);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                logger.info("OTP synced for user: {}", username);
            } else {
                logger.warn("OTP sync failed: User {} not found.", username);
            }
        } catch (SQLException e) {
            logger.error("Error syncing OTP for user: {}", username, e);
        }
    }

    // Helper Methods
    private void updateBalance(String username, double newBalance) {
        String query = "UPDATE users SET balance = ? WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setDouble(1, newBalance);
            stmt.setString(2, username);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                logger.info("Balance updated for user: {} to {}", username, newBalance);
            } else {
                logger.warn("Balance update failed: User {} not found.", username);
            }
        } catch (SQLException e) {
            logger.error("Error updating balance for user: {}", username, e);
        }
    }

    private void updateLoginStatus(String username, boolean status) {
        String query = "UPDATE users SET is_online = ? WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setBoolean(1, status);
            stmt.setString(2, username);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                logger.info("Login status updated for user: {} to {}", username, status);
            } else {
                logger.warn("Login status update failed: User {} not found.", username);
            }
        } catch (SQLException e) {
            logger.error("Error updating login status for user: {}", username, e);
        }
    }

    private void logTransaction(int userId, double amount, String description) {
        String txnQuery = "INSERT INTO transactions (user_id, amount, description, timestamp) VALUES (?, ?, ?, NOW())";
        try (PreparedStatement txnStmt = connection.prepareStatement(txnQuery)) {
            txnStmt.setInt(1, userId);
            txnStmt.setDouble(2, amount);
            txnStmt.setString(3, description);
            txnStmt.executeUpdate();
            logger.info("Transaction logged for user ID: {} | Amount: {} | Description: {}", userId, amount, description);
        } catch (SQLException e) {
            logger.error("Error logging transaction for user ID: {}", userId, e);
        }
    }

    // Synchronization Helpers
    private void syncBalanceWithOtherServers(String username, double newBalance) {
        List<ServerAddress> servers = new ArrayList<>();
        servers.add(new ServerAddress("localhost", 2000)); // Server 1
        // Add more servers if needed, excluding Server 2 (self)

        // Exclude self from synchronization to prevent recursive calls
        servers.removeIf(server -> server.getPort() == 2004);

        for (ServerAddress server : servers) {
            try {
                Registry registry = LocateRegistry.getRegistry(server.getAddress(), server.getPort());
                BankService bankService = (BankService) registry.lookup("BankService");
                bankService.syncBalance(username, newBalance, true); // Pass flag to prevent further syncing
                logger.info("Successfully synced balance with server: {}", server);
            } catch (Exception e) {
                logger.error("Error syncing balance with server: {}", server, e);
            }
        }
    }

    private void syncLoginStatusWithOtherServers(String username, boolean status) throws RemoteException {
        List<ServerAddress> servers = new ArrayList<>();
        servers.add(new ServerAddress("localhost", 2000)); // Server 1
        // Add more servers if needed, excluding Server 2 (self)

        // Exclude self from synchronization to prevent recursive calls
        servers.removeIf(server -> server.getPort() == 2004);

        for (ServerAddress server : servers) {
            try {
                Registry registry = LocateRegistry.getRegistry(server.getAddress(), server.getPort());
                BankService bankService = (BankService) registry.lookup("BankService");
                bankService.syncLoginStatus(username, status, true); // Pass flag to prevent further syncing
                logger.info("Successfully synced login status with server: {}", server);
            } catch (Exception e) {
                logger.error("Error syncing login status with server: {}", server, e);
            }
        }
    }

    private void syncTransactionWithOtherServers(int userId, double amount, String description) {
        List<ServerAddress> servers = new ArrayList<>();
        servers.add(new ServerAddress("localhost", 2000)); // Server 1
        // Add more servers if needed, excluding Server 2 (self)

        // Exclude self from synchronization to prevent recursive calls
        servers.removeIf(server -> server.getPort() == 2004);

        for (ServerAddress server : servers) {
            try {
                Registry registry = LocateRegistry.getRegistry(server.getAddress(), server.getPort());
                BankService bankService = (BankService) registry.lookup("BankService");
                bankService.syncTransaction(userId, amount, description, true); // Pass flag to prevent further syncing
                logger.info("Successfully synced transaction with server: {}", server);
            } catch (Exception e) {
                logger.error("Error syncing transaction with server: {}", server, e);
            }
        }
    }

    private void syncRegisterWithOtherServers(String username, String hashedPassword, String email) {
        List<ServerAddress> servers = new ArrayList<>();
        servers.add(new ServerAddress("localhost", 2000)); // Server 1
        // servers.add(new ServerAddress("localhost", 2006)); // Server 3 (if any)

        // Exclude self from synchronization to prevent recursive calls
        servers.removeIf(server -> server.getPort() == 2004);

        for (ServerAddress server : servers) {
            try {
                Registry registry = LocateRegistry.getRegistry(server.getAddress(), server.getPort());
                BankService bankService = (BankService) registry.lookup("BankService");

                // Call syncRegister on other servers with hashed password
                bankService.syncRegister(username, hashedPassword, email);
                logger.info("Successfully synced registration with server: {}", server);
            } catch (Exception e) {
                logger.error("Error syncing registration with server: {}", server, e);
            }
        }
    }
    private void syncOtpWithOtherServers(String username, String otp) {
        List<ServerAddress> servers = new ArrayList<>();
        servers.add(new ServerAddress("localhost", 2000)); // Server 1
        // servers.add(new ServerAddress("localhost", 2006)); // Server 3 (if any)

        // Exclude self from synchronization to prevent recursive calls
        servers.removeIf(server -> server.getPort() == 2004); // Server 1's port

        for (ServerAddress server : servers) {
            try {
                Registry registry = LocateRegistry.getRegistry(server.getAddress(), server.getPort());
                BankService bankService = (BankService) registry.lookup("BankService");

                // Call syncRegister on other servers with hashed password
                bankService.syncOTP(username, otp);
                logger.info("Successfully synced registration with server: {}", server);
            } catch (Exception e) {
                logger.error("Error syncing registration with server: {}", server, e);
            }
        }
    }
    // Main Method to Start Server 2
    public static void main(String[] args) {
        try {
            // Start RMI registry on port 2004
            LocateRegistry.createRegistry(2004);
            // Bind BankService_2 to the registry
            Naming.rebind("//localhost:2004/BankService", new BankService_2());
            logger.info("Server 2 is running on port 2004...");
        } catch (Exception e) {
            logger.error("Failed to start Server 2.", e);
        }
    }
}
