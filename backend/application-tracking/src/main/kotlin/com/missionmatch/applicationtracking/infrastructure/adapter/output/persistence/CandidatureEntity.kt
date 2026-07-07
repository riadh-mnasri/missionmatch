package com.missionmatch.applicationtracking.infrastructure.adapter.output.persistence

import com.missionmatch.applicationtracking.domain.Candidature
import com.missionmatch.applicationtracking.domain.CandidatureId
import com.missionmatch.applicationtracking.domain.CandidatureStatus
import com.missionmatch.applicationtracking.domain.FreelancerId
import com.missionmatch.applicationtracking.domain.MissionId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "candidatures",
    uniqueConstraints = [UniqueConstraint(columnNames = ["mission_id", "freelancer_id"])],
)
class CandidatureEntity(
    @Id
    val id: UUID,

    @Column(nullable = false)
    val missionId: UUID,

    @Column(nullable = false)
    val freelancerId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: CandidatureStatus,
) {
    fun toDomain(): Candidature = Candidature.reconstitute(
        id = CandidatureId(id),
        missionId = MissionId(missionId),
        freelancerId = FreelancerId(freelancerId),
        status = status,
    )

    companion object {
        fun fromDomain(candidature: Candidature): CandidatureEntity = CandidatureEntity(
            id = candidature.id.value,
            missionId = candidature.missionId.value,
            freelancerId = candidature.freelancerId.value,
            status = candidature.status,
        )
    }
}
