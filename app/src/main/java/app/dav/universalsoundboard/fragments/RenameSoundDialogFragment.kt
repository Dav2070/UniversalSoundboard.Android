package app.dav.universalsoundboard.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import app.dav.universalsoundboard.R
import app.dav.universalsoundboard.data.FileManager
import app.dav.universalsoundboard.models.Sound
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class RenameSoundDialogFragment : DialogFragment() {
    var sound: Sound? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return createDialog() ?: super.onCreateDialog(savedInstanceState)
    }

    private fun createDialog() : AlertDialog?{
        if(sound == null) return null
        val layout = activity?.layoutInflater?.inflate(R.layout.dialog_rename_sound, null) ?: return null
        val nameEditText = layout.findViewById<TextInputEditText>(R.id.rename_sound_dialog_name_input)

        // Set the text of the EditText
        nameEditText.setText(sound?.name)

        val alertDialog = AlertDialog.Builder(activity, R.style.MaterialDesignAlertDialog)
                .setView(layout)
                .setPositiveButton(R.string.dialog_save, DialogInterface.OnClickListener { dialog, which ->
                    val s = sound ?: return@OnClickListener
                    GlobalScope.launch(Dispatchers.Main) {
                        FileManager.renameSound(s.uuid, nameEditText.text.toString())
                        FileManager.itemViewHolder.loadSounds()
                    }
                })
                .setNegativeButton(R.string.dialog_negative_button, null)
                .create()

        alertDialog.show()

        val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.isEnabled = true

        // Disable the positive button when the name is too short
        nameEditText.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(p0: Editable?) {
                positiveButton.isEnabled = nameEditText.text?.length ?: 0 > 1
            }
        })

        return alertDialog
    }
}