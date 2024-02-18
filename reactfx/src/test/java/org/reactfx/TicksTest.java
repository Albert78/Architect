package org.reactfx;

import static org.junit.Assert.*;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.reactfx.util.FxTimer;


public class TicksTest {
    private ScheduledExecutorService scheduler;

    @Before
    public void setUp() throws Exception {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        new JFXPanel(); // initializes JavaFX toolkit
    }

    @After
    public void tearDown() throws Exception {
        scheduler.shutdown();
    }

    @Test
    public void fxTicksTest() throws InterruptedException, ExecutionException {
        CompletableFuture<Integer> nTicks = new CompletableFuture<>();
        Platform.runLater(() -> {
            EventCounter counter = new EventCounter();
            Subscription sub = EventStreams.ticks(Duration.ofMillis(100)).subscribe(counter::accept);
            FxTimer.runLater(Duration.ofMillis(350), sub::unsubscribe); // stop after 3 ticks
            // wait a little more to test that no more than 3 ticks arrive anyway
            FxTimer.runLater(Duration.ofMillis(550), () -> nTicks.complete(counter.get()));
        });
        assertEquals(3, nTicks.get().intValue());
    }

    @Test
    public void fxTicks0Test() throws InterruptedException, ExecutionException {
        CompletableFuture<Integer> nTicks = new CompletableFuture<>();
        Platform.runLater(() -> {
	        EventCounter counter = new EventCounter();
	        Subscription sub = EventStreams.ticks0(Duration.ofMillis(100)).subscribe(counter::accept);
	        // 000 (tick 1) -> 100 (tick 2) -> 200 (tick 3) -> 300 (tick 4) -> 350 (interrupted) = 4 ticks
	        FxTimer.runLater(Duration.ofMillis(350), sub::unsubscribe); // stop after 4 ticks
	        // wait a little more to test that no more than 4 ticks arrive anyway
	        FxTimer.runLater(Duration.ofMillis(550), () -> nTicks.complete(counter.get()));
        });
        assertEquals(4, nTicks.get().intValue());
    }

    @Test
    public void fxRestartableTicksTest() throws InterruptedException, ExecutionException {
        CompletableFuture<Integer> nTicks = new CompletableFuture<>();
        Platform.runLater(() -> {
            EventCounter counter = new EventCounter();
            EventSource<?> impulse = new EventSource<Void>();
            Subscription sub = EventStreams.restartableTicks(Duration.ofMillis(100), impulse)
                    .subscribe(counter::accept);
            FxTimer.runLater(Duration.ofMillis(400), sub::unsubscribe);
            FxTimer.runLater(Duration.ofMillis(80),() -> impulse.push(null));
            FxTimer.runLater(Duration.ofMillis(260),() -> impulse.push(null));
            // 000: Start -> 80 (restart)
            // 080: Start
            // 180: End (tick)
            // 180: Start -> 80 (restart)
            // 260: Start
            // 360: End (tick)
            // 400: unsubscribed: 2 ticks
            // wait a little more to test that no more ticks arrive anyway
            FxTimer.runLater(Duration.ofMillis(550), () -> nTicks.complete(counter.get()));
        });
        assertEquals(2, nTicks.get().intValue());
    }

    @Test
    public void fxRestartableTicks0Test() throws InterruptedException, ExecutionException {
        CompletableFuture<Integer> nTicks = new CompletableFuture<>();
        Platform.runLater(() -> {
            EventCounter counter = new EventCounter();
            EventSource<?> impulse = new EventSource<Void>();
            Subscription sub = EventStreams.restartableTicks0(Duration.ofMillis(100), impulse)
                    .subscribe(counter::accept);
            FxTimer.runLater(Duration.ofMillis(400), sub::unsubscribe);
            FxTimer.runLater(Duration.ofMillis(80), () -> impulse.push(null));
            FxTimer.runLater(Duration.ofMillis(260), () -> impulse.push(null));
            // 000: 0 (tick) -> 80 (restart)
            // 080: 0 (tick)
            // 180: 0 (tick) -> 80 (restart)
            // 260: 0 (tick)
            // 360: 0 (tick)
            // 400: unsubscribed: 5 ticks
            // wait a little more to test that no more ticks arrive anyway
            FxTimer.runLater(Duration.ofMillis(550), () -> nTicks.complete(counter.get()));
        });
        assertEquals(5, nTicks.get().intValue());
    }

    @Test
    public void executorTest() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        CompletableFuture<Integer> nTicks = new CompletableFuture<>();
        executor.execute(() -> {
            EventCounter counter = new EventCounter();
            Subscription sub = EventStreams.ticks(Duration.ofMillis(100), scheduler, executor).subscribe(counter::accept);
            ScheduledExecutorServiceTimer.create(Duration.ofMillis(350), sub::unsubscribe, scheduler, executor).restart(); // stop after 3 ticks
            // wait a little more to test that no more than 3 ticks arrive anyway
            ScheduledExecutorServiceTimer.create(Duration.ofMillis(550), () -> nTicks.complete(counter.get()), scheduler, executor).restart();
        });
        assertEquals(3, nTicks.get().intValue());

        executor.shutdown();
    }
}
