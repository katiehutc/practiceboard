import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransactionService {

    private List<Transaction> transactions;

    public TransactionService() {
        transactions = new ArrayList<>();
    }

    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
    }

    // Filter by category
    public List<Transaction> filterByCategory(String category) {
        List<Transaction> results = new ArrayList<>();

        for (Transaction transaction : transactions) {
            if (transaction.getCategory().equalsIgnoreCase(category)) {
                results.add(transaction);
            }
        }

        return results;
    }

    // Filter by date
    public List<Transaction> filterByDate(LocalDate date) {
        List<Transaction> results = new ArrayList<>();

        for (Transaction transaction : transactions) {
            if (transaction.getDate().equals(date)) {
                results.add(transaction);
            }
        }

        return results;
    }

    // Filter by date range
    public List<Transaction> filterByDateRange(LocalDate startDate, LocalDate endDate) {
        List<Transaction> results = new ArrayList<>();

        for (Transaction transaction : transactions) {
            LocalDate transactionDate = transaction.getDate();

            if (!transactionDate.isBefore(startDate)
                    && !transactionDate.isAfter(endDate)) {
                results.add(transaction);
            }
        }

        return results;
    }

    // Search by category AND date range
    public List<Transaction> search(String category,
                                    LocalDate startDate,
                                    LocalDate endDate) {

        List<Transaction> results = new ArrayList<>();

        for (Transaction transaction : transactions) {

            boolean categoryMatches = category == null
                    || transaction.getCategory().equalsIgnoreCase(category);

            boolean dateMatches = (startDate == null || !transaction.getDate().isBefore(startDate))
                    && (endDate == null || !transaction.getDate().isAfter(endDate));

            if (categoryMatches && dateMatches) {
                results.add(transaction);
            }
        }

        return results;
    }
}
