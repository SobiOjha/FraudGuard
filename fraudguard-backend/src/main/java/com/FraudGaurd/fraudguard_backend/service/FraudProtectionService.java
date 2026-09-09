package com.FraudGaurd.fraudguard_backend.service;

import com.FraudGaurd.fraudguard_backend.model.ProtectionMode;
import org.springframework.stereotype.Service;

@Service
public class FraudProtectionService {

    public FraudAction decideAction(
            int riskScore,
            ProtectionMode mode) {

        if (mode == ProtectionMode.NORMAL) {
            return FraudAction.APPROVE;
        }

        if (mode == ProtectionMode.CAUTION) {

            if (riskScore >= 30) {
                return FraudAction.FLAG;
            }

            return FraudAction.APPROVE;
        }

        if (mode == ProtectionMode.PROTECTED) {

            if (riskScore >= 60) {
                return FraudAction.BLOCK;
            }

            if (riskScore >= 30) {
                return FraudAction.FLAG;
            }

            return FraudAction.APPROVE;
        }

        return FraudAction.APPROVE;
    }
}