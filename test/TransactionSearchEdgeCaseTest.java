import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

// Edge cases the transaction search does not handle yet. Each test states the rule
// it is checking (from the README's API section) and fails against the current code.
class TransactionSearchEdgeCaseTest {

    private static final LocalDate DATE = LocalDate.of(2026, 9, 1);

    private static List<Integer> ids(SearchResult result) {
        return result.getItems().stream().map(Transaction::getId).collect(Collectors.toList());
    }

    // Rule: categories match "ignoring case", and the criteria side is trimmed.
    // TransactionService.filterByCategories trims the stored category too, so the
    // same data gives different answers depending on which class is asked.
    @Test
    void storedCategoriesWithSurroundingWhitespaceStillMatch() {
        TransactionService service = new TransactionService();
        service.addTransaction(new Transaction(1, "Whole Foods", 82.40, " Food ", DATE));

        assertEquals(1, service.filterByCategories("food").size());
        assertEquals(List.of(1), ids(new TransactionSearch(service.getTransactions())
                .search(new SearchCriteria().categories("food"))));
    }

    // Rule: "Case-insensitive partial match on the description."
    // Lowercasing U+0130 gives "i" plus a combining dot, so a plain "istanbul" misses it.
    @Test
    void caseInsensitiveMatchWorksForNonAsciiDescriptions() {
        TransactionSearch search = new TransactionSearch(List.of(
                new Transaction(1, "İSTANBUL KEBAB", 18.00, "Food", DATE)));

        assertEquals(List.of(1), ids(search.search(new SearchCriteria().query("istanbul"))));
    }

    // Rule: queries up to MAX_QUERY_LENGTH (100) characters are accepted.
    // The cap counts UTF-16 units, so 60 emoji are rejected as "over 100 characters".
    @Test
    void queryLengthLimitCountsCharactersNotUtf16Units() {
        String sixtyEmoji = "🍕".repeat(60);

        assertDoesNotThrow(() -> new SearchCriteria().query(sixtyEmoji));
    }

    // Rule: the constructor throws NullPointerException "for a null list" only.
    // TransactionService.addTransaction accepts null, so one bad row currently
    // makes every search over that service impossible.
    @Test
    void nullTransactionsInTheListAreSkippedNotFatal() {
        List<Transaction> transactions = Arrays.asList(
                new Transaction(1, "Rent", 1200.00, "Housing", DATE),
                null,
                new Transaction(3, "Refund", -20.00, "Other", DATE));

        TransactionSearch search = assertDoesNotThrow(() -> new TransactionSearch(transactions));
        assertEquals(List.of(1, 3), ids(search.search(new SearchCriteria())));
    }

    // Rule: the README usage builds the search from service.getTransactions(), a live
    // view. Transactions added afterwards are silently missing from every search.
    @Test
    void transactionsAddedAfterTheSearchIsBuiltAreFound() {
        TransactionService service = new TransactionService();
        service.addTransaction(new Transaction(1, "Rent", 1200.00, "Housing", DATE));
        TransactionSearch search = new TransactionSearch(service.getTransactions());

        service.addTransaction(new Transaction(2, "Rent", 1200.00, "Housing", DATE.plusMonths(1)));

        assertEquals(List.of(1, 2), ids(search.search(new SearchCriteria().query("rent"))));
    }

    // Rule: getItems() — "The list can't be modified."
    // SearchResult wraps the caller's list without copying it, so changing that
    // list afterwards changes the result.
    @Test
    void resultItemsDoNotChangeWhenTheSourceListChanges() {
        List<Transaction> source = new ArrayList<>(List.of(
                new Transaction(1, "Rent", 1200.00, "Housing", DATE)));
        SearchResult result = new SearchResult(source, 1, 25, 1);

        source.clear();

        assertEquals(List.of(1), ids(result));
    }

    // Rule: "Invalid input throws IllegalArgumentException" and page sizes are 1-100.
    // The public SearchResult constructor accepts a page size of 0, and
    // getTotalPages() then fails with ArithmeticException (divide by zero).
    @Test
    void resultRejectsANonPositivePageSize() {
        assertThrows(IllegalArgumentException.class,
                () -> new SearchResult(List.of(), 1, 0, 5).getTotalPages());
    }
}
