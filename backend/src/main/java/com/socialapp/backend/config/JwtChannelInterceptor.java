package com.socialapp.backend.config;

import com.socialapp.backend.service.JwtService;
import com.socialapp.backend.service.PresenceService;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.util.Map;


@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final PresenceService presenceService;

    public JwtChannelInterceptor(JwtService jwtService,
                                 UserDetailsService userDetailsService,
                                 PresenceService presenceService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.presenceService = presenceService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String userId = jwtService.extractUserId(token);

                if (userId != null && jwtService.isTokenValid(token)) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(userId);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    accessor.setUser(authentication);

                    // Salvăm autentificarea în atributele sesiunii pentru frame-urile SEND ulterioare
                    Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                    if (sessionAttributes != null) {
                        sessionAttributes.put("USER_AUTH", authentication);
                    }

                    // Marcăm utilizatorul ca ONLINE
                    presenceService.userConnected(accessor.getSessionId(), userId);
                }
            }
        } else if (accessor.getUser() == null && accessor.getSessionAttributes() != null) {
            // Ne asigurăm că Principal-ul este prezent și pe mesajele SEND (ex: /app/chat.typing)
            Object savedAuth = accessor.getSessionAttributes().get("USER_AUTH");
            if (savedAuth instanceof UsernamePasswordAuthenticationToken auth) {
                accessor.setUser(auth);
            }
        }

        return message;
    }
}
