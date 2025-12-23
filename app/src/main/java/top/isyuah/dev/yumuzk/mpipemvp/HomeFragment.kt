package top.isyuah.dev.yumuzk.mpipemvp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.core.widget.NestedScrollView
import com.google.android.material.button.MaterialButton

class HomeFragment : Fragment(R.layout.fragment_home) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val btnFeature = view.findViewById<MaterialButton>(R.id.btn_enter_feature)
        val btnSynonym = view.findViewById<MaterialButton>(R.id.btn_synonym_compare)
        val btnPhoto = view.findViewById<MaterialButton>(R.id.btn_photo_search)

        btnFeature.setOnClickListener {
            startActivity(Intent(requireContext(), FeatureActivity::class.java))
        }
        btnSynonym.setOnClickListener {
            startActivity(Intent(requireContext(), SynonymCompareActivity::class.java))
        }
        btnPhoto.setOnClickListener {
            startActivity(Intent(requireContext(), PhotoSearchActivity::class.java))
        }

        // Handle WindowInsets so bottom navigation / gesture bar doesn't cover content.
        val scroll = view.findViewById<NestedScrollView>(R.id.scroll_home)
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { _, insets ->
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            // add a small extra padding so the last card isn't flush to the nav bar
            val extra = (16 * resources.displayMetrics.density).toInt()
            val baseBottom = navInsets.bottom + extra

            // apply base padding first (covers system navigation bar)
            scroll.updatePadding(bottom = baseBottom)

            // Also account for the app's floating bottom navigation card, if present.
            // We find the container by id and add its measured height once laid out.
            val bottomNavContainer = requireActivity().findViewById<View>(R.id.bottom_nav_container)
            bottomNavContainer?.post {
                val navHeight = bottomNavContainer.height
                if (navHeight > 0) {
                    scroll.updatePadding(bottom = baseBottom + navHeight)
                }
            }

            insets
        }
        // Make sure insets are applied immediately
        ViewCompat.requestApplyInsets(scroll)
    }
}

