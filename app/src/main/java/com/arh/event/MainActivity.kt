package com.arh.event

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arh.event.data.DatabaseHelper
import com.arh.event.utils.NotificationsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import com.arh.event.model.Event
import java.nio.charset.Charset

class MainActivity : AppCompatActivity() {

    companion object {
        var isActive = false
    }

    override fun onResume() {
        super.onResume()
        isActive = true
    }

    override fun onPause() {
        super.onPause()
        isActive = false
    }

    private lateinit var iconHome: ImageView
    private lateinit var iconSettings: ImageView
    private lateinit var dbHelper: DatabaseHelper

    private val LOCATION_PERMISSION_REQUEST_CODE = 101
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 102
    private val homeFragment = HomeFragment()
    private val settingsFragment = SettingsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)

        iconHome = findViewById(R.id.iconHome)
        iconSettings = findViewById(R.id.iconSettings)

        if (savedInstanceState == null) {
            replaceFragment(homeFragment)
            highlightHome()
        }

        iconHome.setOnClickListener {
            replaceFragment(homeFragment)
            highlightHome()
        }

        iconSettings.setOnClickListener {
            replaceFragment(settingsFragment)
            highlightSettings()
        }

        checkPermissionsSequentially()
        NotificationsHelper.createChannel(this)
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun highlightHome() {
        iconHome.setColorFilter(ContextCompat.getColor(this, R.color.blue))
        iconSettings.setColorFilter(ContextCompat.getColor(this, android.R.color.white))
    }

    private fun highlightSettings() {
        iconSettings.setColorFilter(ContextCompat.getColor(this, R.color.blue))
        iconHome.setColorFilter(ContextCompat.getColor(this, android.R.color.white))
    }

    private fun checkPermissionsSequentially() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            } else {
                checkNotificationPermission()
            }
        } else {
            checkNotificationPermission()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Разрешение на геолокацию получено", Toast.LENGTH_SHORT).show()
                    Log.d("ArhEvent", "Разрешение на геолокацию получено")
                    collectOpenDataSources()
                } else {
                    Toast.makeText(this, "Разрешение на геолокацию не предоставлено", Toast.LENGTH_SHORT).show()
                    Log.d("ArhEvent", "Разрешение на геолокацию не предоставлено")
                }
                checkNotificationPermission()
            }
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Разрешение на уведомления получено", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Разрешение на уведомления не предоставлено", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun collectOpenDataSources() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val emergencyKeywords = listOf(
                    "авария","пожар","взрыв","дтп","обрушение","утечка","короткое замыкание",
                    "загорелось","тушат","эвакуировали","разрушилось","обрушивают",
                    "задымление","прорыв","повреждение","порыв","замыкание","течь","разлив"
                )

                val planKeywords = listOf(
                    "плановые работы","отключение","проверка","профилактика","ремонт","учения",
                    "отключат","проверят","ремонтируют","реконструируют","обследуют","тестируют",
                    "обновят","заменят","инспектируют","обслужат","контролируют","осмотрят","технический","тестирование"
                )

                val locationsList = listOf(
                    "Архангельск", "Коряжма", "Котлас", "Мирный", "Новодвинск", "Онега", "Северодвинск",
                    "Вельский район", "Вельск",
                    "Верхнетоемский район", "Верхняя Тойма",
                    "Вилегодский район", "Ильинско-Подомское",
                    "Виноградовский район", "Березник",
                    "Каргопольский район", "Каргополь",
                    "Коношский район", "Коноша",
                    "Котласский район", "Котлас",
                    "Красноборский район", "Красноборск",
                    "Ленский район", "Яренск",
                    "Лешуконский район", "Лешуконское",
                    "Мезенский район", "Мезень",
                    "Новая Земля", "Белушья Губа",
                    "Няндомский район", "Няндома",
                    "Онежский район", "Онега",
                    "Пинежский район", "Карпогоры",
                    "Плесецкий район", "Плесецк",
                    "Приморский район", "Архангельск",
                    "Соловецкий район", "Соловецкий",
                    "Устьянский район", "Октябрьский",
                    "Холмогорский район", "Холмогоры",
                    "Шенкурский район", "Шенкурск"
                )

                val Day = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -30) }.time

                suspend fun processHtml(html: String, source: String, charset: Charset = Charsets.UTF_8) {
                    val titles: List<String>
                    val datesRaw: List<String>

                    val outputFormat = SimpleDateFormat("d MMMM yyyy ',' HH:mm", Locale("ru"))
                    val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val sdfDateFull = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
                    val calendar = Calendar.getInstance()

                    when (source) {
                        "telegram" -> {
                            val postRegex = "<div class=\"tgme_widget_message_wrap.*?\">.*?<div class=\"tgme_widget_message_text js-message_text\".*?>(.*?)</div>.*?<time datetime=\"(.*?)\"".toRegex(RegexOption.DOT_MATCHES_ALL)
                            val posts = postRegex.findAll(html).map {
                                val text = it.groupValues[1].replace("<br>", "\n").replace(Regex("<.*?>"), "").trim()
                                val datetime = it.groupValues[2].trim()
                                text to datetime
                            }.toList()
                            titles = posts.map { it.first }
                            datesRaw = posts.map { it.second }
                        }
                        "news29" -> {
                            val newsBlockRegex = "<div class=\"dataContainer\".*?>.*?<div class=\"title\"><a.*?>(.*?)</a></div>.*?<div class=\"date\".*?>(.*?)</div>".toRegex(RegexOption.DOT_MATCHES_ALL)
                            val blocks = newsBlockRegex.findAll(html).map {
                                val title = it.groupValues[1].trim()
                                val rawDate = it.groupValues[2].trim()
                                title to rawDate
                            }.toList()
                            titles = blocks.map { it.first }
                            datesRaw = blocks.map { it.second }
                        }
                        else -> return
                    }

                    for (i in titles.indices.reversed()) {
                        val title = titles[i]
                        val cursorCheck = dbHelper.getAllEvents()
                        var exists = false
                        cursorCheck.use { c ->
                            while (c.moveToNext()) {
                                if (c.getString(c.getColumnIndexOrThrow("title")) == title) {
                                    exists = true
                                    break
                                }
                            }
                        }
                        if (exists) continue

                        val lower = title.lowercase()
                        val rawDate = datesRaw.getOrNull(i) ?: continue

                        val eventDate = try {
                            when (source) {
                                "telegram" -> SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).parse(rawDate)
                                "news29" -> {
                                    calendar.time = Date()
                                    when {
                                        rawDate.contains("сегодня") -> {
                                            val timePart = rawDate.replace("сегодня", "").trim()
                                            val time = sdfTime.parse(timePart)!!
                                            calendar.set(Calendar.HOUR_OF_DAY, time.hours)
                                            calendar.set(Calendar.MINUTE, time.minutes)
                                            calendar.time
                                        }
                                        rawDate.contains("вчера") -> {
                                            val timePart = rawDate.replace("вчера", "").trim()
                                            val time = sdfTime.parse(timePart)!!
                                            calendar.add(Calendar.DAY_OF_MONTH, -1)
                                            calendar.set(Calendar.HOUR_OF_DAY, time.hours)
                                            calendar.set(Calendar.MINUTE, time.minutes)
                                            calendar.time
                                        }
                                        else -> sdfDateFull.parse(rawDate)
                                    }
                                }
                                else -> continue
                            }
                        } catch (_: Exception) { continue }

                        if (eventDate.before(Day)) continue

                        val category = when {
                            emergencyKeywords.any { it in lower } -> "Аварийные"
                            planKeywords.any { it in lower } -> "Плановые"
                            else -> "Прочее"
                        }
                        if (category == "Прочее") continue

                        val location = locationsList.firstOrNull { it.lowercase() in lower } ?: "Архангельская область"
                        val formattedDate = outputFormat.format(eventDate)

                        dbHelper.insertEvent(
                            title = title,
                            description = "Сообщение из $source",
                            category = category,
                            location = location,
                            date = formattedDate
                        )

                        dbHelper.getUserSettings().use { cursor ->
                            if (cursor.moveToFirst()) {
                                val notificationsEnabled = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTIFICATIONS_ENABLED)) == 1
                                val notifyEmergency = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTIFY_EMERGENCY)) == 1
                                val notifyPlan = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTIFY_PLAN)) == 1
                                val priority = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PRIORITY_LEVEL))

                                val shouldNotifyCategory = (category == "Аварийные" && notifyEmergency) ||
                                        (category == "Плановые" && notifyPlan)

                                if (notificationsEnabled && shouldNotifyCategory && !MainActivity.isActive) {
                                    NotificationsHelper.sendNotification(
                                        this@MainActivity,
                                        title,
                                        priority
                                    )
                                }
                            }
                        }

                        runOnUiThread { homeFragment.loadEvents() }

                        val frequencyMillis = dbHelper.getUserSettings().use {
                            val freq = if (it.moveToFirst()) it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COL_FREQUENCY)) else "Каждые 5 мин"
                            when (freq) {
                                "Каждые 5 мин" -> 5 * 100 * 10L
                                "Раз в час" -> 60 * 60 * 1000L
                                "Раз в день" -> 24 * 60 * 60 * 1000L
                                "Раз в неделю" -> 7 * 24 * 60 * 60 * 1000L
                                else -> 5 * 60 * 1000L
                            }
                        }
                        kotlinx.coroutines.delay(frequencyMillis)
                    }
                }

                val newsHtml = URL("https://www.news29.ru/novosti/proishestvija/").openStream().bufferedReader(Charset.forName("windows-1251")).use { it.readText() }
                processHtml(newsHtml, "news29", Charset.forName("windows-1251"))

                val telegramHtml = URL("https://t.me/s/zhest_29").readText()
                processHtml(telegramHtml, "telegram")

            } catch (e: Exception) {
                Log.e("CollectData", "Global error: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Ошибка при сборе данных: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    class EventAdapter(private val events: List<Event>) :
        RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

        class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvTitle: TextView = itemView.findViewById(R.id.textTitle)
            val tvDesc: TextView = itemView.findViewById(R.id.textDesc)
            val tvCategory: TextView = itemView.findViewById(R.id.textCategory)
            val tvTime: TextView = itemView.findViewById(R.id.textTime)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.event_card, parent, false)
            return EventViewHolder(view)
        }

        override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
            val event = events[position]
            holder.tvTitle.text = event.title
            holder.tvDesc.text = event.description ?: ""
            holder.tvCategory.text = event.category
            holder.tvTime.text = event.startTime
        }

        override fun getItemCount() = events.size
    }

    class HomeFragment : Fragment() {

        private lateinit var dbHelper: DatabaseHelper
        private lateinit var recyclerEvents: RecyclerView
        private val eventList = mutableListOf<Event>()
        private lateinit var adapter: EventAdapter

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            val view = inflater.inflate(R.layout.fragment_home, container, false)

            dbHelper = DatabaseHelper(requireContext())
            recyclerEvents = view.findViewById(R.id.recyclerEvents)
            recyclerEvents.layoutManager = LinearLayoutManager(requireContext())
            adapter = EventAdapter(eventList)
            recyclerEvents.adapter = adapter

            loadEvents()
            return view
        }

        fun loadEvents() {
            eventList.clear()
            dbHelper.getUserSettings().use { settingsCursor ->
                val notifyEmergency = if (settingsCursor.moveToFirst())
                    settingsCursor.getInt(settingsCursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTIFY_EMERGENCY)) == 1
                else true
                val notifyPlan = if (settingsCursor.moveToFirst())
                    settingsCursor.getInt(settingsCursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTIFY_PLAN)) == 1
                else true

                dbHelper.getAllEvents().use { cursor ->
                    val colTitle = cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TITLE)
                    val colCategory = cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CATEGORY)
                    val colLocation = cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOCATION)
                    val colDescription = cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DESCRIPTION)
                    val colStartTime = cursor.getColumnIndexOrThrow(DatabaseHelper.COL_START_TIME)

                    while (cursor.moveToNext()) {
                        val category = cursor.getString(colCategory)
                        if ((category == "Аварийные" && !notifyEmergency) ||
                            (category == "Плановые" && !notifyPlan)
                        ) continue

                        val title = cursor.getString(colTitle)
                        val location = cursor.getString(colLocation)
                        val description = cursor.getString(colDescription)
                        val startTime = cursor.getString(colStartTime)

                        eventList.add(Event(title, category, location, description, startTime))
                    }
                }
            }
            adapter.notifyDataSetChanged()
        }
    }

    class SettingsFragment : Fragment() {

        private lateinit var dbHelper: DatabaseHelper

        private lateinit var switchNotifications: Switch
        private lateinit var checkEmergency: CheckBox
        private lateinit var checkPlanned: CheckBox
        private lateinit var spinnerFrequency: Spinner
        private lateinit var seekPriority: SeekBar
        private lateinit var priorityLabel: TextView

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            val view = inflater.inflate(R.layout.fragment_settings, container, false)

            dbHelper = DatabaseHelper(requireContext())

            switchNotifications = view.findViewById(R.id.switchNotifications)
            checkEmergency = view.findViewById(R.id.checkUtilities)
            checkPlanned = view.findViewById(R.id.checkWeather)
            spinnerFrequency = view.findViewById(R.id.spinnerFrequency)
            seekPriority = view.findViewById(R.id.seekPriority)
            priorityLabel = view.findViewById(R.id.priorityLabel)

            loadSettings()
            setupListeners()

            return view
        }

        private fun loadSettings() {
            dbHelper.getUserSettings().use { cursor ->
                if (cursor.moveToFirst()) {
                    switchNotifications.isChecked = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTIFICATIONS_ENABLED)) == 1
                    checkPlanned.isChecked = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTIFY_PLAN)) == 1
                    checkEmergency.isChecked = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTIFY_EMERGENCY)) == 1

                    val frequency = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FREQUENCY))
                    val priority = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PRIORITY_LEVEL))

                    spinnerFrequency.setSelection((spinnerFrequency.adapter as ArrayAdapter<String>).getPosition(frequency))
                    seekPriority.progress = priority
                    priorityLabel.text = "Приоритет: $priority"
                }
            }
        }

        private fun setupListeners() {
            switchNotifications.setOnCheckedChangeListener { _, isChecked ->
                setNotificationsEnabled(isChecked)
            }

            checkPlanned.setOnCheckedChangeListener { _, _ -> saveSettings() }
            checkEmergency.setOnCheckedChangeListener { _, _ -> saveSettings() }

            spinnerFrequency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = saveSettings()
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            seekPriority.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    priorityLabel.text = "Приоритет: $progress"
                    saveSettings()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        private fun setNotificationsEnabled(enabled: Boolean) {
            dbHelper.writableDatabase.use { db ->
                val values = ContentValues().apply {
                    put(DatabaseHelper.COL_NOTIFICATIONS_ENABLED, if (enabled) 1 else 0)
                }
                db.update("user_settings", values, null, null)
            }
        }

        private fun saveSettings() {
            dbHelper.writableDatabase.use { db ->
                val values = ContentValues().apply {
                    put(DatabaseHelper.COL_NOTIFY_PLAN, if (checkPlanned.isChecked) 1 else 0)
                    put(DatabaseHelper.COL_NOTIFY_EMERGENCY, if (checkEmergency.isChecked) 1 else 0)
                    put(DatabaseHelper.COL_FREQUENCY, spinnerFrequency.selectedItem.toString())
                    put(DatabaseHelper.COL_PRIORITY_LEVEL, seekPriority.progress)
                }
                db.update("user_settings", values, null, null)
            }
        }
    }
}
