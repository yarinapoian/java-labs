package ua.kpi.comsys.dop;

import java.math.BigDecimal;

public class TransactionProcessor {

    public String process(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        return switch (transaction) {
            case Deposit(String accountId, BigDecimal amount) -> """
                    Transaction Type: DEPOSIT
                    Account: %s
                    Amount: USD %s""".formatted(accountId, amount.toPlainString());
            case Withdrawal(String accountId, BigDecimal amount, String category) -> """
                    Transaction Type: WITHDRAWAL
                    Account: %s
                    Category: %s
                    Amount: USD %s""".formatted(accountId, category, amount.toPlainString());
            case Transfer(String fromAccount, String toAccount, BigDecimal amount) -> """
                    Transaction Type: TRANSFER
                    From: %s
                    To: %s
                    Amount: USD %s""".formatted(fromAccount, toAccount, amount.toPlainString());
        };
    }
}
