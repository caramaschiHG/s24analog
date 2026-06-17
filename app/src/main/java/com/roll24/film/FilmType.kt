package com.roll24.film

/**
 * Broad film-process categories used to drive pipeline decisions
 * (e.g. orange-mask removal is only relevant for color negative stocks).
 */
enum class FilmType {
    C41,
    E6,
    BLACK_AND_WHITE,
    VISION3
}
