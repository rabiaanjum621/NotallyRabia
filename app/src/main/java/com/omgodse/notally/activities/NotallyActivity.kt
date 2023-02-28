package com.omgodse.notally.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.omgodse.notally.R
import com.omgodse.notally.TextSizeEngine
import com.omgodse.notally.databinding.ActivityNotallyBinding
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.miscellaneous.Operations
import com.omgodse.notally.miscellaneous.add
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.Folder
import com.omgodse.notally.room.Type
import com.omgodse.notally.viewmodels.BaseNoteModel
import com.omgodse.notally.viewmodels.NotallyModel
import kotlinx.coroutines.launch

abstract class NotallyActivity(private val type: Type) : AppCompatActivity() {

    internal lateinit var binding: ActivityNotallyBinding
    internal val model: NotallyModel by viewModels { NotallyModel.Factory(application, type) }

    override fun onBackPressed() {
        model.saveNote { super.onBackPressed() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        model.saveNote {}
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialiseBinding()
        setContentView(binding.root)
        setupToolbar()

        if (model.isFirstInstance) {
            val selectedBaseNote = intent.getParcelableExtra<BaseNote>(Constants.SelectedBaseNote)
            if (selectedBaseNote != null) {
                model.isNewNote = false
                model.setStateFromBaseNote(selectedBaseNote)
            } else model.isNewNote = true

            if (intent.action == Intent.ACTION_SEND) {
                receiveSharedNote()
            }

            model.isFirstInstance = false
        }

        setupListeners()
        setStateFromModel()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (menu != null) {
            val pin = menu.add(R.string.pin, R.drawable.pin) { item -> pin(item) }
            bindPinned(pin)

            menu.add(R.string.share, R.drawable.share) { share() }
            menu.add(R.string.labels, R.drawable.label) { label() }

            when (model.folder) {
                Folder.NOTES -> {
                    menu.add(R.string.delete, R.drawable.delete) { delete() }
                    menu.add(R.string.delete_permanently, R.drawable.delete) {
                        deleteForever(R.string.delete_note_permanently)
                    }
                    menu.add(R.string.archive, R.drawable.archive) { archive() }
                }
                Folder.DELETED -> {
                    menu.add(R.string.restore, R.drawable.restore) { restore(Constants.DeletedResultValue) }
                    menu.add(R.string.delete_forever, R.drawable.delete) { deleteForever(R.string.delete_note_forever) }
                }
                Folder.ARCHIVED -> {
                    menu.add(R.string.delete, R.drawable.delete) { delete() }
                    menu.add(R.string.unarchive, R.drawable.unarchive) { restore(Constants.ArchivedResultValue) }
                }
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }


    open fun receiveSharedNote() {}

    open fun setupListeners() {
        binding.EnterTitle.doAfterTextChanged { text ->
            model.title = requireNotNull(text).trim().toString()
        }
    }

    open fun setStateFromModel() {
        val formatter = BaseNoteModel.getDateFormatter(this)
        binding.DateCreated.text = formatter.format(model.timestamp)

        val color = Operations.extractColor(model.color, this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.statusBarColor = color
        }
        binding.root.setBackgroundColor(color)
        binding.RecyclerView.setBackgroundColor(color)
        binding.AppBarLayout.backgroundTintList = ColorStateList.valueOf(color)

        binding.EnterTitle.setText(model.title)
        Operations.bindLabels(binding.LabelGroup, model.labels, model.textSize)
    }


    private fun share() {
        val body = when (type) {
            Type.NOTE -> model.body
            Type.LIST -> Operations.getBody(model.items)
            Type.PHONE -> model.body
        }
        Operations.shareNote(this, model.title, body)
    }

    private fun label() {
        lifecycleScope.launch {
            val labels = model.getAllLabels()
            val onUpdated = { newLabels: HashSet<String> ->
                model.labels = newLabels
                Operations.bindLabels(binding.LabelGroup, newLabels, model.textSize)
            }
            val addLabel = { Operations.displayAddLabelDialog(this@NotallyActivity, model::insertLabel) { label() } }
            Operations.labelNote(this@NotallyActivity, labels, model.labels, onUpdated, addLabel)
        }
    }

    private fun delete() {
        model.moveBaseNoteToDeleted()
        onBackPressed()
    }

    private fun restore(value: String) {
        model.restoreBaseNote()
        model.saveNote {
            val intent = Intent().putExtra(Constants.RestoreResultKey, value)
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    private fun archive() {
        model.moveBaseNoteToArchive()
        onBackPressed()
    }

    private fun deleteForever(messageId: Int) {
        MaterialAlertDialogBuilder(this)
            .setMessage(messageId)
            .setPositiveButton(R.string.delete) { dialog, which ->
                model.deleteBaseNoteForever {
                    super.onBackPressed()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun pin(item: MenuItem) {
        model.pinned = !model.pinned
        bindPinned(item)
    }


    private fun setupToolbar() {
        setSupportActionBar(binding.Toolbar)
        supportActionBar?.title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initialiseBinding() {
        binding = ActivityNotallyBinding.inflate(layoutInflater)
        when (type) {
            Type.NOTE -> {
                binding.AddItem.visibility = View.GONE
                binding.RecyclerView.visibility = View.GONE
                binding.EnterNumber.visibility = View.GONE
            }
            Type.LIST -> {
                binding.EnterBody.visibility = View.GONE
                binding.EnterNumber.visibility = View.GONE
            }
            Type.PHONE -> {
                binding.EnterTitle.hint = resources.getString(R.string.name)
                binding.AddItem.visibility = View.GONE
                binding.EnterBody.visibility = View.GONE
                binding.RecyclerView.visibility = View.GONE
            }
        }

        val title = TextSizeEngine.getEditTitleSize(model.textSize)
        val date = TextSizeEngine.getDisplayBodySize(model.textSize)
        val body = TextSizeEngine.getEditBodySize(model.textSize)

        binding.EnterTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, title)
        binding.DateCreated.setTextSize(TypedValue.COMPLEX_UNIT_SP, date)
        binding.EnterBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, body)

        binding.root.isSaveFromParentEnabled = false
    }

    private fun bindPinned(item: MenuItem) {
        val icon: Int
        val title: Int
        if (model.pinned) {
            icon = R.drawable.unpin
            title = R.string.unpin
        } else {
            icon = R.drawable.pin
            title = R.string.pin
        }
        item.setTitle(title)
        item.setIcon(icon)
    }
}