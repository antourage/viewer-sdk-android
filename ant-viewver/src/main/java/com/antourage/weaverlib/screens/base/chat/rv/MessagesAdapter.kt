package com.antourage.weaverlib.screens.base.chat.rv

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.millisToTime
import com.antourage.weaverlib.other.models.Message
import com.antourage.weaverlib.other.models.MessageType
import com.antourage.weaverlib.other.models.User
import com.antourage.weaverlib.other.parseToDisplayMessagesAgoTimeLong
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_videos_list.*
import org.jetbrains.anko.backgroundDrawable
import kotlin.collections.ArrayList

internal class MessagesAdapter(
    var list: MutableList<Message>, var orientation: Int = Configuration.ORIENTATION_PORTRAIT,
    private val isPlayerLive: Boolean
) :
    RecyclerView.Adapter<MessagesAdapter.MessageViewHolder>() {
    companion object {
        private const val PORTRAIT_RIGHT = 0
        private const val PORTRAIT_LEFT = 1
    }

    fun changeOrientation(newOrientation: Int) {
        orientation = newOrientation
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (
            list[position].userID != null && UserCache.getInstance()
                ?.getUserId() != null && list[position].userID == UserCache.getInstance()
                ?.getUserId().toString()
        ) {
            PORTRAIT_RIGHT
        } else
            PORTRAIT_LEFT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MessageViewHolder(
        LayoutInflater.from(parent.context).inflate(
            when (viewType) {
                PORTRAIT_RIGHT -> {
                    R.layout.chat_bubble_right_layout
                }
                else -> {
                    R.layout.chat_bubble_left_layout
                }
            }, parent, false
        )
    )

    override fun getItemCount() = list.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val messageItem = list[position]
        with(messageItem) {
            with(holder) {
                val picasso = if (!avatarUrl.isNullOrBlank()) Picasso.get()
                    .load(avatarUrl)
                else
                    Picasso.get()
                        .load(R.drawable.antourage_ic_incognito_user)
                picasso
                    .placeholder(R.drawable.antourage_ic_incognito_user)
                    .error(R.drawable.antourage_ic_incognito_user)
                    .into(userImageView)
                if (!avatarUrl.isNullOrBlank()) {
                    shadowView.backgroundDrawable =
                        ContextCompat.getDrawable(shadowView.context, R.drawable.antourage_blue_shadow)
                } else {
                    shadowView.backgroundDrawable =
                        ContextCompat.getDrawable(shadowView.context, R.drawable.antourage_dark_shadow)
                }
                txtMessage.text = text
                txtUser.text = nickname
                if (isPlayerLive) {
                    txtAt.visibility = View.GONE
                    txtTimeAdded.text = timestamp?.toDate()?.parseToDisplayMessagesAgoTimeLong(itemView.context)
                } else {
                    txtAt.visibility = View.VISIBLE
                    txtTimeAdded.text = when {
                        pushTimeMills != null -> pushTimeMills?.millisToTime()
                        else -> txtTimeAdded.context.getString(R.string.ant_init_time)
                    }
                }
            }
        }
    }

    fun setMessageList(newList: List<Message>, isFiltered: Boolean = false) {
        val userMessagesList = if (isFiltered) {
            newList
        } else {
            newList.filter { it.type == MessageType.USER }
        }
        val diffResult = DiffUtil.calculateDiff(MessageListDiffUtilCallback(list, userMessagesList))

        this.list.clear()
        //use this instead of addAll, as it will create copy without reference to prev. objects
        this.list = ArrayList(userMessagesList.map { it.copy().apply { id = it.id } })

        diffResult.dispatchUpdatesTo(this)
    }

    fun amountOfNewMessagesWillBeAdd(list: List<Message>): Int = list.size - this.list.size

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtUser: TextView = itemView.findViewById(R.id.txtUser)
        val txtMessage: TextView = itemView.findViewById(R.id.txtMessage)
        val userImageView: ImageView = itemView.findViewById(R.id.userImage)
        val txtTimeAdded: TextView = itemView.findViewById(R.id.txtTimeAt)
        val txtAt: TextView = itemView.findViewById(R.id.txtAt)
        val shadowView: View = itemView.findViewById(R.id.shadowView)
    }
}