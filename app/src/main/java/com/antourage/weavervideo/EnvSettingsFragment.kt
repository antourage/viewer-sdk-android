package com.antourage.weavervideo

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.antourage.weaverlib.dev_settings.UserCache
import com.antourage.weaverlib.dev_settings.DevSettingsDialog
import com.antourage.weaverlib.dev_settings.OnDevSettingsChangedListener

class EnvSettingsFragment : Fragment(), OnDevSettingsChangedListener {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_env_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dialog = DevSettingsDialog(requireContext(), this)
        dialog.show()
    }

    override fun onBeChanged(choice: String) {
        choice.let {
            UserCache.getInstance(requireContext())
                ?.updateEnvChoice(choice)
        }
    }

}