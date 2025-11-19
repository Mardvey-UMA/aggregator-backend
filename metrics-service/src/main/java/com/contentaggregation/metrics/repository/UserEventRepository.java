package com.contentaggregation.metrics.repository;

import com.contentaggregation.metrics.entity.UserEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserEventRepository extends JpaRepository<UserEvent, UUID> {

    List<UserEvent> findByUserIdAndProcessedFalse(UUID userId);

    @Query("""
        select e
          from UserEvent e
         where e.userId = :userId
           and e.timestamp between :from and :to
         order by e.timestamp asc
        """)
    List<UserEvent> findEventsWithinRange(
        @Param("userId") UUID userId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );
}

