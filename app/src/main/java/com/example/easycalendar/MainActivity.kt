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

        val title = binding.ScheduleTitleInput.text.toString()
        val today = Calendar.getInstance()
        var selectedDateTime = ""
        binding.datePicker.init(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)){
            view, year, month, day ->
            val month = month + 1
            val day = if (day < 10) "0$day" else day
            selectedDateTime = "$year$month$day"
        }
        binding.SaveButton.setOnClickListener {
            Thread {
                Log.i("datetime", selectedDateTime)
                saveSchedule(title, selectedDateTime)
            }.start()
        }
    }

    private fun saveSchedule(title: String, DateTime: String) {
        val token = getAccessToken()
        val header = "Bearer $token"

        try {
            val apiURL = "https://openapi.naver.com/calendar/createSchedule.json"
            val url = URL(apiURL)
            val con: HttpURLConnection = url.openConnection() as HttpURLConnection
            con.setRequestMethod("POST")
            con.setRequestProperty("Authorization", header)
            val title = URLEncoder.encode(title, "UTF-8")
            val dateTime = DateTime+"T170000"
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
            println(postParams)
            con.setDoOutput(true)
            val wr = DataOutputStream(con.getOutputStream())
            wr.writeBytes(postParams)
            wr.flush()
            wr.close()
            val responseCode: Int = con.getResponseCode()
            val br: BufferedReader
            if (responseCode == 200) { // 정상 호출
                br = BufferedReader(InputStreamReader(con.getInputStream()))
                Log.i("호출", "정상호출")
            } else {  // 에러 발생
                br = BufferedReader(InputStreamReader(con.getErrorStream()))
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
                    Toast.makeText(applicationContext, "로그인 성공", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(applicationContext, "로그인 성공", Toast.LENGTH_SHORT).show()
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
