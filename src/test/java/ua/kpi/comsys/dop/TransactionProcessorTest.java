package ua.kpi.comsys.dop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TransactionProcessorTest {

    private final TransactionProcessor processor = new TransactionProcessor();

    @Test
    void processDeposit() {
        Transaction deposit = new Deposit("ACC-1", new BigDecimal("150.50"));

        String expected = """
                Transaction Type: DEPOSIT
                Account: ACC-1
                Amount: USD 150.50""";

        assertEquals(expected, processor.process(deposit));
    }

    @Test
    void processWithdrawal() {
        Transaction withdrawal = new Withdrawal("ACC-2", new BigDecimal("40"), "Food");

        String expected = """
                Transaction Type: WITHDRAWAL
                Account: ACC-2
                Category: Food
                Amount: USD 40""";

        assertEquals(expected, processor.process(withdrawal));
    }

    @Test
    void processTransfer() {
        Transaction transfer = new Transfer("ACC-1", "ACC-2", new BigDecimal("999.99"));

        String expected = """
                Transaction Type: TRANSFER
                From: ACC-1
                To: ACC-2
                Amount: USD 999.99""";

        assertEquals(expected, processor.process(transfer));
    }

    @Test
    void processLargeAmountWithoutExponent() {
        Transaction deposit = new Deposit("ACC-1", new BigDecimal("1E+3"));

        String expected = """
                Transaction Type: DEPOSIT
                Account: ACC-1
                Amount: USD 1000""";

        assertEquals(expected, processor.process(deposit));
    }

    @Test
    void processNullThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> processor.process(null));

        assertEquals("Transaction cannot be null", exception.getMessage());
    }
}
