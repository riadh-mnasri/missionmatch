package com.missionmatch.applicationtracking.domain

import com.fasterxml.jackson.annotation.JsonValue
import java.util.UUID

data class FreelancerId(@get:JsonValue val value: UUID)
