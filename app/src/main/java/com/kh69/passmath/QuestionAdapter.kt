package com.kh69.passmath

import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kh69.passmath.data.Question
import com.kh69.passmath.ui.katex.KatexView

class QuestionAdapter(private val mList: List<Question>) : RecyclerView.Adapter<QuestionAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_question_card, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemsViewModel = mList[position]

        holder.kv_question.setDisplayText(itemsViewModel.katex_question)
        holder.kv_answer.setDisplayText(itemsViewModel.katex_answer)
        holder.btn_show.setOnClickListener {
            if (holder.kv_answer.visibility == GONE) holder.kv_answer.visibility =
                VISIBLE else holder.kv_answer.visibility =
                GONE
        }

    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return mList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val kv_question: KatexView = itemView.findViewById(R.id.kv_question)
        val kv_answer: KatexView = itemView.findViewById(R.id.kv_answer)
        val btn_show: View = itemView.findViewById(R.id.btn_show)

    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }
}

