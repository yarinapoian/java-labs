package ua.kpi.comsys.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ActionHistoryManagerTest {

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    void constructorWithBadCapacityThrowsException(int maxCapacity) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new ActionHistoryManager(maxCapacity));

        assertEquals("maxCapacity must be greater than 0", exception.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void performActionWithBadActionThrowsException(String action) {
        ActionHistoryManager manager = new ActionHistoryManager(3);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> manager.performAction(action));

        assertEquals("Action cannot be null or blank", exception.getMessage());
        assertEquals(0, manager.getHistorySize());
    }

    @Test
    void newManagerHasEmptyHistory() {
        ActionHistoryManager manager = new ActionHistoryManager(3);

        assertEquals(0, manager.getHistorySize());
        assertTrue(manager.getHistory().isEmpty());
        assertNull(manager.peekRecentAction());
        assertNull(manager.undo());
    }

    @Test
    void historyIsFromNewestToOldest() {
        ActionHistoryManager manager = new ActionHistoryManager(5);
        manager.performAction("open");
        manager.performAction("edit");
        manager.performAction("save");

        assertEquals(List.of("save", "edit", "open"), manager.getHistory());
        assertEquals(3, manager.getHistorySize());
    }

    @Test
    void oldestActionIsRemovedWhenCapacityIsFull() {
        ActionHistoryManager manager = new ActionHistoryManager(2);
        manager.performAction("first");
        manager.performAction("second");
        manager.performAction("third");

        assertEquals(List.of("third", "second"), manager.getHistory());
        assertEquals(2, manager.getHistorySize());
    }

    @Test
    void capacityOfOneKeepsOnlyLastAction() {
        ActionHistoryManager manager = new ActionHistoryManager(1);
        manager.performAction("first");
        manager.performAction("second");

        assertEquals(List.of("second"), manager.getHistory());
    }

    @Test
    void undoRemovesAndReturnsNewestAction() {
        ActionHistoryManager manager = new ActionHistoryManager(3);
        manager.performAction("first");
        manager.performAction("second");

        assertEquals("second", manager.undo());
        assertEquals("first", manager.undo());
        assertNull(manager.undo());
        assertEquals(0, manager.getHistorySize());
    }

    @Test
    void peekReturnsNewestActionAndKeepsIt() {
        ActionHistoryManager manager = new ActionHistoryManager(3);
        manager.performAction("first");
        manager.performAction("second");

        assertEquals("second", manager.peekRecentAction());
        assertEquals("second", manager.peekRecentAction());
        assertEquals(2, manager.getHistorySize());
    }

    @Test
    void changingReturnedListDoesNotChangeHistory() {
        ActionHistoryManager manager = new ActionHistoryManager(3);
        manager.performAction("first");

        List<String> history = manager.getHistory();
        history.add("fake");
        history.remove("first");

        assertEquals(List.of("first"), manager.getHistory());
    }

    @Test
    void clearRemovesAllActions() {
        ActionHistoryManager manager = new ActionHistoryManager(3);
        manager.performAction("first");
        manager.performAction("second");

        manager.clear();

        assertEquals(0, manager.getHistorySize());
        assertTrue(manager.getHistory().isEmpty());
        assertNull(manager.peekRecentAction());
    }

    @Test
    void managerWorksAfterClear() {
        ActionHistoryManager manager = new ActionHistoryManager(2);
        manager.performAction("first");
        manager.clear();
        manager.performAction("second");

        assertEquals(List.of("second"), manager.getHistory());
    }
}
