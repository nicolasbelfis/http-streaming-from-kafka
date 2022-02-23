package twitter.tweet

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule


data class SimpleTweet(
    val id: String,
    val text: String,
    val hashTags: List<String>,
    val retweetCount: Int,
    val lang: String,
) {
    fun toJson(): String = ObjectMapper().writeValueAsString(this)

}

object ObjectMapperKotlin : ObjectMapper(ObjectMapper().apply {
    registerModule(KotlinModule())
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
})
