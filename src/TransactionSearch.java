import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

// Search index and query for transactions (issue #4).
// Combines text search, date range and category filters with pagination,
// keeping the original order of the transactions.
public final class TransactionSearch {

    private final List<Entry> index;

    // Lowercases each description once up front, so queries don't redo it on every search.
    public TransactionSearch(List<Transaction> transactions) {
        Objects.requireNonNull(transactions, "transactions");
        index = new ArrayList<>(transactions.size());
        for (Transaction transaction : transactions) {
            index.add(new Entry(transaction, lower(transaction.getDescription()), lower(transaction.getCategory())));
        }
    }

    public SearchResult search(SearchCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria");

        List<Transaction> matches = new ArrayList<>();
        for (Entry entry : index) {
            if (matches(entry, criteria)) {
                matches.add(entry.transaction);
            }
        }

        int from = (int) Math.min((long) (criteria.getPage() - 1) * criteria.getPageSize(), matches.size());
        int to = Math.min(from + criteria.getPageSize(), matches.size());
        return new SearchResult(new ArrayList<>(matches.subList(from, to)),
                criteria.getPage(), criteria.getPageSize(), matches.size());
    }

    private static boolean matches(Entry entry, SearchCriteria criteria) {
        if (!criteria.getQuery().isEmpty() && !entry.description.contains(criteria.getQuery())) {
            return false;
        }
        if (!criteria.getCategories().isEmpty() && !criteria.getCategories().contains(entry.category)) {
            return false;
        }

        LocalDate start = criteria.getStartDate();
        LocalDate end = criteria.getEndDate();
        if (start != null || end != null) {
            LocalDate date = entry.transaction.getDate();
            if (date == null) {
                return false;
            }
            if (start != null && date.isBefore(start)) {
                return false;
            }
            if (end != null && date.isAfter(end)) {
                return false;
            }
        }
        return true;
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static final class Entry {
        final Transaction transaction;
        final String description;
        final String category;

        Entry(Transaction transaction, String description, String category) {
            this.transaction = transaction;
            this.description = description;
            this.category = category;
        }
    }
}
