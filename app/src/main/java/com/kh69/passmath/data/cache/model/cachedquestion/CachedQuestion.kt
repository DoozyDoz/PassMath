package com.kh69.passmath.data.cache.model.cachedquestion


data class CachedQuestion(
    val questionId: String,
    val text: String,
    val type: String,
    val adoptionStatus: String,
    val publishedAt: String
) {

    companion object {
        fun fromDomain(animal: Animal): CachedAnimal {
            return CachedAnimal(
                animal.id,
                animal.name,
                animal.type,
                animal.adoptionStatus.toString(),
                animal.publishedAt.toString()
            )
        }
    }

    fun toDomain(photos: List<CachedPhoto>, videos: List<CachedVideo>, tags: List<CachedTag>): Animal {
        return Animal(
            animalId,
            name,
            type,
            Media(
                photos = photos.map { it.toDomain() },
                videos = videos.map { it.toDomain() }
            ),
            tags.map { it.tag },
            AdoptionStatus.valueOf(adoptionStatus),
            DateTimeUtils.parse(publishedAt)
        )
    }
}