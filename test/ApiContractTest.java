import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

// Fails when the public API of the issue #4 classes changes without
// api/transaction-search.api (and the README) being updated to match.
//
// After an intentional API change, regenerate the file with:
//   mvn test -Dtest=ApiContractTest -Dapi.update=true
class ApiContractTest {

    private static final Path CONTRACT = Path.of("api", "transaction-search.api");
    private static final List<Class<?>> PUBLIC_API =
            List.of(TransactionSearch.class, SearchCriteria.class, SearchResult.class);

    @Test
    void publicApiMatchesTheDocumentedContract() throws Exception {
        String actual = String.join("\n", describe()) + "\n";

        if (Boolean.getBoolean("api.update")) {
            Files.writeString(CONTRACT, actual);
            return;
        }

        assertEquals(Files.readString(CONTRACT), actual,
                "Public API drifted from " + CONTRACT + ". If the change is intentional, update the README "
                        + "and regenerate the contract with: mvn test -Dtest=ApiContractTest -Dapi.update=true");
    }

    private static List<String> describe() throws IllegalAccessException {
        List<String> lines = new ArrayList<>();
        for (Class<?> type : PUBLIC_API) {
            String name = type.getSimpleName();
            for (Field field : type.getDeclaredFields()) {
                int mod = field.getModifiers();
                if (Modifier.isPublic(mod)) {
                    String value = Modifier.isStatic(mod) && Modifier.isFinal(mod) ? " = " + field.get(null) : "";
                    lines.add(name + " field " + field.getType().getSimpleName() + " " + field.getName() + value);
                }
            }
            for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                if (Modifier.isPublic(constructor.getModifiers())) {
                    lines.add(name + " constructor (" + params(constructor.getParameterTypes()) + ")");
                }
            }
            for (Method method : type.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers()) && !method.isSynthetic()) {
                    lines.add(name + " method " + method.getName() + "(" + params(method.getParameterTypes())
                            + ") : " + method.getGenericReturnType().getTypeName());
                }
            }
        }
        lines.sort(null);
        return lines;
    }

    private static String params(Class<?>[] types) {
        return Arrays.stream(types).map(Class::getSimpleName).collect(Collectors.joining(", "));
    }
}
