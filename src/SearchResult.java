import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// One page of TransactionSearch results plus the totals needed for pagination.
public final class SearchResult {

    private final List<Transaction> items;
    private final int page;
    private final int pageSize;
    private final int total;

    // Copies the items, so later changes to the caller's list don't change the result.
    public SearchResult(List<Transaction> items, int page, int pageSize, int total) {
        if (page < 1 || pageSize < 1 || total < 0) {
            throw new IllegalArgumentException("page and pageSize must be at least 1, and total at least 0");
        }
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
    }

    public List<Transaction> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotal() {
        return total;
    }

    public int getTotalPages() {
        return (int) (((long) total + pageSize - 1) / pageSize);
    }
}
