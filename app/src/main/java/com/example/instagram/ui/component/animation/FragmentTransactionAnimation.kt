package com.example.instagram.ui.component.animation;

import androidx.fragment.app.FragmentTransaction;
/*
    Lớp này dùng để định nghĩa những hiệu ứng khi mà chuyển màn Fragment
    đây là 1 hàm extension cho lớp mà đã có sẵn rồi
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
