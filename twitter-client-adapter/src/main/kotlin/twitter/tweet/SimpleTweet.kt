package twitter.tweet

import com.fasterxml.jackson.databind.ObjectMapper

data class SimpleTweet(val id: String, val text: String) {
    fun toJson(): String = ObjectMapper().createObjectNode()
        .put("id", id)
        .put("text", text)
        .toString()
}
