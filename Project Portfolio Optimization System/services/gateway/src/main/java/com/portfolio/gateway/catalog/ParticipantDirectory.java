package com.portfolio.gateway.catalog;

import java.util.Optional;
import java.util.UUID;

public interface ParticipantDirectory {

    Optional<ParticipantCard> findById(UUID id);
}
