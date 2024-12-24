package org.dacs.Client;

import org.dacs.Common.BankService;
import org.dacs.Common.TransactionResult;

import java.rmi.Naming;
import java.util.Scanner;

public class Client_1 {
    public static void main(String[] args) {
        try {
            BankService server1 = (BankService) Naming.lookup("//localhost:2000/BankService");
            BankService server2 = (BankService) Naming.lookup("//localhost:2004/BankService");

            String username = "";
            String password = "";
            double amount = 89777;
            Scanner scanner = new Scanner(System.in);
            System.out.println("Enter username: ");
            username = scanner.nextLine();
            System.out.println("Enter password: ");
            password = scanner.nextLine();

            if (server1.login(username, password)) {
                System.out.println("Login to Server 1: Success");
                double balance = server1.getBalance(username);
                System.out.println("Balance of user " + username + " on Server 1: " + balance);
                System.out.println("Enter amount to withdraw: ");
                amount = scanner.nextDouble();
                scanner.nextLine(); // Consume newline
                System.out.println("Enter description for withdrawal: ");
                String description = scanner.nextLine();
                TransactionResult withdrawResult = server1.withdraw(username, amount, description);
                if (withdrawResult.isSuccess()) {
                    System.out.println("Withdraw from Server 1: Success");
                } else {
                    System.out.println("Withdraw from Server 1: Failed - " + withdrawResult.getDescription());
                }
                System.out.println("Withdraw from Server 1: " + withdrawResult);
                balance = server1.getBalance(username);
                System.out.println("Balance of user " + username + " on Server 1 after draw: " + amount + " is: " + balance);
            } else {
                System.out.println("Login to Server 1: Failed");
            }
            boolean isOnline = server2.isUserOnline("admin");
            System.out.println("Is user 'admin' online on Server 2: " + isOnline);
            double balance = server2.getBalance(username);
            System.out.println("Balance of user " + username + " on Server 2: " + balance);

            server1.logout(username);
            server1.setBalance(username, 1000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}