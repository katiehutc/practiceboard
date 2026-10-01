import java.util.Collections;
import java.util.List;

// One page of TransactionSearch results plus the totals needed for pagination.
public final class SearchResult {

    private final List<Transaction> items;
    private final int page;
    private final int pageSize;
    private final int total;

    public SearchResult(List<Transaction> items, int page, int pageSize, int total) {
        this.items = Collections.unmodifiableList(items);
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
        return (total + pageSize - 1) / pageSize;
    }
}
