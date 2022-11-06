package com.example.easycalendar

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.easycalendar.databinding.ActivityMainBinding
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.NaverIdLoginSDK.getAccessToken
import com.navercorp.nid.oauth.OAuthLoginCallback
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.buttonOAuthLoginImg.setOAuthLogin(launcher, oauthLoginCallback)

        NaverIdLoginSDK.initialize(
            applicationContext,
            getString(R.string.naver_client_id),
            getString(R.string.naver_client_secret),
            getString(R.string.naver_client_name)
        )

        val today = Calendar.getInstance()
        var selectedDateTime =
            today.get(Calendar.YEAR).toString() +
                    (today.get(Calendar.MONTH) + 1).toString() +
                    today.get(Calendar.DAY_OF_MONTH).toString()
        binding.datePicker.init(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)){
            view, year, month, day ->
            val month = month + 1
            val day = if (day < 10) "0$day" else day
            selectedDateTime = "$year$month$day"
        }

        binding.SaveButton.setOnClickListener {
            var title = if (binding.ScheduleTitleInput.text.isNotEmpty()) binding.ScheduleTitleInput.text.toString() else ""
            val regex  = "^\\d+시.*".toRegex()
            val matchResult : MatchResult? = regex.matchEntire(title)
            if (matchResult != null) {
                val tempList = getTitleBreakdown(title, selectedDateTime)
                title = tempList[0]
                selectedDateTime = tempList[1]
            }
            Log.i("test", title)
            Log.i("test", selectedDateTime)
            Thread {
                saveSchedule(title, selectedDateTime)
            }.start()
            Toast.makeText(applicationContext, getString(R.string.SucessSave), Toast.LENGTH_SHORT).show()
        }
    }

    private fun getTitleBreakdown(title: String, selectedDateTime: String): Array<String> {
        //time
        var time = title.split("시")[0].trim().toInt()
        val half = 12
        time = if(time < half) time + half else time
        val newDateTime = selectedDateTime + "T%s0000".format(time.toString())
        //title
        val newTitle = title.split("시")[1].trim()
        return arrayOf(newTitle, newDateTime)
    }

    private fun saveSchedule(title: String, DateTime: String) {
        val token = getAccessToken()
        val header = "Bearer $token"
        Log.i("전달된 datetime", DateTime)
        try {
            val apiURL = "https://openapi.naver.com/calendar/createSchedule.json"
            val url = URL(apiURL)
            val con: HttpURLConnection = url.openConnection() as HttpURLConnection
            con.setRequestMethod("POST")
            con.setRequestProperty("Authorization", header)
            val title = URLEncoder.encode(title, "UTF-8")
            val uid: String = UUID.randomUUID().toString()
            val scheduleIcalString =
                """
BEGIN:VCALENDAR
VERSION:2.0
PRODID:Naver Calendar
CALSCALE:GREGORIAN
BEGIN:VTIMEZONE
TZID:Asia/Seoul
BEGIN:STANDARD
DTSTART:19700101T000000
TZNAME:GMT%2B09:00
TZOFFSETFROM:%2B0900
TZOFFSETTO:%2B0900
END:STANDARD
END:VTIMEZONE
BEGIN:VEVENT
SEQUENCE:0
CLASS:PUBLIC
TRANSP:OPAQUE
UID:$uid
DTSTART;TZID=Asia/Seoul:$DateTime
DTEND;TZID=Asia/Seoul:$DateTime
SUMMARY:$title
DESCRIPTION:
LOCATION:
ORGANIZER;CN=
ATTENDEE;ROLE=
CREATED:20140905T015408Z
LAST-MODIFIED:20140905T015408Z
DTSTAMP:20140905T015409Z
END:VEVENT
END:VCALENDAR
                                    """
            val postParams =
                "calendarId=defaultCalendarId&scheduleIcalString=$scheduleIcalString"
            con.doOutput = true
            val wr = DataOutputStream(con.outputStream)
            wr.writeBytes(postParams)
            wr.flush()
            wr.close()
            val responseCode: Int = con.responseCode
            val br: BufferedReader
            if (responseCode == 200) { // 정상 호출
                br = BufferedReader(InputStreamReader(con.inputStream))
                Log.i("호출", "정상호출")
            } else {  // 에러 발생
                br = BufferedReader(InputStreamReader(con.errorStream))
            }
            var inputLine: String?
            val response = StringBuffer()
            while (br.readLine().also { inputLine = it } != null) {
                response.append(inputLine)
            }
            br.close()
            Log.i("response", response.toString())
        } catch (e: Exception) {
            println(e)
        }
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                RESULT_OK -> {
                    Toast.makeText(applicationContext, getString(R.string.SuccessLogin), Toast.LENGTH_SHORT).show()
                }
                RESULT_CANCELED -> {
                    val errorCode = NaverIdLoginSDK.getLastErrorCode().code
                    val errorDescription = NaverIdLoginSDK.getLastErrorDescription()
                    Toast.makeText(
                        applicationContext,
                        "errorCode:$errorCode, errorDesc:$errorDescription",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    private val oauthLoginCallback = object : OAuthLoginCallback {
        override fun onSuccess() {
            Toast.makeText(applicationContext, getString(R.string.SuccessLogin), Toast.LENGTH_SHORT).show()
        }

        override fun onFailure(httpStatus: Int, message: String) {
            val errorCode = NaverIdLoginSDK.getLastErrorCode().code
            val errorDescription = NaverIdLoginSDK.getLastErrorDescription()
            Toast.makeText(
                applicationContext,
                "errorCode:$errorCode, errorDesc:$errorDescription",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onError(errorCode: Int, message: String) {
            onFailure(errorCode, message)
        }
    }
}
