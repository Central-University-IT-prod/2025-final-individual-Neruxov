package xyz.neruxov.advertee.util.enum

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
enum class GenderFilter {
    MALE, FEMALE, ALL;

    fun matches(gender: Gender) = when (this) {
        MALE -> gender == Gender.MALE
        FEMALE -> gender == Gender.FEMALE
        ALL -> true
    }

}