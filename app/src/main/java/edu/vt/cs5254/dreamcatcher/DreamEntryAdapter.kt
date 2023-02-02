package edu.vt.cs5254.dreamcatcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import edu.vt.cs5254.dreamcatcher.databinding.ListItemDreamEntryBinding
import java.util.*

private const val TAG = "DreamListAdapter"

class DreamEntryHolder(
    private val binding: ListItemDreamEntryBinding
) : RecyclerView.ViewHolder(binding.root) {

    lateinit var boundEntry: DreamEntry
        private set

    fun bind(entry: DreamEntry) {

        boundEntry = entry
        binding.entryButton.displayEntry(entry)
    }

    private fun Button.displayEntry(entry: DreamEntry) {
        visibility = View.VISIBLE
        when(entry.kind) {
            DreamEntryKind.CONCEIVED -> {
                setBackgroundWithContrastingText("#B7513C")
                text = entry.kind.toString()
            }
            DreamEntryKind.DEFERRED -> {
                setBackgroundWithContrastingText("#E4A307")
                text = entry.kind.toString()
            }
            DreamEntryKind.FULFILLED -> {
                setBackgroundWithContrastingText("#38CE6C")
                text = entry.kind.toString()
            }
            DreamEntryKind.REFLECTION -> {
                setBackgroundWithContrastingText("#8D54D8")
                isAllCaps = false
                text = entry.text
            }
        }
    }
}

class DreamEntryAdapter(
    private val entries: List<DreamEntry>
) : RecyclerView.Adapter<DreamEntryHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DreamEntryHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemDreamEntryBinding.inflate(inflater, parent, false)
        return DreamEntryHolder(binding)
    }

    override fun onBindViewHolder(holder: DreamEntryHolder, position: Int) {
        val entry = entries[position]
        holder.bind(entry)
    }

    override fun getItemCount() = entries.size
}