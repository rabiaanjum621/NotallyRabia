package com.omgodse.notally.activities

import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import com.omgodse.notally.miscellaneous.setOnNextAction
import com.omgodse.notally.room.Type

class PhoneNumberNote: NotallyActivity(Type.PHONE) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.EnterTitle.setOnNextAction {
            binding.EnterNumber.requestFocus()
        }
    }

    override fun setupListeners() {
        super.setupListeners()
        binding.EnterNumber.doAfterTextChanged { text ->
            model.body = text
        }
    }

    override fun setStateFromModel() {
        super.setStateFromModel()
        binding.EnterNumber.text = model.body
    }
}