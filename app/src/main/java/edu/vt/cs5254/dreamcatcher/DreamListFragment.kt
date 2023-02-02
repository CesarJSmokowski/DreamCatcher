package edu.vt.cs5254.dreamcatcher

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.vt.cs5254.dreamcatcher.databinding.FragmentDreamListBinding
import kotlinx.coroutines.launch


class DreamListFragment : Fragment()  {

    private var _binding: FragmentDreamListBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    private val dreamListViewModel: DreamListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDreamListBinding.inflate(inflater, container, false)

        requireActivity().addMenuProvider(object : MenuProvider {

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_dream_list, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.new_dream -> {
                        showNewDream()
                        true
                    }
                    else -> false
                }
            }
        },
        viewLifecycleOwner
    )

    getItemTouchHelper().attachToRecyclerView(binding.dreamRecyclerView)

        binding.dreamRecyclerView.layoutManager = LinearLayoutManager(context)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.noDreamAddButton.setOnClickListener {
            showNewDream()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dreamListViewModel.dreams.collect { dreams ->

                    if (dreams.isEmpty()) {
                        binding.dreamRecyclerView.visibility = View.GONE
                        binding.noDreamText.visibility = View.VISIBLE
                        binding.noDreamAddButton.visibility = View.VISIBLE
                    }
                    else {
                        binding.dreamRecyclerView.visibility = View.VISIBLE
                        binding.noDreamAddButton.visibility = View.GONE
                        binding.noDreamText.visibility = View.GONE
                    }


                    binding.dreamRecyclerView.adapter =
                        DreamListAdapter(dreams) { dreamId ->
                            findNavController().navigate(
                                DreamListFragmentDirections.showDreamDetail(dreamId)
                            )
                        }
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showNewDream() {
        viewLifecycleOwner.lifecycleScope.launch {
            val newDream = Dream()
            dreamListViewModel.addDream(newDream)
            findNavController().navigate(
                DreamListFragmentDirections.showDreamDetail(newDream.id)
            )
        }
    }

    private fun getItemTouchHelper(): ItemTouchHelper {

        return ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean = true

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    viewLifecycleOwner.lifecycleScope.launch {
                                val dreamHolder: DreamHolder = viewHolder as DreamHolder
                                val dream = dreamHolder.boundDream;
                                dreamListViewModel.deleteDream(dream)
                            }

                    }
            }
         )
    }


}