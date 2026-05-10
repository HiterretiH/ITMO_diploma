package com.logistic.backend.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistic.backend.order.Order;
import com.logistic.backend.user.User;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void record(User user, Order order, AuditEventType type, Map<String, Object> payload) {
        AuditEvent e = new AuditEvent();
        e.setUser(user);
        e.setOrder(order);
        e.setEventType(type);
        if (payload != null && !payload.isEmpty()) {
            try {
                e.setPayload(objectMapper.writeValueAsString(payload));
            } catch (JsonProcessingException ex) {
                e.setPayload(payload.toString());
            }
        }
        auditEventRepository.save(e);
    }

    @Transactional
    public void record(User user, AuditEventType type, Map<String, Object> payload) {
        record(user, null, type, payload);
    }
}
