package com.ls.webflux_server.sse;

import com.ls.webflux_server.queue.QueueManager;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class BaseSseTaskService {
    protected final Map<String, FluxSink<String>> emitters = new ConcurrentHashMap<>();
    protected final Queue<String> waitingQueue = new ConcurrentLinkedQueue<>();

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

    // 내 앞에 몇 명이 대기 중인지 계산
    public int getMyWaitingCount(String taskId) {
        int count = 0;
        for (String id : waitingQueue) {
            if (id.equals(taskId)) break;
            count++;
        }
        return count;
    }
}
