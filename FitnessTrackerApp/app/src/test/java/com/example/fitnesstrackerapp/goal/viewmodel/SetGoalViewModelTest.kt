package com.example.fitnesstrackerapp.goal.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SetGoalViewModelTest {

    @get:Rule
    @JvmField
    var instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockApplication: Application

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    private lateinit var setGoalViewModel: SetGoalViewModel

    @Before
    fun setUp() {
        `when`(mockApplication.getSharedPreferences(SetGoalViewModel.PREFS_NAME, Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)

        // Mock editor's fluent interface
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)
        `when`(mockEditor.putLong(anyString(), anyLong())).thenReturn(mockEditor)

        setGoalViewModel = SetGoalViewModel(mockApplication)
    }

    @Test
    fun `saveGoal with valid target count saves to SharedPreferences and returns true`() {
        val targetCount = 5
        val result = setGoalViewModel.saveGoal(targetCount)

        assertTrue(result)
        verify(mockEditor).putString(eq(SetGoalViewModel.KEY_GOAL_TYPE), eq(SetGoalViewModel.DEFAULT_GOAL_TYPE))
        verify(mockEditor).putInt(eq(SetGoalViewModel.KEY_GOAL_TARGET_COUNT), eq(targetCount))
        verify(mockEditor).putLong(eq(SetGoalViewModel.KEY_GOAL_START_TIMESTAMP), anyLong())
        verify(mockEditor).apply()
    }

    @Test
    fun `saveGoal with zero target count does not save and returns false`() {
        val targetCount = 0
        val result = setGoalViewModel.saveGoal(targetCount)

        assertFalse(result)
        verify(mockEditor, never()).putInt(eq(SetGoalViewModel.KEY_GOAL_TARGET_COUNT), anyInt())
        verify(mockEditor, never()).apply()
    }

    @Test
    fun `saveGoal with negative target count does not save and returns false`() {
        val targetCount = -1
        val result = setGoalViewModel.saveGoal(targetCount)

        assertFalse(result)
        verify(mockEditor, never()).putInt(eq(SetGoalViewModel.KEY_GOAL_TARGET_COUNT), anyInt())
        verify(mockEditor, never()).apply()
    }

    @Test
    fun `currentGoalTarget LiveData loads target if exists in SharedPreferences`() {
        val existingTarget = 7
        `when`(mockSharedPreferences.contains(SetGoalViewModel.KEY_GOAL_TARGET_COUNT)).thenReturn(true)
        `when`(mockSharedPreferences.getInt(SetGoalViewModel.KEY_GOAL_TARGET_COUNT, 0)).thenReturn(existingTarget)

        // Re-initialize ViewModel or call a specific load method if init isn't sufficient
        // The current ViewModel calls loadGoalTargetForEditing() in init.
        val viewModelForTest = SetGoalViewModel(mockApplication) // Trigger init

        assertEquals(existingTarget, viewModelForTest.currentGoalTarget.value)
    }

    @Test
    fun `currentGoalTarget LiveData is null if no target in SharedPreferences`() {
        `when`(mockSharedPreferences.contains(SetGoalViewModel.KEY_GOAL_TARGET_COUNT)).thenReturn(false)
        
        val viewModelForTest = SetGoalViewModel(mockApplication) // Trigger init

        assertNull(viewModelForTest.currentGoalTarget.value)
    }
    
    @Test
    fun `getGoalTarget returns value when goal is set`() {
        val target = 10
        `when`(mockSharedPreferences.contains(SetGoalViewModel.KEY_GOAL_TARGET_COUNT)).thenReturn(true)
        `when`(mockSharedPreferences.getInt(SetGoalViewModel.KEY_GOAL_TARGET_COUNT, 0)).thenReturn(target)

        assertEquals(target, setGoalViewModel.getGoalTarget())
    }

    @Test
    fun `getGoalTarget returns null when no goal is set`() {
        `when`(mockSharedPreferences.contains(SetGoalViewModel.KEY_GOAL_TARGET_COUNT)).thenReturn(false)
        
        assertNull(setGoalViewModel.getGoalTarget())
    }

    @Test
    fun `getGoalStartTimestamp returns value when set`() {
        val timestamp = 123456789L
        `when`(mockSharedPreferences.getLong(SetGoalViewModel.KEY_GOAL_START_TIMESTAMP, 0L)).thenReturn(timestamp)
        assertEquals(timestamp, setGoalViewModel.getGoalStartTimestamp())
    }

    @Test
    fun `getGoalType returns value when set`() {
        val type = "test_type"
        `when`(mockSharedPreferences.getString(SetGoalViewModel.KEY_GOAL_TYPE, null)).thenReturn(type)
        assertEquals(type, setGoalViewModel.getGoalType())
    }

    @Test
    fun `isGoalSet returns true when keys exist`() {
        `when`(mockSharedPreferences.contains(SetGoalViewModel.KEY_GOAL_TARGET_COUNT)).thenReturn(true)
        `when`(mockSharedPreferences.contains(SetGoalViewModel.KEY_GOAL_START_TIMESTAMP)).thenReturn(true)
        assertTrue(setGoalViewModel.isGoalSet())
    }

    @Test
    fun `isGoalSet returns false if target key is missing`() {
        `when`(mockSharedPreferences.contains(SetGoalViewModel.KEY_GOAL_TARGET_COUNT)).thenReturn(false)
        `when`(mockSharedPreferences.contains(SetGoalViewModel.KEY_GOAL_START_TIMESTAMP)).thenReturn(true)
        assertFalse(setGoalViewModel.isGoalSet())
    }

     @Test
    fun `isGoalSet returns false if timestamp key is missing`() {
        `when`(mockSharedPreferences.contains(SetGoalViewModel.KEY_GOAL_TARGET_COUNT)).thenReturn(true)
        `when`(mockSharedPreferences.contains(SetGoalViewModel.KEY_GOAL_START_TIMESTAMP)).thenReturn(false)
        assertFalse(setGoalViewModel.isGoalSet())
    }
}
