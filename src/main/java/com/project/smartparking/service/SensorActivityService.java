package com.project.smartparking.service;

import com.project.smartparking.dto.SensorActivityEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SensorActivityService {

    private static final int MAX_HISTORY = 200;

    private final Deque<SensorActivityEvent> history = new ArrayDeque<>();
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public synchronized void log(String level, String message) {
        SensorActivityEvent event = new SensorActivityEvent(LocalDateTime.now(), level, message);

        history.addLast(event);
        while (history.size() > MAX_HISTORY) {
            history.pollFirst();
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("activity").data(event));
            } catch (Exception e) {
                emitter.complete();
                emitters.remove(emitter);
            }
        }
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);

        Runnable cleanup = () -> emitters.remove(emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        try {
            emitter.send(SseEmitter.event().name("history").data(getHistory()));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    public synchronized List<SensorActivityEvent> getHistory() {
        return List.copyOf(history);
    }
}