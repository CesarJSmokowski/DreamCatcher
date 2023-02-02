package edu.vt.cs5254.dreamcatcher

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.view.doOnLayout
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import edu.vt.cs5254.dreamcatcher.databinding.FragmentPhotoDialogBinding
import java.io.File

class PhotoDialogFragment : DialogFragment()  {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = FragmentPhotoDialogBinding.inflate(layoutInflater)
        val args: PhotoDialogFragmentArgs by navArgs()

        //----- Adapting Listing 17.15 ------
        with(binding.photoDetail) {
            val photoFile = File(requireContext().applicationContext.filesDir, args.dreamPhotoFilename)
            if (photoFile.exists()) {
                binding.root.doOnLayout { measuredView ->
                    val scaledBM = getScaledBitmap(
                        photoFile.path,
                        measuredView.width,
                        measuredView.height
                    )
                    setImageBitmap(scaledBM)
                }
            }
        }
        //----- Adapting Listing 17.15 ------

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .show()
    }
}