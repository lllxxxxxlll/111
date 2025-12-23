package top.isyuah.dev.yumuzk.mpipemvp

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView

class PhotoAdapter(
    private val items: MutableList<Uri> = mutableListOf(),
    private val onDelete: (Uri) -> Unit = {},
    private val onItemClick: (Uri) -> Unit = {}
) : RecyclerView.Adapter<PhotoAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.img_photo)
        val btnDelete: MaterialCardView = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val uri = items[position]
        Glide.with(holder.itemView.context)
            .load(uri)
            .centerCrop()
            .into(holder.img)

        holder.itemView.setOnClickListener {
            onItemClick(uri)
        }

        holder.btnDelete.setOnClickListener {
            onDelete(uri)
        }
    }

    override fun getItemCount(): Int = items.size

    fun replaceAll(list: List<Uri>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun addToTop(uri: Uri) {
        items.add(0, uri)
        notifyItemInserted(0)
    }

    fun remove(uri: Uri) {
        val idx = items.indexOf(uri)
        if (idx >= 0) {
            items.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }
}
