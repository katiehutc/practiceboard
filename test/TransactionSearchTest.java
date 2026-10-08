import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TransactionSearchTest {

    private static final List<Transaction> TRANSACTIONS = List.of(
            new Transaction(1, "Starbucks Coffee", 5.25, "Food", LocalDate.of(2026, 9, 1)),
            new Transaction(2, "Rent", 1200.00, "Housing", LocalDate.of(2026, 9, 3)),
            new Transaction(3, "Whole Foods", 82.40, "Food", LocalDate.of(2026, 9, 10)),
            new Transaction(4, "C++ Book (50% off)", 24.99, "Education", LocalDate.of(2026, 9, 15)),
            new Transaction(5, "Refund", -20.00, "Other", null));

    private final TransactionSearch search = new TransactionSearch(TRANSACTIONS);

    private List<Integer> ids(SearchCriteria criteria) {
        return search.search(criteria).getItems().stream().map(Transaction::getId).collect(Collectors.toList());
    }

    @Nested
    class TextSearch {
        @Test
        void exactNameReturnsTheExpectedTransaction() {
            assertEquals(List.of(2), ids(new SearchCriteria().query("Rent")));
        }

        @Test
        void partialMatch() {
            assertEquals(List.of(1), ids(new SearchCriteria().query("star")));
        }

        @Test
        void caseInsensitive() {
            assertEquals(List.of(3), ids(new SearchCriteria().query("WHOLE foods")));
        }

        @Test
        void emptyOrWhitespaceQueryReturnsAllInOriginalOrder() {
            assertEquals(List.of(1, 2, 3, 4, 5), ids(new SearchCriteria().query("")));
            assertEquals(List.of(1, 2, 3, 4, 5), ids(new SearchCriteria().query("   ")));
            assertEquals(List.of(1, 2, 3, 4, 5), ids(new SearchCriteria().query(null)));
        }

        @Test
        void noMatchesReturnsAnEmptyPageNotAnError() {
            SearchResult result = search.search(new SearchCriteria().query("zzz"));
            assertTrue(result.getItems().isEmpty());
            assertEquals(0, result.getTotal());
            assertEquals(0, result.getTotalPages());
        }

        @Test
        void resetClearsAllActiveFiltersToDefaults() {
            SearchCriteria criteria = new SearchCriteria()
                    .query("coffee")
                    .dateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))
                    .categories("Food", "Housing")
                    .page(3)
                    .pageSize(50);

            criteria.reset();

            assertEquals("", criteria.getQuery());
            assertEquals(null, criteria.getStartDate());
            assertEquals(null, criteria.getEndDate());
            assertTrue(criteria.getCategories().isEmpty());
            assertEquals(1, criteria.getPage());
            assertEquals(SearchCriteria.DEFAULT_PAGE_SIZE, criteria.getPageSize());
        }

        @Test
        void emptyResultsProvideAClearEmptyState() {
            SearchResult result = search.search(new SearchCriteria()
                    .query("zzz")
                    .dateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))
                    .categories("Food"));

            assertTrue(result.getItems().isEmpty());
            assertEquals(0, result.getTotal());
            assertEquals(0, result.getTotalPages());
            assertEquals(1, result.getPage());
            assertEquals(SearchCriteria.DEFAULT_PAGE_SIZE, result.getPageSize());
        }

        @Test
        void specialCharactersAreMatchedLiterally() {
            assertEquals(List.of(4), ids(new SearchCriteria().query("c++")));
            assertEquals(List.of(4), ids(new SearchCriteria().query("(50%")));
            assertEquals(List.of(), ids(new SearchCriteria().query(".*")));
        }

        @Test
        void queriesOverTheLimitAreRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> new SearchCriteria().query("a".repeat(SearchCriteria.MAX_QUERY_LENGTH + 1)));
            new SearchCriteria().query("a".repeat(SearchCriteria.MAX_QUERY_LENGTH));
        }

        @Test
        void errorMessagesDoNotEchoInput() {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> new SearchCriteria().query("<script>" + "a".repeat(200)));
            assertFalse(error.getMessage().contains("<script>"));
        }
    }

    @Nested
    class DateRange {
        @Test
        void boundsAreInclusive() {
            assertEquals(List.of(2, 3), ids(new SearchCriteria()
                    .dateRange(LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 10))));
        }

        @Test
        void openEndedRanges() {
            assertEquals(List.of(3, 4), ids(new SearchCriteria().dateRange(LocalDate.of(2026, 9, 10), null)));
            assertEquals(List.of(1), ids(new SearchCriteria().dateRange(null, LocalDate.of(2026, 9, 1))));
        }

        @Test
        void transactionsWithoutADateAreExcludedOnlyWhenARangeIsSet() {
            assertFalse(ids(new SearchCriteria().dateRange(LocalDate.of(2000, 1, 1), null)).contains(5));
            assertTrue(ids(new SearchCriteria()).contains(5));
        }

        @Test
        void startAfterEndIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> new SearchCriteria()
                    .dateRange(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 1)));
        }
    }

    @Nested
    class Categories {
        @Test
        void multipleCategoriesAreOredAndCaseInsensitive() {
            assertEquals(List.of(2, 4), ids(new SearchCriteria().categories("housing", "EDUCATION")));
        }

        @Test
        void blankAndNullCategoriesAreIgnored() {
            assertEquals(List.of(1, 2, 3, 4, 5), ids(new SearchCriteria().categories("", null, "  ")));
            assertEquals(List.of(1, 2, 3, 4, 5), ids(new SearchCriteria().categories((String[]) null)));
        }
    }

    @Nested
    class Combined {
        @Test
        void searchDateAndCategoryAreAndedTogether() {
            assertEquals(List.of(1, 3), ids(new SearchCriteria()
                    .query("o")
                    .dateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))
                    .categories("Food")));
        }

        @Test
        void worksWithTransactionService() {
            TransactionService service = new TransactionService();
            TRANSACTIONS.forEach(service::addTransaction);
            SearchResult result = new TransactionSearch(service.getTransactions())
                    .search(new SearchCriteria().categories("Food"));
            assertEquals(2, result.getTotal());
        }

            @Test
            void transactionServiceFiltersByMultipleCategories() {
                TransactionService service = new TransactionService();
                TRANSACTIONS.forEach(service::addTransaction);

                List<Integer> matchedIds = service.filterByCategories(" housing ", "EDUCATION")
                    .stream().map(Transaction::getId).collect(Collectors.toList());
                assertEquals(List.of(2, 4), matchedIds);
            }

            @Test
            void transactionServiceTreatsEmptyCategoryFiltersAsUnfiltered() {
                TransactionService service = new TransactionService();
                TRANSACTIONS.forEach(service::addTransaction);

                assertEquals(List.of(1, 2, 3, 4, 5), service.filterByCategories().stream()
                    .map(Transaction::getId).collect(Collectors.toList()));
                assertEquals(List.of(1, 2, 3, 4, 5), service.filterByCategories("", null).stream()
                    .map(Transaction::getId).collect(Collectors.toList()));
                assertEquals(List.of(1, 2, 3, 4, 5), service.filterByCategories((String[]) null).stream()
                    .map(Transaction::getId).collect(Collectors.toList()));
            }
    }

    @Nested
    class Pagination {
        @Test
        void paginatesAfterFilteringAndReportsTotals() {
            SearchResult result = search.search(new SearchCriteria().page(2).pageSize(2));
            assertEquals(List.of(3, 4), result.getItems().stream().map(Transaction::getId).collect(Collectors.toList()));
            assertEquals(2, result.getPage());
            assertEquals(2, result.getPageSize());
            assertEquals(5, result.getTotal());
            assertEquals(3, result.getTotalPages());
        }

        @Test
        void pagePastTheEndReturnsEmptyItemsWithTheCorrectTotal() {
            SearchResult result = search.search(new SearchCriteria().page(99).pageSize(2));
            assertTrue(result.getItems().isEmpty());
            assertEquals(5, result.getTotal());
        }

        @Test
        void hugePageNumbersDoNotOverflow() {
            assertTrue(search.search(new SearchCriteria().page(Integer.MAX_VALUE).pageSize(100)).getItems().isEmpty());
        }

        @Test
        void clampsBadPageAndPageSizeValues() {
            assertEquals(1, new SearchCriteria().page(0).getPage());
            assertEquals(1, new SearchCriteria().page(-3).getPage());
            assertEquals(1, new SearchCriteria().pageSize(0).getPageSize());
            assertEquals(SearchCriteria.MAX_PAGE_SIZE, new SearchCriteria().pageSize(10_000).getPageSize());
            assertEquals(SearchCriteria.DEFAULT_PAGE_SIZE, new SearchCriteria().getPageSize());
        }

        @Test
        void resultsCannotBeModified() {
            SearchResult result = search.search(new SearchCriteria());
            assertThrows(UnsupportedOperationException.class, () -> result.getItems().clear());
        }
    }

    @Test
    void nullInputsAreRejected() {
        assertThrows(NullPointerException.class, () -> new TransactionSearch(null));
        assertThrows(NullPointerException.class, () -> search.search(null));
    }
}
