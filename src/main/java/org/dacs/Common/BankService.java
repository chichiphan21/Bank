package org.dacs.Common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface BankService extends Remote {
    // Send OTP when register
    boolean sendOTP(String to, String subject, String text) throws RemoteException;
//    Verify OTP when register
    boolean verifyOTP(String username, String otp) throws RemoteException;
    // User Management
    boolean register(String username, String password, String email) throws RemoteException;
    boolean login(String username, String password) throws RemoteException;
    void logout(String username) throws RemoteException;
    boolean isUserOnline(String username) throws RemoteException;

    // Financial Operations

    TransactionResult withdraw(String username, double amount, String description) throws RemoteException;
    double getBalance(String username) throws RemoteException;
    void setBalance(String username, double newBalance) throws RemoteException;

    // Transaction History
    List<String> getTransactionHistory(String username) throws RemoteException;

    // Synchronization Methods
    boolean syncRegister(String username, String hashedPassword, String email) throws RemoteException;
    void syncBalance(String username, double newBalance, boolean isSyncCall) throws RemoteException;
    void syncLoginStatus(String username, boolean status, boolean isSyncCall) throws RemoteException;
    void syncTransaction(int userId, double amount, String description, boolean isSyncCall) throws RemoteException;
    void syncOTP(String username, String otp) throws RemoteException;
}
