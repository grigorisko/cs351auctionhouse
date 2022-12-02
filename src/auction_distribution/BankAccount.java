package auction_distribution;

public class BankAccount {
    private String accountNumber;
    private int balance;
    private int lockedBalance=0;

    //Constructor
    public BankAccount(String accountNumber, int startBalance) {
        this.accountNumber = accountNumber;
        this.balance = startBalance;
    }

    //check if balance is enough for bid
    public synchronized boolean checkBalance(int bidAmount) {
        if(bidAmount<=this.balance) {
            this.balance-=bidAmount;
            this.lockedBalance+=bidAmount;
            return true;
        }
        else {
            return false;
        }
    }

    //send a payment to specified account
    public synchronized void sendPayment(int amount,BankAccount account) {
        this.lockedBalance -= amount;
        account.receivePayment(amount);
    }

    //add to balance when receiving a payment
    public synchronized void receivePayment(int amount) {
        this.balance+=amount;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public int getBalance() {
        return balance;
    }

    public int getLockedBalance() {
        return lockedBalance;
    }
}
