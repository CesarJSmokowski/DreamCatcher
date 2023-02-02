package edu.vt.cs5254.dreamcatcher

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.MenuProvider
import androidx.core.view.doOnLayout
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.vt.cs5254.dreamcatcher.databinding.FragmentDreamDetailBinding
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat

private const val TAG = "DreamDetailViewModel.kt"

//name: Cesar Smokowski
//PID: cesar8800

class DreamDetailFragment : Fragment() {

    private var _binding: FragmentDreamDetailBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    private val args: DreamDetailFragmentArgs by navArgs()

    private val dreamDetailViewModel: DreamDetailViewModel by viewModels {
        DreamDetailViewModelFactory(args.dreamId)
    }


    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { didTakePhoto: Boolean ->
        if (didTakePhoto) {
            binding.dreamPhoto.tag = null
            dreamDetailViewModel.dream.value?.let { updatePhoto(it) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDreamDetailBinding.inflate(layoutInflater, container, false)

        requireActivity().addMenuProvider(object : MenuProvider {

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_dream_detail, menu)

                val captureImageIntent = takePhoto.contract.createIntent(
                    requireContext(),
                    Uri.EMPTY // NOTE: The "null" used in BNRG is obsolete now
                )
                menu.findItem(R.id.take_photo_menu).isVisible = canResolveIntent(captureImageIntent)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.share_dream_menu -> {
                        dreamDetailViewModel.dream.value?.let { shareDream(it) }
                        true
                    }
                    R.id.take_photo_menu -> {
                        dreamDetailViewModel.dream.value?.let {
                            val photoFile = File(
                                requireContext().applicationContext.filesDir,
                                it.photoFileName
                            )
                            val photoUri = FileProvider.getUriForFile(
                                requireContext(),
                                "edu.vt.cs5254.dreamcatcher.fileprovider",
                                photoFile
                            )
                            takePhoto.launch(photoUri)
                        }
                        true
                    }
                    else -> false
                }
            }
        },
            viewLifecycleOwner
        )

        getItemTouchHelper().attachToRecyclerView(binding.dreamEntryRecycler)
        binding.dreamEntryRecycler.layoutManager = LinearLayoutManager(context)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {

            titleText.doOnTextChanged { text, _, _, _ ->
                dreamDetailViewModel.updateDream { oldDream ->
                    oldDream.copy(title = text.toString()).apply { entries = oldDream.entries }
                }
            }

            fulfilledCheckbox.setOnClickListener {
                dreamDetailViewModel.updateDream { oldDream ->
                    if (oldDream.isFulfilled) {
                        val newEntries: List<DreamEntry> = oldDream.entries.filter {
                            it.kind != DreamEntryKind.FULFILLED
                        }
                        oldDream.copy().apply { entries = newEntries }
                    }
                    else {
                        val newEntry = DreamEntry(
                            kind = DreamEntryKind.FULFILLED,
                            text = "",
                            dreamId = oldDream.id
                        )
                        oldDream.copy().apply { entries = oldDream.entries + newEntry }
                    }
                }
            }

            deferredCheckbox.setOnClickListener {
                dreamDetailViewModel.updateDream { oldDream ->
                    if (oldDream.isDeferred) {
                        val newEntries: List<DreamEntry> = oldDream.entries.filter {
                            it.kind != DreamEntryKind.DEFERRED
                        }
                        oldDream.copy().apply { entries = newEntries }
                    }
                    else {
                        val newEntry = DreamEntry(
                            kind = DreamEntryKind.DEFERRED,
                            text = "",
                            dreamId = oldDream.id
                        )
                        oldDream.copy().apply { entries = oldDream.entries + newEntry }
                    }
                }
            }

            addReflectionButton.setOnClickListener {
                findNavController().navigate(
                    DreamDetailFragmentDirections.addReflection()
                )
            }

            dreamPhoto.setOnClickListener {
                    dreamDetailViewModel.dream.value?.let { dream ->
                        findNavController().navigate(
                            DreamDetailFragmentDirections.showPhotoDetail(dream.photoFileName)
                        )
                    }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dreamDetailViewModel.dream.collect { dream ->
                    dream?.let { updateUi(it) }

                    //Part 7
                    binding.dreamEntryRecycler.visibility = View.VISIBLE
                    if (dream != null) {
                        binding.dreamEntryRecycler.adapter = DreamEntryAdapter(dream.entries)
                    }
                }
            }
        }


        setFragmentResultListener(
            ReflectionDialogFragment.REQUEST_KEY
        ) { _, bundle ->
            val entryText = bundle.getString(ReflectionDialogFragment.BUNDLE_KEY) ?: ""

            dreamDetailViewModel.updateDream { oldDream ->

                val newEntry = DreamEntry(
                    kind = DreamEntryKind.REFLECTION,
                    text = entryText,
                    dreamId = oldDream.id
                )
                oldDream.copy().apply { entries = oldDream.entries + newEntry }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateUi(dream: Dream) {
        binding.apply {

            if (dream.isFulfilled) {
                addReflectionButton.hide()
            }
            else {
                addReflectionButton.show()
            }

            if (titleText.text.toString() != dream.title) {
                titleText.setText(dream.title)
            }

            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd 'at' hh:mm:ss a")
            val date: String = simpleDateFormat.format(dream.lastUpdated)
            lastUpdatedText.text = getString(R.string.dream_details_time_value, date)

            fulfilledCheckbox.setEnabled(!dream.isDeferred)
            fulfilledCheckbox.setChecked(dream.isFulfilled)
            deferredCheckbox.setEnabled(!dream.isFulfilled)
            deferredCheckbox.setChecked(dream.isDeferred)
        }
        updatePhoto(dream)
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

    private fun updatePhoto(dream: Dream) {
        with(binding.dreamPhoto) {
            if (tag != dream.photoFileName) {
                val photoFile =
                    File(requireContext().applicationContext.filesDir, dream.photoFileName)
                if (photoFile.exists()) {
                    doOnLayout { measuredView ->
                        val scaledBM = getScaledBitmap(
                            photoFile.path,
                            measuredView.width,
                            measuredView.height
                        )
                        setImageBitmap(scaledBM)
                        tag = dream.photoFileName
                        binding.dreamPhoto.isEnabled = true //TEST
                    }
                } else {
                    setImageBitmap(null)
                    tag = null
                    binding.dreamPhoto.isEnabled = false //TEST
                }
            }
        }
    }

    private fun getDreamReport(dream: Dream): String {
        val dreamTitle = dream.title + "\n"

        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd 'at' hh:mm:ss a")
        val date: String = simpleDateFormat.format(dream.lastUpdated)
        val dreamDate = "Last updated $date\n"

        var reflectionString = ""

        val reflectionArray = dream.entries.filter {
            it.kind == DreamEntryKind.REFLECTION
        }

        if (!reflectionArray.isEmpty()) {
            val reflectionList = reflectionArray.joinToString (separator = "\n * ", prefix = "\n * ") {it.text}

            reflectionString = getString(R.string.dream_reflections, reflectionList) + "\n"
        }

        var deferredOrFulfilled = ""
        if (dream.isFulfilled) {
            deferredOrFulfilled = getString(R.string.dream_deferred_or_fulfilled, "Fulfilled")
        }
        else if (dream.isDeferred) {
            deferredOrFulfilled = getString(R.string.dream_deferred_or_fulfilled, "Deferred")
        }

        val finalString = dreamTitle + dreamDate + reflectionString + deferredOrFulfilled

        return finalString


    }

    private fun shareDream(dream: Dream) {

        val reportIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getDreamReport(dream))
            putExtra(
                Intent.EXTRA_SUBJECT,
                getString(R.string.dream_report_subject)
            )
        }
        val chooserIntent = Intent.createChooser(
            reportIntent,
            getString(R.string.send_report)
        )
        startActivity(chooserIntent)
    }

    private fun canResolveIntent(intent: Intent): Boolean {
        val packageManager: PackageManager = requireActivity().packageManager
        val resolvedActivity: ResolveInfo? =
            packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
        return resolvedActivity != null
    }


    private fun getItemTouchHelper(): ItemTouchHelper {

        return ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, 0) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = true

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                super.getSwipeDirs(recyclerView, viewHolder)
                val dreamEntryHolder: DreamEntryHolder = viewHolder as DreamEntryHolder
                val entry = dreamEntryHolder.boundEntry
                return if (entry.kind == DreamEntryKind.REFLECTION) {
                    ItemTouchHelper.LEFT
                } else {
                    0
                }
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                viewLifecycleOwner.lifecycleScope.launch {

                    if (getSwipeDirs(binding.dreamEntryRecycler, viewHolder) == ItemTouchHelper.LEFT) {
                        val dreamEntryHolder: DreamEntryHolder = viewHolder as DreamEntryHolder
                        val entry = dreamEntryHolder.boundEntry
                        dreamDetailViewModel.updateDream { oldDream ->

                            val newEntries: List<DreamEntry> = oldDream.entries.filter {
                                it != entry
                            }
                            oldDream.copy().apply { entries = newEntries }
                        }
                    }

                }

            }
        }
        )
    }
}
