package top.isyuah.dev.yumuzk.mpipemvp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class HomeFragment : Fragment(R.layout.fragment_home) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialButton>(R.id.btn_enter_feature).setOnClickListener {
            startActivity(Intent(requireContext(), FeatureActivity::class.java))
        }
        view.findViewById<MaterialButton>(R.id.btn_synonym_compare).setOnClickListener {
            startActivity(Intent(requireContext(), SynonymCompareActivity::class.java))
        }
    }
}

