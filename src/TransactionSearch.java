import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Search index and query for transactions (issue #4).
// Combines text search, date range and category filters with pagination,
// keeping the original order of the transactions.
public final class TransactionSearch {

    private final List<Transaction> transactions;

    // Rebuilt when the size of the list changes; swapped as a whole so a search
    // always works on one consistent snapshot.
    private volatile Index index;

    // Folds each description and category once up front, so queries don't redo it on every search.
    // The list is kept, not copied: transactions added to it later are picked up by the next search.
    public TransactionSearch(List<Transaction> transactions) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.index = buildIndex();
    }

    public SearchResult search(SearchCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria");

        List<Transaction> matches = new ArrayList<>();
        for (Entry entry : currentIndex().entries) {
            if (matches(entry, criteria)) {
                matches.add(entry.transaction);
            }
        }

        int from = (int) Math.min((long) (criteria.getPage() - 1) * criteria.getPageSize(), matches.size());
        int to = Math.min(from + criteria.getPageSize(), matches.size());
        return new SearchResult(matches.subList(from, to),
                criteria.getPage(), criteria.getPageSize(), matches.size());
    }

    private Index currentIndex() {
        Index current = index;
        if (current.sourceSize != transactions.size()) {
            current = buildIndex();
            index = current;
        }
        return current;
    }

    // Null transactions are left out, so one bad row can't break every search.
    private Index buildIndex() {
        List<Transaction> snapshot = new ArrayList<>(transactions);
        List<Entry> entries = new ArrayList<>(snapshot.size());
        for (Transaction transaction : snapshot) {
            if (transaction != null) {
                entries.add(new Entry(transaction,
                        SearchCriteria.fold(transaction.getDescription()),
                        SearchCriteria.fold(transaction.getCategory()).trim()));
            }
        }
        return new Index(entries, snapshot.size());
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

    private static final class Index {
        final List<Entry> entries;
        final int sourceSize;

        Index(List<Entry> entries, int sourceSize) {
            this.entries = entries;
            this.sourceSize = sourceSize;
        }
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
