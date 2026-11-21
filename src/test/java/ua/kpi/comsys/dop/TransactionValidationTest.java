package ua.kpi.comsys.dop;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class TransactionValidationTest {

    private static final BigDecimal AMOUNT = new BigDecimal("10");

    @Test
    void validTransactionsAreCreated() {
        assertDoesNotThrow(() -> new Deposit("ACC-1", AMOUNT));
        assertDoesNotThrow(() -> new Withdrawal("ACC-1", AMOUNT, "Food"));
        assertDoesNotThrow(() -> new Transfer("ACC-1", "ACC-2", AMOUNT));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void depositWithBadAccountThrowsException(String accountId) {
        assertThrows(IllegalArgumentException.class, () -> new Deposit(accountId, AMOUNT));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-5", "0.00"})
    void depositWithBadAmountThrowsException(BigDecimal amount) {
        assertThrows(IllegalArgumentException.class, () -> new Deposit("ACC-1", amount));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void withdrawalWithBadAccountThrowsException(String accountId) {
        assertThrows(IllegalArgumentException.class, () -> new Withdrawal(accountId, AMOUNT, "Food"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-5"})
    void withdrawalWithBadAmountThrowsException(BigDecimal amount) {
        assertThrows(IllegalArgumentException.class, () -> new Withdrawal("ACC-1", amount, "Food"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void withdrawalWithBadCategoryThrowsException(String category) {
        assertThrows(IllegalArgumentException.class, () -> new Withdrawal("ACC-1", AMOUNT, category));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void transferWithBadFromAccountThrowsException(String fromAccount) {
        assertThrows(IllegalArgumentException.class, () -> new Transfer(fromAccount, "ACC-2", AMOUNT));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void transferWithBadToAccountThrowsException(String toAccount) {
        assertThrows(IllegalArgumentException.class, () -> new Transfer("ACC-1", toAccount, AMOUNT));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-5"})
    void transferWithBadAmountThrowsException(BigDecimal amount) {
        assertThrows(IllegalArgumentException.class, () -> new Transfer("ACC-1", "ACC-2", amount));
    }
}
