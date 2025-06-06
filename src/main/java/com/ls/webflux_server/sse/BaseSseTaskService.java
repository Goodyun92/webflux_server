package com.ls.webflux_server.sse;

import com.ls.webflux_server.queue.QueueManager;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseSseTaskService {
    protected final QueueManager queueManager;
    protected final Map<String, FluxSink<String>> emitters = new ConcurrentHashMap<>();

    public BaseSseTaskService(QueueManager queueManager) {
        this.queueManager = queueManager;
    }

    // SSE 구독: 대기자수 전송, 해제 등 공통
    public Flux<String> taskFlux(String taskId) {
        return Flux.create(emitter -> {
            emitters.put(taskId, emitter);

            Disposable intervalTask = Flux.interval(Duration.ZERO, Duration.ofSeconds(2))
                    .subscribe(tick -> Optional.ofNullable(emitters.get(taskId))
                            .ifPresent(e -> e.next("{\"status\":\"WAITING\",\"queue\":" + queueManager.getMyWaitingCount(taskId) + "}")));

            emitter.onDispose(() -> {
                intervalTask.dispose();
                emitters.remove(taskId);
            });
        });
    }

    // 공통 완료/실패 처리
    public void notifyCompletion(String taskId, String doneJson) {
        Optional.ofNullable(emitters.get(taskId)).ifPresent(e -> {
            e.next(doneJson);
            e.complete();
            emitters.remove(taskId);
        });
    }
    public void notifyFailure(String taskId, String failMessage) {
        Optional.ofNullable(emitters.get(taskId)).ifPresent(e -> {
            e.next("{\"status\":\"FAIL\",\"message\":\"" + failMessage + "\"}");
            e.complete();
            emitters.remove(taskId);
        });
    }
}
