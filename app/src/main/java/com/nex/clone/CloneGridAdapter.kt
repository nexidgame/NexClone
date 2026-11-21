package com.nex.clone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nex.clone.core.CloneItem
import com.nex.clone.databinding.ItemCloneBinding

/** Kisi ikon aplikasi klon: ketuk = buka, tahan = menu. */
class CloneGridAdapter(
    private val onClick: (CloneItem) -> Unit,
    private val onLong: (CloneItem, View) -> Unit,
) : RecyclerView.Adapter<CloneGridAdapter.VH>() {

    private var items: List<CloneItem> = emptyList()

    fun submit(list: List<CloneItem>) {
        items = list
        notifyDataSetChanged()
    }

    class VH(val vb: ItemCloneBinding) : RecyclerView.ViewHolder(vb.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemCloneBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.vb.imgIcon.setImageDrawable(item.icon)
        holder.vb.tvLabel.text = item.label
        holder.vb.imgLock.visibility = if (item.entry.locked) View.VISIBLE else View.GONE
        holder.vb.imgProfile.visibility = if (item.inProfile) View.VISIBLE else View.GONE
        holder.vb.root.alpha = if (item.missing) 0.45f else 1f
        holder.vb.root.setOnClickListener { onClick(item) }
        holder.vb.root.setOnLongClickListener {
            onLong(item, holder.vb.imgIcon)
            true
        }
    }
}
