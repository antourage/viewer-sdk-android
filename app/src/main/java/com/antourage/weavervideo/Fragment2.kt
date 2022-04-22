package com.antourage.weavervideo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_2.*

class Fragment2 : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        invalidateTitle()
        btnSet.setOnClickListener {
            val lang = etLanguage.text.toString()
            if (lang.isNotBlank()) {
                (activity as MainActivity).updateLanguageChoice(lang)
                LocaleHelper.setLocale(context, lang.lowercase())
                etLanguage.setText("")
                invalidateTitle()
                Toast.makeText(context, "Please restart application", Toast.LENGTH_SHORT).show()
            }
        }
        btnReset.setOnClickListener {
                (activity as MainActivity).updateLanguageChoice(null)
            Toast.makeText(context, "Please restart application", Toast.LENGTH_SHORT).show()
        }
    }

    private fun invalidateTitle(){
        tvLanguage.text = resources.getString(
            R.string.in_case_language_is_not_set_during_integration_n_will_used,
            (activity as MainActivity).getLanguage().uppercase()
        )
    }
}