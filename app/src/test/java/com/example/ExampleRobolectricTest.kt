package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.example.ui.viewmodel.MainViewModel

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Simplex Privacy Vault", appName)
  }

  @Test
  fun `test inactivity timer updates on reset`() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = MainViewModel(context)
    val firstTime = viewModel.lastActivityTime.value
    Thread.sleep(10)
    viewModel.resetInactivityTimer()
    val secondTime = viewModel.lastActivityTime.value
    assertTrue(secondTime > firstTime)
  }

  @Test
  fun `test zero knowledge cloud backup settings toggle`() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = MainViewModel(context)
    
    // Default status should be disabled (false) and Isolated status
    assertEquals(false, viewModel.zkCloudBackupEnabled.value)
    assertEquals("Isolated", viewModel.zkCloudBackupStatus.value)

    // Toggle enabled
    viewModel.toggleZkCloudBackup(true)
    assertEquals(true, viewModel.zkCloudBackupEnabled.value)
    
    // Toggle disabled
    viewModel.toggleZkCloudBackup(false)
    assertEquals(false, viewModel.zkCloudBackupEnabled.value)
    assertEquals("Isolated", viewModel.zkCloudBackupStatus.value)
  }

  @Test
  fun `test zero knowledge cloud backup url changes`() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = MainViewModel(context)
    
    val initialUrl = viewModel.zkCloudBackupUrl.value
    assertEquals("https://cloud-sync.simplex.privacy", initialUrl)

    viewModel.setZkCloudBackupUrl("https://my-secure-node.custom")
    assertEquals("https://my-secure-node.custom", viewModel.zkCloudBackupUrl.value)
  }

  @Test
  fun `test zero knowledge sync frequency adjustment`() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = MainViewModel(context)
    
    assertEquals(5, viewModel.zkCloudSyncFrequencyMinutes.value)

    viewModel.setZkCloudSyncFrequency(30)
    assertEquals(30, viewModel.zkCloudSyncFrequencyMinutes.value)
  }
}
