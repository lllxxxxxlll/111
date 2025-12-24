package top.isyuah.dev.yumuzk.mpipemvp

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import top.isyuah.dev.yumuzk.mpipemvp.databinding.FragmentProfileBinding

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)
        
        updateStatistics()
    }
    
    private fun updateStatistics() {
        val repository = PhotoRepository.getInstance(requireContext())
        
        // 更新照片数量
        val photosCount = repository.getAllPhotos().size
        binding.tvPhotosCount.text = photosCount.toString()
        
        // 更新问题数量
        val questionsCount = repository.getQuestionCount()
        binding.tvQuestionsCount.text = questionsCount.toString()
        
        // 更新使用天数
        val daysCount = repository.getUsageDays()
        binding.tvDaysCount.text = daysCount.toString()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

