package com.example.easymedia.extension

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

fun <T> Flow<T>.observe(
    owner: LifecycleOwner,
    minActiveState: Lifecycle.State = Lifecycle.State.CREATED,
    action: suspend (T) -> Unit
) {
    flowWithLifecycle(owner.lifecycle, minActiveState)
        .onEach(action)
        .launchIn(owner.lifecycleScope)
}