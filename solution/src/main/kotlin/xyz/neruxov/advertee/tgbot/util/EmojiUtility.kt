package xyz.neruxov.advertee.tgbot.util

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
object EmojiUtility {

    fun getCountEmoji(number: Int): String {
        return when (number) {
            0 -> "0️⃣"
            1 -> "1️⃣"
            2 -> "2️⃣"
            3 -> "3️⃣"
            4 -> "4️⃣"
            5 -> "5️⃣"
            6 -> "6️⃣"
            7 -> "7️⃣"
            8 -> "8️⃣"
            9 -> "9️⃣"
            10 -> "🔟"
            else -> {
                val numberString = number.toString()
                val emojis = numberString.map { getCountEmoji(it.toString().toInt()) }
                emojis.joinToString(separator = "")
            }
        }
    }

}