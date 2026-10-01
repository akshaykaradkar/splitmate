package com.splitmate.app.ui.screens.plan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * v2.3.4 Plan tab entry point for TripHomeScreen's PLAN branch.
 *
 * - The [TripGuideViewModel] (and the app-scoped guide graph behind it) is created only when this
 *   composable enters composition, so opening the app or other tabs costs nothing.
 * - One ViewModel per trip (keyed by [groupId]), scoped to the hosting NavBackStackEntry/Activity.
 * - Visibility: the VM's plan-sync heartbeat and shared-link intake run only while this host is
 *   composed AND the lifecycle is at least STARTED.
 * - [onSubViewChanged] lets the parent hide the add-expense FAB on Explore/Loop.
 */
@Composable
fun TripPlanTabHost(
    groupId: String,
    bookingsContent: @Composable () -> Unit,
    onSubViewChanged: (PlanSubView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val factory = remember(groupId) { TripGuideViewModel.factory(context, groupId) }
    val vm: TripGuideViewModel = viewModel(key = "trip-guide-$groupId", factory = factory)
    val state by vm.uiState.collectAsStateWithLifecycle()

    val latestOnSubViewChanged by rememberUpdatedState(onSubViewChanged)
    LaunchedEffect(state.subView) { latestOnSubViewChanged(state.subView) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(vm, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> vm.onHostVisibilityChanged(true)
                Lifecycle.Event.ON_STOP -> vm.onHostVisibilityChanged(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        vm.onHostVisibilityChanged(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            vm.onHostVisibilityChanged(false)
        }
    }

    TripPlanTab(
        state = state,
        actions = vm,
        effects = vm.effects,
        bookingsContent = bookingsContent,
        modifier = modifier
    )
}
