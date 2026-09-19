package com.FraudGaurd.fraudguard_backend.service;

import com.FraudGaurd.fraudguard_backend.model.ProtectionMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FraudProtectionServiceTest {

    private final FraudProtectionService fraudProtectionService =
            new FraudProtectionService();


    @Test
    void normalModeShouldAlwaysApprove() {

        assertEquals(
                FraudAction.APPROVE,
                fraudProtectionService.decideAction(
                        0,
                        ProtectionMode.NORMAL
                )
        );

        assertEquals(
                FraudAction.APPROVE,
                fraudProtectionService.decideAction(
                        50,
                        ProtectionMode.NORMAL
                )
        );

        assertEquals(
                FraudAction.APPROVE,
                fraudProtectionService.decideAction(
                        100,
                        ProtectionMode.NORMAL
                )
        );
    }


    @Test
    void cautionModeShouldApproveLowRisk() {

        assertEquals(
                FraudAction.APPROVE,
                fraudProtectionService.decideAction(
                        29,
                        ProtectionMode.CAUTION
                )
        );
    }


    @Test
    void cautionModeShouldFlagSuspiciousRisk() {

        assertEquals(
                FraudAction.FLAG,
                fraudProtectionService.decideAction(
                        30,
                        ProtectionMode.CAUTION
                )
        );

        assertEquals(
                FraudAction.FLAG,
                fraudProtectionService.decideAction(
                        59,
                        ProtectionMode.CAUTION
                )
        );
    }


    @Test
    void protectedModeShouldApproveLowRisk() {

        assertEquals(
                FraudAction.APPROVE,
                fraudProtectionService.decideAction(
                        29,
                        ProtectionMode.PROTECTED
                )
        );
    }


    @Test
    void protectedModeShouldFlagSuspiciousRisk() {

        assertEquals(
                FraudAction.FLAG,
                fraudProtectionService.decideAction(
                        30,
                        ProtectionMode.PROTECTED
                )
        );

        assertEquals(
                FraudAction.FLAG,
                fraudProtectionService.decideAction(
                        59,
                        ProtectionMode.PROTECTED
                )
        );
    }


    @Test
    void protectedModeShouldBlockHighRisk() {

        assertEquals(
                FraudAction.BLOCK,
                fraudProtectionService.decideAction(
                        60,
                        ProtectionMode.PROTECTED
                )
        );

        assertEquals(
                FraudAction.BLOCK,
                fraudProtectionService.decideAction(
                        80,
                        ProtectionMode.PROTECTED
                )
        );

        assertEquals(
                FraudAction.BLOCK,
                fraudProtectionService.decideAction(
                        100,
                        ProtectionMode.PROTECTED
                )
        );
    }
}