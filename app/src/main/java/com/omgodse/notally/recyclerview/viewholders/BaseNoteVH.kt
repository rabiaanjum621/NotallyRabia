package com.omgodse.notally.recyclerview.viewholders

import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.R
import com.omgodse.notally.TextSizeEngine
import com.omgodse.notally.databinding.RecyclerBaseNoteBinding
import com.omgodse.notally.miscellaneous.Operations
import com.omgodse.notally.miscellaneous.applySpans
import com.omgodse.notally.preferences.DateFormat
import com.omgodse.notally.recyclerview.ItemListener
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.Color
import com.omgodse.notally.room.Type
import org.ocpsoft.prettytime.PrettyTime
import java.text.SimpleDateFormat
import java.util.*

class BaseNoteVH(
    private val binding: RecyclerBaseNoteBinding,
    private val dateFormat: String,
    private val textSize: String,
    private val maxItems: Int,
    maxLines: Int,
    listener: ItemListener,
    private val prettyTime: PrettyTime,
    private val formatter: SimpleDateFormat,
) : RecyclerView.ViewHolder(binding.root) {

    init {
        val title = TextSizeEngine.getDisplayTitleSize(textSize)
        val body = TextSizeEngine.getDisplayBodySize(textSize)

        binding.Title.setTextSize(TypedValue.COMPLEX_UNIT_SP, title)
        binding.Date.setTextSize(TypedValue.COMPLEX_UNIT_SP, body)
        binding.Note.setTextSize(TypedValue.COMPLEX_UNIT_SP, body)

        binding.LinearLayout.children.forEach { view ->
            view as TextView
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, body)
        }

        binding.Note.maxLines = maxLines

        binding.CardView.setOnClickListener {
            listener.onClick(adapterPosition)
        }

        binding.CardView.setOnLongClickListener {
            listener.onLongClick(adapterPosition)
            return@setOnLongClickListener true
        }
    }

    fun bind(baseNote: BaseNote) {
        when (baseNote.type) {
            Type.NOTE -> bindNote(baseNote)
            Type.LIST -> bindList(baseNote)
            Type.PHONE -> bindNote(baseNote)
        }

        setDate(baseNote)
        setColor(baseNote)

        binding.Title.text = baseNote.title
        binding.Title.isVisible = baseNote.title.isNotEmpty()

        Operations.bindLabels(binding.LabelGroup, baseNote.labels, textSize)

        if (isEmpty(baseNote)) {
            binding.Title.setText(getEmptyMessage(baseNote))
            binding.Title.visibility = View.VISIBLE
        }
    }

    private fun bindNote(note: BaseNote) {
        binding.LinearLayout.visibility = View.GONE

        binding.Note.text = note.body.applySpans(note.spans)
        binding.Note.isVisible = note.body.isNotEmpty()
    }

    private fun bindList(list: BaseNote) {
        binding.Note.visibility = View.GONE

        if (list.items.isEmpty()) {
            binding.LinearLayout.visibility = View.GONE
        } else {
            binding.LinearLayout.visibility = View.VISIBLE

            val filteredList = list.items.take(maxItems)
            binding.LinearLayout.children.forEachIndexed { index, view ->
                if (view.id != R.id.ItemsRemaining) {
                    if (index < filteredList.size) {
                        val item = filteredList[index]
                        view as TextView
                        view.text = item.body
                        handleChecked(view, item.checked)
                        view.visibility = View.VISIBLE
                    } else view.visibility = View.GONE
                }
            }

            if (list.items.size > maxItems) {
                binding.ItemsRemaining.visibility = View.VISIBLE
                val itemsRemaining = list.items.size - maxItems
                binding.ItemsRemaining.text = itemsRemaining.toString()
            } else binding.ItemsRemaining.visibility = View.GONE
        }
    }


    private fun setDate(baseNote: BaseNote) {
        val date = Date(baseNote.timestamp)
        if (dateFormat != DateFormat.none) {
            binding.Date.visibility = View.VISIBLE
            when (dateFormat) {
                DateFormat.relative -> binding.Date.text = prettyTime.format(date)
                DateFormat.absolute -> binding.Date.text = formatter.format(date)
            }
        } else binding.Date.visibility = View.GONE
    }

    private fun setColor(baseNote: BaseNote) {
        val context = binding.root.context
        val color = Operations.extractColor(baseNote.color, context)

        if (baseNote.color == Color.DEFAULT) {
            binding.CardView.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.unit)
            binding.CardView.setCardBackgroundColor(0)
        } else {
            binding.CardView.strokeWidth = 0
            binding.CardView.setCardBackgroundColor(color)
        }
    }


    private fun isEmpty(baseNote: BaseNote): Boolean {
        return when (baseNote.type) {
            Type.NOTE -> baseNote.title.isBlank() && baseNote.body.isBlank()
            Type.LIST -> baseNote.title.isBlank() && baseNote.items.isEmpty()
            Type.PHONE -> baseNote.title.isBlank() && baseNote.body.isBlank()
        }
    }

    private fun getEmptyMessage(baseNote: BaseNote): Int {
        return when (baseNote.type) {
            Type.NOTE -> R.string.empty_note
            Type.LIST -> R.string.empty_list
            Type.PHONE -> R.string.empty_number
        }
    }

    private fun handleChecked(textView: TextView, checked: Boolean) {
        if (checked) {
            textView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.checkbox_16, 0, 0, 0)
        } else textView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.checkbox_outline_16, 0, 0, 0)
    }
}