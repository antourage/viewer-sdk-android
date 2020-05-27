package com.antourage.weaverlib.other.models

import androidx.room.*
import androidx.room.ForeignKey.CASCADE

@Entity(tableName = "videos")
internal data class Video(
    @ColumnInfo(name = "id") @PrimaryKey var id: Int,
    @ColumnInfo(name = "stopTimeMillis") var stopTimeMillis: Long,
    @ColumnInfo(name = "startDate") var expirationDate: Long,
    @ColumnInfo(name = "nickname") var nickname: String? = null,
    @ColumnInfo(name = "text") var text: String? = null
)

@Entity(tableName = "comments",
    foreignKeys = [ForeignKey(
        entity = Video::class,
        parentColumns = ["id"],
        childColumns = ["vodId"],
        onDelete = CASCADE)],
    indices = [Index(value = ["vodId"], name = "index_video_id")])
internal data class Comment(
    @ColumnInfo(name = "id") @PrimaryKey var id: String,
    @ColumnInfo(name = "vodId") var vodId: Int,
    @ColumnInfo(name = "avatarUrl") var avatarUrl: String? = null,
    @ColumnInfo(name = "email") var email: String? = null,
    @ColumnInfo(name = "nickname") var nickname: String? = null,
    @ColumnInfo(name = "text") var text: String? = null,
    @ColumnInfo(name = "type") var type: Int? = null,
    @ColumnInfo(name = "userID") var userID: String? = null,
    @ColumnInfo(name = "pushTimeMills") var pushTimeMills: Long? = null
)