import java.time.LocalDate;

public class Transaction {
    private int id;
    private String description;
    private double amount;
    private String category;
    private LocalDate date;

    public Transaction(int id, String description, double amount, String category, LocalDate date) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public LocalDate getDate() {
        return date;
    }

    @Override
    public String toString() {
        return id + " | " + date + " | " + category + " | "
                + description + " | $" + amount;
    }
}
