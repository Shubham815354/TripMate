package com.example.tripmate.myFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tripmate.MyAuthMVVM.UserViewModelFactory
import com.example.tripmate.R
import com.example.tripmate.databinding.FragmentFavouritesBinding
import com.example.tripmate.myAdapter.FavAdapter
import com.example.tripmate.myRoom.AppDatabase
import com.example.tripmate.myRoomMvvm.UserRepository
import com.example.tripmate.myRoomMvvm.UserViewModel

class FavouritesFragment : Fragment() {
    lateinit var binding : FragmentFavouritesBinding
    lateinit var adapter : FavAdapter
    lateinit var viewModel: UserViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_favourites,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyFav.layoutManager = LinearLayoutManager(requireContext())
        adapter = FavAdapter()
        binding.recyFav.adapter = adapter

        val repository = UserRepository(AppDatabase.get_instance(requireActivity()).user_dao())
        val factory = com.example.tripmate.myRoomMvvm.UserViewModelFactory(repository)
        viewModel = ViewModelProvider(requireActivity(),factory)[UserViewModel::class.java]

        adapter.setomRemove { userlist ->
            viewModel.check_existing(userlist)
        }
        viewModel.fav_.observe(requireActivity()){ userlist ->
            adapter.setOnFav(userlist)
            adapter.submitList(userlist)
        }
        viewModel.load_data()



    }

    override fun onResume() {
        super.onResume()
        viewModel.load_data()
    }


}