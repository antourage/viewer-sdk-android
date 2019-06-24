package com.antourage.weaverlib

import android.content.Context
import android.content.res.Resources
import com.antourage.weaverlib.other.parseDate
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KClass

@RunWith(MockitoJUnitRunner::class)
class ExtentionsUnitTest {
    @Mock
    private lateinit var mockContext: Context
    @Mock
    private lateinit var mockContextResources: Resources

    @Test
    fun parseDataSecondTest() {
        val secNumber = 1
        `when`(mockContextResources
            .getQuantityString(R.plurals.seconds, secNumber, secNumber)).thenReturn("$secNumber s")
        `when`(mockContext.resources).thenReturn(mockContextResources)
        `when`(mockContext.getString(R.string.started_ago,mockContext.resources
            .getQuantityString(R.plurals.seconds, secNumber, secNumber)))
            .thenReturn("$secNumber s ago")
        assertEquals(substractDays(0).parseDate(mockContext),"$secNumber s ago")
    }

    private fun substractDays(numberOfDays:Int,minusMin:Int=0):String{
        val cal = GregorianCalendar.getInstance()
        cal.time = Date()
        cal.add(Calendar.DAY_OF_YEAR, -numberOfDays)
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", Locale.ENGLISH)
        df.timeZone = TimeZone.getTimeZone("UTC")
        return df.format(Date(cal.time.time - minusMin))
    }

    @Test
    fun readStringFromContext_LocalizedString() {
        // Given a mocked Context injected into the object under test...
        `when`(mockContext.getString(R.string.set))
            .thenReturn("set")

        // ...then the result should be the expected one.
        assertEquals(mockContext.getString(R.string.set), "set")
    }
}