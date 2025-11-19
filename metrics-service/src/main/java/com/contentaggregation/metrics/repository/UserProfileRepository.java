package com.contentaggregation.metrics.repository;

import com.contentaggregation.metrics.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByUserId(UUID userId);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update UserProfile p
           set p.clickbaitTolerance = :clickbaitTolerance,
               p.explorationVsExploitation = :explorationVsExploitation,
               p.lastUpdated = CURRENT_TIMESTAMP
         where p.userId = :userId
        """)
    int updateBehavioralSignals(
        @Param("userId") UUID userId,
        @Param("clickbaitTolerance") Float clickbaitTolerance,
        @Param("explorationVsExploitation") Float explorationVsExploitation
    );
}

