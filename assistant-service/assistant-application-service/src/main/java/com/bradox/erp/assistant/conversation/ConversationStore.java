package com.bradox.erp.assistant.conversation;

import com.bradox.erp.assistant.provider.AIGenerationRequest;
import com.bradox.erp.assistant.config.AiProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory conversation history for MVP (bounded per conversation).
 */
@Component
public class ConversationStore {

    private final AiProperties properties;
    private final ConcurrentHashMap<UUID, Conversation> conversations = new ConcurrentHashMap<>();

    public ConversationStore(AiProperties properties) {
        this.properties = properties;
    }

    public Conversation getOrCreate(UUID conversationId, UUID userId, UUID companyId) {
        UUID id = conversationId != null ? conversationId : UUID.randomUUID();
        return conversations.compute(id, (key, existing) -> {
            if (existing == null) {
                return new Conversation(id, userId, companyId);
            }
            if (userId != null && existing.userId != null && !existing.userId.equals(userId)) {
                throw new IllegalArgumentException("Conversation does not belong to the current user");
            }
            if (companyId != null && existing.companyId != null && !existing.companyId.equals(companyId)) {
                throw new IllegalArgumentException("Conversation does not belong to the current company");
            }
            existing.touch();
            return existing;
        });
    }

    public Optional<Conversation> find(UUID conversationId) {
        return Optional.ofNullable(conversations.get(conversationId));
    }

    public void delete(UUID conversationId, UUID userId) {
        Conversation existing = conversations.get(conversationId);
        if (existing == null) {
            return;
        }
        if (userId != null && existing.userId != null && !existing.userId.equals(userId)) {
            throw new IllegalArgumentException("Conversation does not belong to the current user");
        }
        conversations.remove(conversationId);
    }

    public final class Conversation {
        private final UUID id;
        private final UUID userId;
        private final UUID companyId;
        private final List<AIGenerationRequest.Message> messages = new ArrayList<>();
        private Instant updatedAt = Instant.now();

        Conversation(UUID id, UUID userId, UUID companyId) {
            this.id = id;
            this.userId = userId;
            this.companyId = companyId;
        }

        public UUID getId() {
            return id;
        }

        public UUID getUserId() {
            return userId;
        }

        public UUID getCompanyId() {
            return companyId;
        }

        public synchronized List<AIGenerationRequest.Message> snapshotMessages() {
            return List.copyOf(messages);
        }

        public synchronized void append(AIGenerationRequest.Message message) {
            messages.add(message);
            int max = Math.max(2, properties.getMaxHistoryMessages());
            while (messages.size() > max) {
                // keep system prompt if first, otherwise drop oldest
                if (!messages.isEmpty() && "system".equals(messages.get(0).role()) && messages.size() > 1) {
                    messages.remove(1);
                } else {
                    messages.remove(0);
                }
            }
            touch();
        }

        public synchronized void replaceAll(List<AIGenerationRequest.Message> next) {
            messages.clear();
            messages.addAll(next);
            touch();
        }

        void touch() {
            updatedAt = Instant.now();
        }

        public Instant getUpdatedAt() {
            return updatedAt;
        }
    }
}
