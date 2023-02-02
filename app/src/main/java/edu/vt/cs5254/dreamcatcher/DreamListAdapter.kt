package edu.vt.cs5254.dreamcatcher

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.vt.cs5254.dreamcatcher.databinding.ListItemDreamBinding
import java.util.*

private const val TAG = "DreamListAdapter"

class DreamHolder(
    private val binding: ListItemDreamBinding
) : RecyclerView.ViewHolder(binding.root) {

    lateinit var boundDream: Dream
        private set

    fun bind(dream: Dream, onDreamClicked: (dreamId: UUID) -> Unit) {
        boundDream = dream
        binding.listItemTitle.text = dream.title
        val count = dream.entries.filter {
            it.kind == DreamEntryKind.REFLECTION
        }.size

        binding.listItemReflectionCount.text = binding.root.context.getString(R.string.list_item_reflection_count, count)
        if (dream.isDeferred) {
            binding.listItemImage.setVisibility(View.VISIBLE)
            binding.listItemImage.setImageResource(R.drawable.dream_deferred_icon)
        }
        else if (dream.isFulfilled) {
            binding.listItemImage.setVisibility(View.VISIBLE)
            binding.listItemImage.setImageResource(R.drawable.dream_fulfilled_icon)
        }
        else {
            binding.listItemImage.setVisibility(View.GONE)
        }

        binding.root.setOnClickListener {
            onDreamClicked(dream.id)
        }



    }
}

class DreamListAdapter(
    private val dreams: List<Dream>,
    private val onDreamClicked: (dreamId: UUID) -> Unit
) : RecyclerView.Adapter<DreamHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) : DreamHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemDreamBinding.inflate(inflater, parent, false)
        return DreamHolder(binding)
    }

    override fun onBindViewHolder(holder: DreamHolder, position: Int) {
        val dream = dreams[position]
        holder.bind(dream, onDreamClicked)
    }

    override fun getItemCount() = dreams.size
}