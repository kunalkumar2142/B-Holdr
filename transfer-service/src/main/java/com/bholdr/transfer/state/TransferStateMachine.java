package com.bholdr.transfer.state;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
public class TransferStateMachine {

    private String from;
    private String to;

    private static final Set<Transition> ALLOWED_TRANSITIONS = new HashSet<>();

    static {
        ALLOWED_TRANSITIONS.add(new Transition("PENDING", "FUNDS_HELD"));
        ALLOWED_TRANSITIONS.add(new Transition("PENDING", "COMPENSATING"));
        ALLOWED_TRANSITIONS.add(new Transition("FUNDS_HELD", "RISK_APPROVED"));
        ALLOWED_TRANSITIONS.add(new Transition("FUNDS_HELD", "COMPENSATING"));
        ALLOWED_TRANSITIONS.add(new Transition("RISK_APPROVED", "CREDITED"));
        ALLOWED_TRANSITIONS.add(new Transition("CREDITED", "COMPLETED"));
        ALLOWED_TRANSITIONS.add(new Transition("RISK_APPROVED", "COMPENSATING"));
        ALLOWED_TRANSITIONS.add(new Transition("COMPENSATING", "FAILED"));
    }

    public static boolean canTransition(String from, String to) {
        return ALLOWED_TRANSITIONS.stream()
                .anyMatch(t -> t.from.equals(from) && t.to.equals(to));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Transition {
        private String from;
        private String to;
    }
}