package com.portfolio.gateway.catalog.stub;

import com.portfolio.gateway.catalog.model.ParticipantCard;
import com.portfolio.gateway.catalog.port.ParticipantDirectory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StubParticipantDirectory implements ParticipantDirectory {

    @Override
    public Optional<ParticipantCard> findById(UUID id) {
        if (StubDomainProjectCatalog.OWNER_ID.equals(id)) {
            return Optional.of(new ParticipantCard(id, "System Admin"));
        }
        return Optional.empty();
    }
}
