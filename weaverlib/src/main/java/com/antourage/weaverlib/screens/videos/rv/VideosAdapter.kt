package com.antourage.weaverlib.screens.videos.rv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.models.StreamResponse
import com.squareup.picasso.Picasso

class VideosAdapter(val onClick:(stream:StreamResponse)->Unit) : RecyclerView.Adapter<VideosAdapter.VideoViewHolder>() {
    var listOfStreams: MutableList<StreamResponse?> = mutableListOf()

    fun setStreamList(list: List<StreamResponse>) {
        val tempList: MutableList<StreamResponse?> = list.toMutableList()
        //tempList.add(null)
        val diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(
            StreamListDiffCallback(listOfStreams, tempList)
        )
        diffResult.dispatchUpdatesTo(this)
        this.listOfStreams = tempList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        return VideoViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false))
    }

    override fun getItemCount(): Int {
        return listOfStreams.size
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        Picasso.get().load(listOfStreams[position]?.thumbnailUrl).into(holder.thumbnail)
        holder.txtTitle.text = listOfStreams[position]?.streamTitle
        holder.itemView.setOnClickListener {
            listOfStreams[holder.adapterPosition]?.let{onClick.invoke(it)} }
    }

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnail: ImageView = itemView.findViewById(R.id.thumbnail)
        val txtTitle: TextView = itemView.findViewById(R.id.txtTitle)
    }
}