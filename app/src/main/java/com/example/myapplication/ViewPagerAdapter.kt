package com.example.myapplication

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        // Ustawiamy kolejność swipe'owania: Journal -> Questy -> Profil
        return when (position) {
            0 -> JournalFragment()
            1 -> QuestsFragment()
            2 -> ProfileFragment()
            else -> JournalFragment()
        }
    }
}