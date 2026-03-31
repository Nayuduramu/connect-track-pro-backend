package com.connecttrack.pro.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // This registers the '/ws' endpoint for the WebSocket handshake.
        // Mobile clients like Flutter can connect directly to this.
        // We remove .withSockJS() as it's not needed for native mobile apps
        // and can sometimes cause handshake issues like the 400 Bad Request error.
        registry.addEndpoint("/ws");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Defines the prefix for messages that are bound for @MessageMapping methods
        // in your controllers. The client will send messages to destinations like "/app/chat.sendMessage".
        registry.setApplicationDestinationPrefixes("/app");

        // Defines the prefix for destinations that the message broker will handle.
        // The broker will broadcast messages to all subscribed clients on topics
        // like "/topic/public".
        registry.enableSimpleBroker("/topic");
    }
}