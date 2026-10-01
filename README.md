# Transaction History Filter

A transaction history app in Java. Users can filter and search their transactions by date, category and text.

| File | What it does | Owner |
|---|---|---|
| `src/Transaction.java` | A single transaction | Katie |
| `src/TransactionService.java` | Stores transactions; filters by category and date | Katie |
| `src/Main.java` | Demo program | Katie |
| `src/TransactionSearch.java`, `src/SearchCriteria.java`, `src/SearchResult.java` | Text search combined with date and category filters, plus pagination | Issue #4 |

## Transaction search (issue [katiehutc/portfolio#4](https://github.com/katiehutc/portfolio/issues/4))

`TransactionService` filters by one thing at a time. `TransactionSearch` adds **text search** and lets you combine text, date range and categories in one query, with paging for long histories.

```java
TransactionSearch search = new TransactionSearch(service.getTransactions());

SearchResult result = search.search(new SearchCriteria()
        .query("grocery")
        .dateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))
        .categories("Groceries")
        .page(1)
        .pageSize(25));

result.getItems();      // the transactions on this page
result.getTotal();      // how many matched in total
result.getTotalPages();
```

## API

> This section and `api/transaction-search.api` are the documented public API. If you change the public API, update both in the same commit. CI and the AI review check that they match the code.

### `TransactionSearch`

| Member | Behavior |
|---|---|
| `new TransactionSearch(List<Transaction>)` | Builds the search index. Descriptions and categories are lowercased once, here. Throws `NullPointerException` for a null list. |
| `SearchResult search(SearchCriteria)` | Returns the matching page. Keeps the original order. Throws `NullPointerException` for null criteria. |

### `SearchCriteria`

Each setter returns the same `SearchCriteria`, so calls can be chained. All filters combine with AND.

| Setter | Default | Behavior |
|---|---|---|
| `query(String)` | no search | Case-insensitive partial match on the description. Null or blank means no search. Special characters are matched as plain text. Throws `IllegalArgumentException` if longer than `MAX_QUERY_LENGTH` (100) characters. |
| `dateRange(LocalDate start, LocalDate end)` | no range | Inclusive. Either end may be null for an open-ended range. Transactions without a date are left out when a range is set. Throws `IllegalArgumentException` if `start` is after `end`. |
| `categories(String...)` | all categories | Matches any of the given categories (OR), ignoring case. Null and blank values are ignored. |
| `page(int)` | `1` | Values below 1 become 1. |
| `pageSize(int)` | `DEFAULT_PAGE_SIZE` (25) | Clamped to 1–`MAX_PAGE_SIZE` (100). |

Each setter has a matching getter (`getQuery()`, `getStartDate()`, `getEndDate()`, `getCategories()`, `getPage()`, `getPageSize()`).

Error messages never repeat the user's input back.

### `SearchResult`

| Method | Returns |
|---|---|
| `getItems()` | The transactions on this page. The list can't be modified. |
| `getPage()`, `getPageSize()` | The page and page size actually used, after clamping |
| `getTotal()` | Total number of matches across all pages |
| `getTotalPages()` | Number of pages; `0` when nothing matched |

A page past the end returns no items, with the correct total.

### Change to `TransactionService`

Added `List<Transaction> getTransactions()`, a read-only view of the stored transactions, so `TransactionSearch` can use them. No existing methods changed.

## Building and testing

You need Java 17 or newer and Maven.

```bash
mvn verify
```

This compiles everything, runs the tests, and fails if coverage of the issue #4 classes drops below 90%. The coverage report is written to `target/site/jacoco/index.html`.

| Test | What it covers |
|---|---|
| `test/TransactionSearchTest.java` | Text search, date ranges, categories, combined filters, pagination, input limits, unmodifiable results, and use with `TransactionService` |
| `test/ApiContractTest.java` | Fails if the public API of the search classes changes without updating `api/transaction-search.api` |

After an **intentional** API change, update the API section above, then regenerate the contract file:

```bash
mvn test -Dtest=ApiContractTest -Dapi.update=true
```

To run Katie's demo:

```bash
javac -d out src/*.java && java -cp out Main
```

## Automated checks

| Check | Workflow | Runs on | Blocks merge? |
|---|---|---|---|
| JUnit tests with a 90% coverage threshold | `ci.yml` | every push to `main` and every PR | Yes |
| API contract test (catches undocumented API drift) | `ci.yml` | every push to `main` and every PR | Yes |
| CodeQL security scan | `ci.yml` | every push to `main` and every PR | Shows alerts in the Security tab |
| Dependency vulnerability check | `ci.yml` | PRs | Yes, for high severity |
| AI security and API drift review (Claude) | `ai-review.yml` | every push to `main` and every PR | No. On a PR it posts review comments; on a push to `main` it opens an issue only if it finds a high or critical problem or API drift. |

### One-time setup for the AI review

1. Get an API key from [console.anthropic.com](https://console.anthropic.com).
2. In this repo, go to **Settings → Secrets and variables → Actions → New repository secret**.
3. Name it `ANTHROPIC_API_KEY` and paste in the key.

Without the secret, only the AI review job fails; the other checks still run. Each review uses a small amount of API credit.
