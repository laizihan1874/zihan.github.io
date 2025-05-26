package com.example.fitnesstrackerapp.tracking.viewmodel

import android.location.Location
import android.os.SystemClock
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.robolectric.Shadows.shadowOf // For SystemClock with Robolectric if needed
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.util.concurrent.TimeUnit
import org.mockito.Mockito.`when` as whenever // Alias for Mockito.when

// Since SystemClock.elapsedRealtime() is used, we might need Robolectric or PowerMockito.
// For simplicity here, I'll try to control time by directly setting elapsedTime LiveData for pace tests,
// and for startTracking, I'll verify trackingStartTimeMillis which uses System.currentTimeMillis().
// A full test of the timer coroutine itself is more complex.

@RunWith(MockitoJUnitRunner::class)
class LiveTrackingViewModelTest {

    @get:Rule
    @JvmField
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: LiveTrackingViewModel

    @Mock
    private lateinit var observerElapsedTime: Observer<Long>
    @Mock
    private lateinit var observerIsTracking: Observer<Boolean>
    @Mock
    private lateinit var observerLocationPointsList: Observer<MutableList<Pair<Double, Double>>>
    @Mock
    private lateinit var observerTotalDistanceMeters: Observer<Float>
    @Mock
    private lateinit var observerCurrentPaceMinutesPerKm: Observer<Float>


    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        viewModel = LiveTrackingViewModel()

        // Observe all LiveData
        viewModel.elapsedTime.observeForever(observerElapsedTime)
        viewModel.isTracking.observeForever(observerIsTracking)
        viewModel.locationPointsList.observeForever(observerLocationPointsList)
        viewModel.totalDistanceMeters.observeForever(observerTotalDistanceMeters)
        viewModel.currentPaceMinutesPerKm.observeForever(observerCurrentPaceMinutesPerKm)
    }
    
    @After
    fun tearDown() {
        viewModel.elapsedTime.removeObserver(observerElapsedTime)
        viewModel.isTracking.removeObserver(observerIsTracking)
        viewModel.locationPointsList.removeObserver(observerLocationPointsList)
        viewModel.totalDistanceMeters.removeObserver(observerTotalDistanceMeters)
        viewModel.currentPaceMinutesPerKm.removeObserver(observerCurrentPaceMinutesPerKm)
    }


    // Mocking Location.distanceBetween is hard without PowerMockito or a wrapper.
    // Instead, we'll provide points and verify the sum based on known behavior.
    // For precise distance calculation testing, an instrumented test or a testable wrapper around Location might be better.
    // Here, we'll assume Location.distanceBetween works as expected and test our aggregation.
    @Test
    fun `addLocationPoint updates totalDistanceMeters correctly`() {
        // Test with no points
        assertEquals(0f, viewModel.totalDistanceMeters.value)

        // Test with one point (distance should remain 0)
        viewModel.addLocationPoint(0.0, 0.0)
        assertEquals(0f, viewModel.totalDistanceMeters.value)
        assertEquals(1, viewModel.locationPointsList.value?.size)

        // Add a second point (approx 111km or 1 degree latitude)
        // Location.distanceBetween(0.0, 0.0, 1.0, 0.0, results) -> results[0] is approx 111195.0m
        viewModel.addLocationPoint(1.0, 0.0) 
        val results = FloatArray(1)
        Location.distanceBetween(0.0, 0.0, 1.0, 0.0, results)
        assertEquals(results[0], viewModel.totalDistanceMeters.value)
        assertEquals(2, viewModel.locationPointsList.value?.size)

        // Add a third point
        // Location.distanceBetween(1.0, 0.0, 1.0, 1.0, results2) -> results2[0] is approx 111195.0m (at equator)
        viewModel.addLocationPoint(1.0, 1.0)
        val results2 = FloatArray(1)
        Location.distanceBetween(1.0, 0.0, 1.0, 1.0, results2)
        // The ViewModel's distance calculation iterates through segments.
        // So the new distance should be sum of (p0-p1) + (p1-p2)
        // For this test, we are testing the accumulation logic.
        // The LiveTrackingViewModel calculates the total distance from scratch each time.
        // So, after adding (1.0, 1.0), the path is (0,0)->(1,0)->(1,1)
        // Distance = dist( (0,0)->(1,0) ) + dist( (1,0)->(1,1) )
        val expectedDistance = results[0] + results2[0]
        assertEquals(expectedDistance, viewModel.totalDistanceMeters.value)
        assertEquals(3, viewModel.locationPointsList.value?.size)
    }
    
    @Test
    fun `addLocationPoint with single point results in zero distance and pace`() {
        viewModel.addLocationPoint(34.0, -118.0)
        assertEquals(0f, viewModel.totalDistanceMeters.value)
        assertEquals(0f, viewModel.currentPaceMinutesPerKm.value)
    }


    @Test
    fun `calculatePace updates currentPaceMinutesPerKm correctly`() {
        // Set up conditions for pace calculation
        // The pace calculation is inside calculateMetrics, which is called by addLocationPoint
        // and also by the timer loop in startTracking.
        // We need to simulate elapsed time and distance.

        // Scenario 1: Valid pace
        // Simulate 1km (1000m) in 5 minutes (300,000 ms)
        // Direct manipulation of LiveData for testing specific calculation parts
        (viewModel.elapsedTime as MutableLiveData<Long>).value = 300000L // 5 minutes
        // Add two points to make distance calculation work
        viewModel.addLocationPoint(0.0, 0.0) // Point 1
        viewModel.addLocationPoint(0.009, 0.0) // Point 2 (approx 1000m, 0.009 degrees lat)
        // Distance for (0,0) to (0.009,0) is approx 1000m
        
        // Expected pace: 5 min / 1 km = 5.0 min/km
        // Allow a small delta for floating point comparisons
        assertTrue((viewModel.currentPaceMinutesPerKm.value ?: 0f) > 4.9f && (viewModel.currentPaceMinutesPerKm.value ?: 0f) < 5.1f )

        // Scenario 2: Zero distance
        (viewModel.elapsedTime as MutableLiveData<Long>).value = 60000L // 1 minute
        // Reset points and add only one, or ensure distance is zero
        (viewModel.locationPointsList as MutableLiveData<MutableList<Pair<Double, Double>>>).value = mutableListOf(Pair(0.0,0.0))
        viewModel.calculateMetrics() // Manually trigger if not adding points via addLocationPoint
        assertEquals(0f, viewModel.currentPaceMinutesPerKm.value)

        // Scenario 3: Zero time (though unlikely if tracking started)
        (viewModel.elapsedTime as MutableLiveData<Long>).value = 0L
        viewModel.addLocationPoint(0.0, 0.0)
        viewModel.addLocationPoint(0.009, 0.0) // Has distance
        assertEquals(0f, viewModel.currentPaceMinutesPerKm.value)
    }
    
    @Test
    fun `startTracking sets trackingStartTimeMillis on fresh start`() {
        // Ensure timeWhenPaused and elapsedTime are 0 for a fresh start
        (viewModel.elapsedTime as MutableLiveData<Long>).value = 0L 
        // timeWhenPaused is private, but its effect is on trackingStartTimeMillis calculation.
        // We are checking if trackingStartTimeMillis is set close to current time.
        
        val beforeStart = System.currentTimeMillis()
        viewModel.startTracking() // This will start a coroutine
        val afterStart = System.currentTimeMillis()

        assertTrue(viewModel.trackingStartTimeMillis >= beforeStart)
        assertTrue(viewModel.trackingStartTimeMillis <= afterStart)
        assertTrue(viewModel.isTracking.value == true)
        // Stop tracking to cancel coroutine
        viewModel.stopTracking()
    }

    @Test
    fun `startTracking does not reset trackingStartTimeMillis on resume`() {
        // Simulate a previous start
        val initialStartTime = System.currentTimeMillis() - 10000 // 10 secs ago
        viewModel.trackingStartTimeMillis = initialStartTime
        
        // Simulate being paused: isTracking is false, timeWhenPaused is > 0
        (viewModel.isTracking as MutableLiveData<Boolean>).value = false 
        viewModel.timeWhenPaused = 5000L // Paused at 5s elapsed
        (viewModel.elapsedTime as MutableLiveData<Long>).value = 5000L


        viewModel.startTracking() // Resume

        assertEquals(initialStartTime, viewModel.trackingStartTimeMillis)
        assertTrue(viewModel.isTracking.value == true)
        // Stop tracking to cancel coroutine
        viewModel.stopTracking()
    }


    @Test
    fun `stopTracking resets all relevant LiveData and properties`() {
        // Set some values as if tracking was active
        (viewModel.isTracking as MutableLiveData<Boolean>).value = true
        (viewModel.elapsedTime as MutableLiveData<Long>).value = 10000L
        viewModel.trackingStartTimeMillis = System.currentTimeMillis() - 10000L
        (viewModel.locationPointsList.value)?.add(Pair(1.0, 1.0))
        (viewModel.locationPointsList.value)?.add(Pair(2.0, 2.0))
        (viewModel.totalDistanceMeters as MutableLiveData<Float>).value = 100f
        (viewModel.currentPaceMinutesPerKm as MutableLiveData<Float>).value = 5.0f
        viewModel.timeWhenPaused = 0L // Ensure it's not paused

        viewModel.stopTracking()

        assertEquals(false, viewModel.isTracking.value)
        assertEquals(0L, viewModel.elapsedTime.value)
        assertEquals(0L, viewModel.trackingStartTimeMillis)
        assertTrue(viewModel.locationPointsList.value?.isEmpty() ?: false)
        assertEquals(0f, viewModel.totalDistanceMeters.value)
        assertEquals(0f, viewModel.currentPaceMinutesPerKm.value)
        assertEquals(0L, viewModel.timeWhenPaused) // Check if timeWhenPaused is reset too
    }
}
