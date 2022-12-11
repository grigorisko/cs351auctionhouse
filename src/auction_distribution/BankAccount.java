/**
 * Author:  Fermin Ramos, Vasileios Grigorios Kourakos and Loc Tung Sy
 * Email: locsu@unm.edu, ramosfer@unm.edu, grigorisk@unm.edu
 * Class: Cs 351L
 * Professor: Brooke Chenoweth
 * Project 5: AuctionHouse Distribution
 * The BankAccount.java responsible for making the Bank server behave
 * like a real life bank, such as checking Bank Balance, Blocking and Unblocking Fund
 * Receive incoming payment, sending out payment, etc
 */
package auction_distribution;

public class BankAccount {
    private String accountNumber;
    private double balance;
    private double lockedBalance=0;

    /**
     * Bank Account constructor that take in the account number
     * and the initial bank balance
     * @param accountNumber the account number
     * @param startBalance the starting bank balance
     */
    public BankAccount(String accountNumber, double startBalance) {
        this.accountNumber = accountNumber;
        this.balance = startBalance;
    }

    /**
     * Check if the newly bid request is valid or not
     * @param bidAmount the bid amount
     * @return true if the bid amount is lesser than the current bank balance
     * false if the bid amount is higher than the the current bank balance
     */
    public synchronized boolean checkBalance(double bidAmount) {
        if(bidAmount<=this.balance) {
            this.balance-=bidAmount;
            this.lockedBalance+=bidAmount;
            return true;
        }
        else {
            return false;
        }
    }


    /**
     * Unblocking the balance if the bid got outbid-ed by
     * another agent
     * @param unblockAmount the amount that need to be unblock
     */
    public synchronized void unblockBalance(double unblockAmount) {
        this.lockedBalance -= unblockAmount;
        this.balance += unblockAmount;
    }



    /**
     * Sending the payment when a bid is through
     * @param amount the sending amount
     * @param account the account number to send to
     */
    public synchronized void sendPayment(double amount,BankAccount account) {
        this.lockedBalance -= amount;
        account.receivePayment(amount);
    }



    /**
     * Add the amount to the balance
     * @param amount the amount of money
     */
    public synchronized void receivePayment(double amount) {
        this.balance+=amount;
    }

    /**
     * Set the initial balance
     * @param amount the amount of money
     */
    public synchronized void setStartingBalance(double amount) {
        this.balance = amount;
    }

    /**
     * Get the account number
     * @return the account number
     */
    public String getAccountNumber() {
        return accountNumber;
    }
    /**
     * Get the balance
     * @return the account balance
     */
    public double getBalance() {
        return balance;
    }
    /**
     * Get the locked balance
     * @return the locked balance
     */
    public double getLockedBalance() {
        return lockedBalance;
    }
}
