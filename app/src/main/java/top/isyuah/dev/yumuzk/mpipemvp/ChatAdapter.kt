package top.isyuah.dev.yumuzk.mpipemvp

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

data class ChatMessage(var text: String, val isUser: Boolean)

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {
    private val messages = mutableListOf<ChatMessage>()

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun updateLastMessage(newText: String) {
        if (messages.isNotEmpty()) {
            messages.last().text = newText
            notifyItemChanged(messages.size - 1)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount() = messages.size

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val cardMessage: MaterialCardView = view.findViewById(R.id.card_message)
        private val tvMessage: TextView = view.findViewById(R.id.tv_message_text)
        private val container: LinearLayout = view as LinearLayout

        fun bind(message: ChatMessage) {
            tvMessage.text = message.text
            val params = cardMessage.layoutParams as LinearLayout.LayoutParams
            if (message.isUser) {
                container.gravity = Gravity.END
                cardMessage.setCardBackgroundColor(0xFFE3F2FD.toInt())
                params.setMargins(64, 8, 8, 8)
            } else {
                container.gravity = Gravity.START
                cardMessage.setCardBackgroundColor(0xFFF5F5F5.toInt())
                params.setMargins(8, 8, 64, 8)
            }
            cardMessage.layoutParams = params
        }
    }
}
