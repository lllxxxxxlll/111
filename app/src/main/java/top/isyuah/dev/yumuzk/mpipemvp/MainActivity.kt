package top.isyuah.dev.yumuzk.mpipemvp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import top.isyuah.dev.yumuzk.mpipemvp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private fun navigateTo(itemId: Int): Boolean {
        val fragment: Fragment = when (itemId) {
            R.id.nav_home -> HomeFragment()
            R.id.nav_profile -> ProfileFragment()
            else -> return false
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.setOnItemSelectedListener { item ->
            navigateTo(item.itemId)
        }

        binding.bottomNav.setOnItemReselectedListener { /* no-op */ }

        if (savedInstanceState == null) {
            navigateTo(R.id.nav_home)
            binding.bottomNav.menu.findItem(R.id.nav_home)?.isChecked = true
        }
    }
}
