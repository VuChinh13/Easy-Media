package com.example.instagram.ui.component.main

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.instagram.R
import com.example.instagram.databinding.ActivityMainBinding
import com.example.instagram.ui.component.addpost.AddPostFragment
import com.example.instagram.ui.component.animation.FragmentTransactionAnimation.setSlideAnimations
import com.example.instagram.ui.component.home.HomeFragment
import com.example.instagram.ui.component.myprofile.MyProfileFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    var fragmentCurrent = "HomeFragment"
    var fragmentPre = "HomeFragment"
    var countFragment = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val homeFragment = HomeFragment()
        val transactionHomeFragment = supportFragmentManager.beginTransaction()
        transactionHomeFragment.add(R.id.fragment, homeFragment)
        transactionHomeFragment.commit()

        binding.btnAdd.setOnClickListener {
            if (fragmentCurrent != "AddPostFragment") {
                if (fragmentPre != "HomeFragment")  fragmentPre = fragmentCurrent
                val addPostFragment = AddPostFragment()
                Log.d("kiem tra", "ok roi ma")
                val transactionAddPostFragment = supportFragmentManager.beginTransaction()
                transactionAddPostFragment.setSlideAnimations()
                transactionAddPostFragment.add(R.id.fragment, addPostFragment)
                transactionAddPostFragment.addToBackStack(null)
                transactionAddPostFragment.commit()
            }
        }


        binding.btnMyProfile.setOnClickListener {
            if (fragmentCurrent != "MyProfileFragment") {
                if (fragmentPre != "HomeFragment") fragmentPre = fragmentCurrent
                val myProfileFragment = MyProfileFragment()
                val transactionMyProfileFragment = supportFragmentManager.beginTransaction()
//                transactionMyProfileFragment.setSlideAnimations()
                transactionMyProfileFragment.add(R.id.fragment, myProfileFragment)
                transactionMyProfileFragment.addToBackStack(null)
                transactionMyProfileFragment.commit()
            }
        }


//        binding.btnSearch.setOnClickListener {
//            if (fragmentCurrent != "SearchFragment") {
//                if (fragmentPre != "HomeFragment") fragmentPre = fragmentCurrent
//                val searchFragment = SearchFragment()
//                val transactionMyProfileFragment = supportFragmentManager.beginTransaction()
//                transactionMyProfileFragment.setSlideAnimations()
//                transactionMyProfileFragment.add(R.id.fragment, searchFragment)
//                transactionMyProfileFragment.addToBackStack(null)
//                transactionMyProfileFragment.commit()
//            }
//        }


        binding.btnHome.setOnClickListener {
            if (fragmentCurrent == "AddPostFragment" || fragmentCurrent == "MyProfileFragment") {
                for (i in 0 until countFragment) {
                    supportFragmentManager.popBackStack()
                }
                fragmentCurrent = "HomeFragment"
                fragmentPre = "HomeFragment"
                countFragment = 0
            } else {
                val homeFragment = HomeFragment()
                val transactionHomeFragment = supportFragmentManager.beginTransaction()
                transactionHomeFragment.replace(R.id.fragment, homeFragment)
                transactionHomeFragment.commit()
                fragmentCurrent = "HomeFragment"
                fragmentPre = "HomeFragment"
                countFragment = 0
            }
        }
    }

    fun getBinding(): ActivityMainBinding = this.binding
}
