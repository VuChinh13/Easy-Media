package com.example.easymedia.ui.component.animation;

import androidx.fragment.app.FragmentTransaction;
/**
 * This class is used to define transition effects when switching between Fragments.
 * This is an extension function for an existing class.
*/
object FragmentTransactionAnimation {
    fun FragmentTransaction.setSlideAnimations(): FragmentTransaction {
        return this.setCustomAnimations(
            ScreenTransitionAnimation.slideInRightAnimation,
            ScreenTransitionAnimation.slideOutLeftAnimation,
            ScreenTransitionAnimation.defaultEnterAnimation,
            ScreenTransitionAnimation.defaultExitAnimation,
        )
    }
}
