package com.ls.webflux_server.queue;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class QueueManager {
    private final Queue<String> waitingQueue = new ConcurrentLinkedQueue<>();

    // 작업을 큐에 추가
    public void addTask(String taskId) {
        waitingQueue.add(taskId);
    }

    // 작업이 끝나면 큐에서 제거
    public void removeTask(String taskId) {
        waitingQueue.remove(taskId);
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
