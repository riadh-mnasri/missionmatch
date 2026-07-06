package com.missionmatch.shared.domain

data class SkillSet(val skills: Set<String>) {

    init {
        require(skills.isNotEmpty()) { "A skill set must contain at least one skill" }
    }

    fun overlapWith(other: SkillSet): Set<String> = skills.intersect(other.skills)

    fun overlapRatioWith(other: SkillSet): Double =
        overlapWith(other).size.toDouble() / skills.size

    companion object {
        fun of(vararg skills: String): SkillSet = SkillSet(normalize(skills.toSet()))

        fun of(skills: Collection<String>): SkillSet = SkillSet(normalize(skills.toSet()))

        private fun normalize(skills: Set<String>): Set<String> =
            skills.map { it.trim().lowercase() }.toSet()
    }
}
