package worker

import io.github.redouane59.twitter.dto.stream.StreamRules
import io.github.redouane59.twitter.dto.tweet.Attachments
import io.github.redouane59.twitter.dto.tweet.ContextAnnotation
import io.github.redouane59.twitter.dto.tweet.Geo
import io.github.redouane59.twitter.dto.tweet.ReplySettings
import io.github.redouane59.twitter.dto.tweet.Tweet
import io.github.redouane59.twitter.dto.tweet.TweetType
import io.github.redouane59.twitter.dto.tweet.entities.Entities
import io.github.redouane59.twitter.dto.tweet.entities.MediaEntity
import io.github.redouane59.twitter.dto.user.User
import java.time.LocalDateTime

data class FakeTweet(private val id: String) : Tweet {
    override fun getId(): String {
        return id
    }

    override fun getText(): String {
        TODO("Not yet implemented")
    }

    override fun getUser(): User {
        TODO("Not yet implemented")
    }

    override fun getAuthorId(): String {
        TODO("Not yet implemented")
    }

    override fun getRetweetCount(): Int {
        TODO("Not yet implemented")
    }

    override fun getLikeCount(): Int {
        TODO("Not yet implemented")
    }

    override fun getReplyCount(): Int {
        TODO("Not yet implemented")
    }

    override fun getQuoteCount(): Int {
        TODO("Not yet implemented")
    }

    override fun getCreatedAt(): LocalDateTime {
        TODO("Not yet implemented")
    }

    override fun getLang(): String {
        TODO("Not yet implemented")
    }

    override fun getInReplyToUserId(): String {
        TODO("Not yet implemented")
    }

    override fun getInReplyToStatusId(): String {
        TODO("Not yet implemented")
    }

    override fun getInReplyToStatusId(p0: TweetType?): String {
        TODO("Not yet implemented")
    }

    override fun getContextAnnotations(): MutableList<ContextAnnotation> {
        TODO("Not yet implemented")
    }

    override fun getTweetType(): TweetType {
        TODO("Not yet implemented")
    }

    override fun getConversationId(): String {
        TODO("Not yet implemented")
    }

    override fun getReplySettings(): ReplySettings {
        TODO("Not yet implemented")
    }

    override fun getGeo(): Geo {
        TODO("Not yet implemented")
    }

    override fun getAttachments(): Attachments {
        TODO("Not yet implemented")
    }

    override fun getSource(): String {
        TODO("Not yet implemented")
    }

    override fun getEntities(): Entities {
        TODO("Not yet implemented")
    }

    override fun getMedia(): MutableList<out MediaEntity> {
        TODO("Not yet implemented")
    }

    override fun getMatchingRules(): MutableList<StreamRules.StreamRule> {
        TODO("Not yet implemented")
    }

}
