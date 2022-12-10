package auction_distribution;

public class BankAccount {
    private String accountNumber;
    private double balance;
    private double lockedBalance=0;

    //Constructor
    public BankAccount(String accountNumber, double startBalance) {
        this.accountNumber = accountNumber;
        this.balance = startBalance;
    }

    //check if balance is enough for bid
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

    //unblock agent balance
    public synchronized void unblockBalance(double unblockAmount) {
        this.lockedBalance -= unblockAmount;
        this.balance += unblockAmount;
    }

    //send a payment to specified account
    public synchronized void sendPayment(double amount,BankAccount account) {
        this.lockedBalance -= amount;
        account.receivePayment(amount);
    }

    //add to balance when receiving a payment
    public synchronized void receivePayment(double amount) {
        this.balance+=amount;
    }

    public synchronized void setStartingBalance(double amount) {
        this.balance = amount;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public double getBalance() {
        return balance;
    }

    public double getLockedBalance() {
        return lockedBalance;
    }
}
