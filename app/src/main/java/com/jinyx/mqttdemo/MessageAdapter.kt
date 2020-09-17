package com.jinyx.mqttdemo

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

    private val mDataList = ArrayList<Message>()

    fun addMessage(message: String, isPublish: Boolean = false) {
        if (message.isNotEmpty() && message.isNotBlank()) {
            mDataList.add(Message(message, isPublish))
            notifyItemInserted(mDataList.size - 1)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = View.inflate(parent.context, R.layout.item_message, null)
        return ViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return mDataList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val textView = holder.findView<TextView>(R.id.txtMqttMsg)
        textView.text = mDataList[position].message
        if (mDataList[position].isPublish) {
            textView.setTextColor(Color.parseColor("#00AAAA"))
        } else {
            textView.setTextColor(Color.parseColor("#666666"))
        }
    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun <T : View> findView(@IdRes vId: Int): T {
            return itemView.findViewById(vId)
        }

    }

}