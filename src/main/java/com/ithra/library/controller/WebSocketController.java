package com.ithra.library.controller;

import com.ithra.library.dto.FrameAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Subscribe to live stream updates
     */
    @MessageMapping("/stream/{streamKey}/subscribe")
    @SendTo("/topic/stream/{streamKey}")
    public void subscribeToStream(@DestinationVariable String streamKey) {
        log.info("Client subscribed to stream: {}", streamKey);
    }

    /**
     * Send real-time detection updates
     */
    public void sendDetectionUpdate(String streamKey, FrameAnalysisResult result) {
        messagingTemplate.convertAndSend(
                "/topic/stream/" + streamKey + "/detections",
                result
        );
    }

    /**
     * Send stream status updates
     */
    public void sendStreamStatus(String streamKey, String status) {
        messagingTemplate.convertAndSend(
                "/topic/stream/" + streamKey + "/status",
                status
        );
    }
}
