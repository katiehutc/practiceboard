import java.time.LocalDate;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        TransactionService service = new TransactionService();

        // Add some transactions
        service.addTransaction(
                new Transaction(
                        1,
                        "Grocery Store",
                        54.32,
                        "Groceries",
                        LocalDate.of(2026, 9, 5)
                )
        );

        service.addTransaction(
                new Transaction(
                        2,
                        "Netflix",
                        15.99,
                        "Entertainment",
                        LocalDate.of(2026, 9, 10)
                )
        );

        service.addTransaction(
                new Transaction(
                        3,
                        "Gas Station",
                        42.50,
                        "Transportation",
                        LocalDate.of(2026, 9, 12)
                )
        );

        service.addTransaction(
                new Transaction(
                        4,
                        "Target",
                        35.75,
                        "Groceries",
                        LocalDate.of(2026, 9, 15)
                )
        );

        service.addTransaction(
                new Transaction(
                        5,
                        "Grocery Store",
                        72.10,
                        "Groceries",
                        LocalDate.of(2026, 9, 20)
                )
        );

        // Search for groceries during September
        List<Transaction> results = service.search(
                "Groceries",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        System.out.println("Search Results:");

        for (Transaction transaction : results) {
            System.out.println(transaction);
        }
    }
}
