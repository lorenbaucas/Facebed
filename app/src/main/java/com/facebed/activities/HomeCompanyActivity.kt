package com.facebed.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.facebed.R
import com.facebed.databinding.HomeActivityBinding
import com.facebed.fragments.EventsCompanyFragment
import com.facebed.fragments.HomeCompanyFragment
import com.facebed.fragments.ProfileCompanyFragment
import com.google.android.material.navigation.NavigationBarView

class HomeCompanyActivity : AppCompatActivity() {
    private lateinit var homeActivityBinding: HomeActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_activity)

        //Menu principal con los fragments de la empresa
        homeActivityBinding = HomeActivityBinding.inflate(layoutInflater)
        setContentView(homeActivityBinding.root)
        homeActivityBinding.bottomNavigation.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_UNLABELED
        replaceFragment(HomeCompanyFragment())

        homeActivityBinding.bottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> replaceFragment(HomeCompanyFragment())
                R.id.navigation_events -> replaceFragment(EventsCompanyFragment())
                R.id.navigation_profile -> replaceFragment(ProfileCompanyFragment())
                else -> {}
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        fragmentTransaction.setCustomAnimations(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )

        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }
}