import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// Filters for TransactionSearch (issue #4). Built with chained setters:
//   new SearchCriteria().query("coffee").categories("Groceries").page(2)
// Invalid input throws IllegalArgumentException so an API layer can map it to a 400.
public final class SearchCriteria {

    public static final int DEFAULT_PAGE_SIZE = 25;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int MAX_QUERY_LENGTH = 100;

    private String query = "";
    private LocalDate startDate;
    private LocalDate endDate;
    private Set<String> categories = Collections.emptySet();
    private int page = 1;
    private int pageSize = DEFAULT_PAGE_SIZE;

    // Case-insensitive partial match on the description. Null or blank means no search.
    // The limit counts characters (code points), so an emoji counts as one.
    public SearchCriteria query(String query) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.codePointCount(0, trimmed.length()) > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("query must be at most " + MAX_QUERY_LENGTH + " characters");
        }
        this.query = fold(trimmed);
        return this;
    }

    // Inclusive on both ends. Either bound may be null for an open-ended range.
    public SearchCriteria dateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be on or before endDate");
        }
        this.startDate = startDate;
        this.endDate = endDate;
        return this;
    }

    // Matches any of the given categories, ignoring case. None means all categories.
    public SearchCriteria categories(String... categories) {
        Set<String> normalized = new HashSet<>();
        if (categories != null) {
            for (String category : categories) {
                if (category != null && !category.isBlank()) {
                    normalized.add(fold(category).trim());
                }
            }
        }
        this.categories = Collections.unmodifiableSet(normalized);
        return this;
    }

    // Pages start at 1; lower values are treated as 1.
    public SearchCriteria page(int page) {
        this.page = Math.max(1, page);
        return this;
    }

    // Clamped to 1..MAX_PAGE_SIZE.
    public SearchCriteria pageSize(int pageSize) {
        this.pageSize = Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);
        return this;
    }

<<<<<<< HEAD
    // Clears all active filters and restores the default paging state.
    public SearchCriteria reset() {
        this.query = "";
        this.startDate = null;
        this.endDate = null;
        this.categories = Collections.emptySet();
        this.page = 1;
        this.pageSize = DEFAULT_PAGE_SIZE;
        return this;
=======
    // Case folding shared by the criteria and the search index, so both sides compare alike.
    // Folds one character at a time (upper, then lower), like String.equalsIgnoreCase.
    // String.toLowerCase can turn one character into two (U+0130 becomes "i" plus a
    // combining dot), which would stop a plain "i" from matching it.
    static String fold(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder folded = new StringBuilder(value.length());
        value.codePoints()
                .map(codePoint -> Character.toLowerCase(Character.toUpperCase(codePoint)))
                .forEach(folded::appendCodePoint);
        return folded.toString();
>>>>>>> 4c7c480f8f19254bcffd0777ed1a703eb0f3c842
    }

    public String getQuery() {
        return query;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }
}
