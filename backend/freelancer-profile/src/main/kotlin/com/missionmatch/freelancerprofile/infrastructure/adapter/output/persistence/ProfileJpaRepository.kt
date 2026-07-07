package com.missionmatch.freelancerprofile.infrastructure.adapter.output.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProfileJpaRepository : JpaRepository<ProfileEntity, UUID>
