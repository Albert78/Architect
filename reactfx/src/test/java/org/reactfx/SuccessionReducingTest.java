package org.reactfx;

import static org.junit.Assert.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SuccessionReducingTest {
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
    public void fxTest() throws InterruptedException, ExecutionException {
        CompletableFuture<List<Object>> future1 = new CompletableFuture<>();
        CompletableFuture<List<Object>> future2 = new CompletableFuture<>();

        Platform.runLater(() -> {
            EventSource<Integer> source = new EventSource<>();

            AwaitingEventStream<Integer> reducing1 = source.reduceSuccessions((a,  b) -> a + b, Duration.ofMillis(200));
            EventStream<Boolean> pending1 = EventStreams.valuesOf(reducing1.pendingProperty());
            List<Object> emitted1 = new ArrayList<>();
            EventStreams.merge(reducing1, pending1).subscribe(i -> emitted1.add(i));

            AwaitingEventStream<Integer> reducing2 = source.reduceSuccessions(() -> 0, (a,  b) -> a + b, Duration.ofMillis(200));
            EventStream<Boolean> pending2 = EventStreams.valuesOf(reducing2.pendingProperty());
            List<Object> emitted2 = new ArrayList<>();
            EventStreams.merge(reducing2, pending2).subscribe(i -> emitted2.add(i));

            source.push(1);
            source.push(2);
            scheduler.schedule(() -> Platform.runLater(() -> source.push(3)), 50, TimeUnit.MILLISECONDS);

            scheduler.schedule(() -> Platform.runLater(() -> source.push(4)), 300, TimeUnit.MILLISECONDS);
            scheduler.schedule(() -> Platform.runLater(() -> source.push(5)), 350, TimeUnit.MILLISECONDS);

            scheduler.schedule(() -> Platform.runLater(() -> future1.complete(emitted1)), 600, TimeUnit.MILLISECONDS);
            scheduler.schedule(() -> Platform.runLater(() -> future2.complete(emitted2)), 600, TimeUnit.MILLISECONDS);
        });

        assertEquals(Arrays.asList(false, true, 6, false, true, 9, false), future1.get());
        assertEquals(Arrays.asList(false, true, 6, false, true, 9, false), future2.get());
    }

    @Test
    public void executorTest() throws InterruptedException, ExecutionException {
        CompletableFuture<List<Object>> future1 = new CompletableFuture<>();
        CompletableFuture<List<Object>> future2 = new CompletableFuture<>();

        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            EventSource<Integer> source = new EventSource<>();

            AwaitingEventStream<Integer> reducing1 = source.reduceSuccessions((a,  b) -> a + b, Duration.ofMillis(200), scheduler, executor);
            EventStream<Boolean> pending1 = EventStreams.valuesOf(reducing1.pendingProperty());
            List<Object> emitted1 = new ArrayList<>();
            EventStreams.merge(reducing1, pending1).subscribe(i -> emitted1.add(i));

            AwaitingEventStream<Integer> reducing2 = source.reduceSuccessions(() -> 0, (a,  b) -> a + b, Duration.ofMillis(200), scheduler, executor);
            EventStream<Boolean> pending2 = EventStreams.valuesOf(reducing2.pendingProperty());
            List<Object> emitted2 = new ArrayList<>();
            EventStreams.merge(reducing2, pending2).subscribe(i -> emitted2.add(i));

            source.push(1);
            source.push(2);
            scheduler.schedule(() -> executor.execute(() -> source.push(3)), 50, TimeUnit.MILLISECONDS);

            scheduler.schedule(() -> executor.execute(() -> source.push(4)), 300, TimeUnit.MILLISECONDS);
            scheduler.schedule(() -> executor.execute(() -> source.push(5)), 350, TimeUnit.MILLISECONDS);

            scheduler.schedule(() -> executor.execute(() -> future1.complete(emitted1)), 600, TimeUnit.MILLISECONDS);
            scheduler.schedule(() -> executor.execute(() -> future2.complete(emitted2)), 600, TimeUnit.MILLISECONDS);
        });

        assertEquals(Arrays.asList(false, true, 6, false, true, 9, false), future1.get());
        assertEquals(Arrays.asList(false, true, 6, false, true, 9, false), future2.get());

        executor.shutdown();
    }
}
