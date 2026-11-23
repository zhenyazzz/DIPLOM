package com.portfolio.gateway.catalog.port;

import com.portfolio.gateway.catalog.model.ParticipantCard;
import java.util.Optional;
import java.util.UUID;

public interface ParticipantDirectory {

    Optional<ParticipantCard> findById(UUID id);
}
