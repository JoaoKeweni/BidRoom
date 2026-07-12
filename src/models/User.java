package models;

public class User {
    private String name;
    private double balance;

    public User(String name) {
        this.name = name;
        this.balance = 10000.0; // Saldo inicial generoso
    }

    public String getName() {
        return name;
    }

    public double getBalance() {
        return balance;
    }

    // Métodos para quando chegarmos no leilão
    public boolean subtractBalance(double amount) {
        if (this.balance >= amount) {
            this.balance -= amount;
            return true;
        }
        return false;
    }

    public void addBalance(double amount) {
        this.balance += amount;
    }
}
