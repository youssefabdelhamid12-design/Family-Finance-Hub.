package com.example.familyfinancehub

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.math.pow
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {

    private val green = Color.rgb(25, 116, 78)
    private val darkGreen = Color.rgb(13, 67, 51)
    private val mint = Color.rgb(232, 245, 235)
    private val red = Color.rgb(224, 83, 89)
    private val pink = Color.rgb(253, 235, 237)
    private val blue = Color.rgb(44, 151, 210)
    private val lightBlue = Color.rgb(229, 244, 253)
    private val purple = Color.rgb(121, 95, 191)
    private val lavender = Color.rgb(239, 233, 252)
    private val gold = Color.rgb(235, 166, 37)
    private val yellow = Color.rgb(255, 244, 213)
    private val cream = Color.rgb(250, 249, 245)
    private val white = Color.WHITE
    private val ink = Color.rgb(17, 51, 47)
    private val gray = Color.rgb(105, 125, 128)
    private val pale = Color.rgb(239, 243, 244)
    private val border = Color.rgb(229, 235, 230)

    private var french = true
    private var page = 0
    private lateinit var body: LinearLayout

    private val handler = Handler(Looper.getMainLooper())

    private val prefs by lazy {
        getSharedPreferences("family_finance_v21", Context.MODE_PRIVATE)
    }

    private data class Transaction(
        val description: String,
        val amount: Double,
        val category: String,
        val date: Long,
    )

    private data class Envelope(
        val name: String,
        val limit: Double,
        val spent: Double,
    )

    private data class Goal(
        val name: String,
        val target: Double,
        val saved: Double,
    )

    private data class Bill(
        val name: String,
        val amount: Double,
        val due: String,
        val status: String,
    )

    private data class LoanRow(
        val number: Int,
        val year: Int,
        val payment: Double,
        val interest: Double,
        val principal: Double,
        val balance: Double,
    )

    private data class LoanResult(
        val payment: Double,
        val interest: Double,
        val frequency: Int,
        val rows: List<LoanRow>,
    )

    private val quotesFr =
        listOf(
            "La richesse d'une famille grandit avec les projets qu'elle partage.",
            "La liberté financière commence par de petites décisions réfléchies.",
            "Épargner aujourd'hui, c'est offrir plus de choix à demain.",
            "Une famille prospère avance ensemble, à son propre rythme.",
            "Ton argent est un outil pour construire la vie que tu souhaites.",
            "La vraie réussite financière laisse de la place à la tranquillité.",
            "Chaque objectif atteint commence par une première contribution.",
            "La richesse personnelle, c'est aussi la liberté de choisir.",
            "Un avenir familial solide se construit une décision à la fois.",
            "Le meilleur budget est celui qui respecte les besoins de chacun.",
            "Grandir financièrement ne signifie pas tout sacrifier aujourd'hui.",
            "Les projets partagés transforment les économies en souvenirs.",
            "La sécurité financière donne de l'espace aux rêves.",
            "Chaque dollar bien orienté rapproche un objectif.",
            "La prospérité familiale commence par la confiance et la coopération.",
        )

    private val quotesEn =
        listOf(
            "A family's wealth grows through the dreams it builds together.",
            "Financial freedom begins with small, thoughtful decisions.",
            "Saving today creates more choices tomorrow.",
            "A thriving family moves forward at its own pace.",
            "Your money is a tool for building the life you want.",
            "Financial success also means peace of mind.",
            "Every achieved goal begins with a first contribution.",
            "Personal wealth also means having the freedom to choose.",
            "A strong family future is built one decision at a time.",
            "The best budget respects everyone's needs.",
            "Financial growth doesn't mean sacrificing everything today.",
            "Shared dreams turn savings into memories.",
            "Financial security creates room for dreams.",
            "Every dollar with a purpose brings a goal closer.",
            "Family prosperity begins with trust and cooperation.",
        )

    private fun t(fr: String, en: String): String {
        return if (french) fr else en
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun money(value: Double): String {
        return NumberFormat.getCurrencyInstance(if (french) Locale.CANADA_FRENCH else Locale.CANADA)
            .format(value)
    }

    private fun fmt(value: Double): String {
        return String.format(Locale.CANADA, "%.2f", value)
    }

    private fun get(key: String, default: Double = 0.0): Double {
        return prefs.getString(key, null)?.toDoubleOrNull() ?: default
    }

    private fun put(key: String, value: Double) {
        prefs.edit().putString(key, value.toString()).apply()
    }

    private fun toast(value: String) {
        Toast.makeText(this, value, Toast.LENGTH_LONG).show()
    }

    private fun rounded(
        color: Int,
        radius: Int = 18,
        stroke: Int = Color.TRANSPARENT,
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != Color.TRANSPARENT) {
                setStroke(dp(1), stroke)
            }
        }
    }

    private fun gradient(
        first: Int,
        second: Int,
        radius: Int = 18,
    ): GradientDrawable {
        return GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(first, second),
            )
            .apply {
                cornerRadius = dp(radius).toFloat()
            }
    }

    private fun txt(
        value: String,
        size: Float = 14f,
        color: Int = ink,
        bold: Boolean = false,
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(4))
        }
    }

    private fun column(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
    }

    private fun row(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
    }

    private fun card(
        parent: LinearLayout = body,
        color: Int = white,
    ): LinearLayout {
        val box =
            column().apply {
                setPadding(dp(15), dp(15), dp(15), dp(15))
                background = rounded(color, 18, border)
            }

        parent.addView(
            box,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(12)
            },
        )

        return box
    }

    private fun button(
        label: String,
        color: Int = green,
        action: () -> Unit,
    ): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 13f
            setTextColor(white)
            background = rounded(color, 13)
            setOnClickListener { action() }
        }
    }

    private fun secondary(
        label: String,
        action: () -> Unit,
    ): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 13f
            setTextColor(darkGreen)
            background = rounded(mint, 13)
            setOnClickListener { action() }
        }
    }

    private fun field(
        parent: LinearLayout,
        label: String,
        initial: String = "",
        numeric: Boolean = false,
    ): EditText {
        parent.addView(txt(label, 13f, ink, true))

        val input =
            EditText(this).apply {
                setText(initial)
                textSize = 15f
                setSingleLine(true)

                if (numeric) {
                    inputType =
                        InputType.TYPE_CLASS_NUMBER or
                            InputType.TYPE_NUMBER_FLAG_DECIMAL or
                            InputType.TYPE_NUMBER_FLAG_SIGNED
                }
            }

        parent.addView(input)
        return input
    }

    private fun read(input: EditText): Double {
        val number = input.text.toString().trim().replace(" ", "").replace(",", ".").toDouble()

        require(number.isFinite())
        return number
    }

    private fun heading(
        parent: LinearLayout,
        title: String,
        link: String? = null,
        action: (() -> Unit)? = null,
    ) {
        val line = row()

        line.addView(
            txt(title, 17f, ink, true),
            LinearLayout.LayoutParams(0, -2, 1f),
        )

        if (link != null && action != null) {
            val label = txt(link, 12f, green, true)
            label.setPadding(dp(8), dp(5), dp(2), dp(5))
            label.setOnClickListener { action() }
            line.addView(label)
        }

        parent.addView(line)
    }

    private fun stat(
        parent: LinearLayout,
        label: String,
        value: String,
        color: Int = green,
    ) {
        parent.addView(txt(label, 13f, gray))
        parent.addView(txt(value, 22f, color, true))
    }

    private fun divider(parent: LinearLayout) {
        val line = View(this)
        line.setBackgroundColor(border)

        parent.addView(
            line,
            LinearLayout.LayoutParams(-1, dp(1)).apply {
                topMargin = dp(9)
                bottomMargin = dp(9)
            },
        )
    }

    private fun progress(
        parent: LinearLayout,
        current: Double,
        maximum: Double,
        color: Int = green,
    ) {
        val percentage =
            if (maximum > 0) {
                (current / maximum * 100.0).coerceIn(0.0, 100.0).toInt()
            } else {
                0
            }

        val bar =
            ProgressBar(
                    this,
                    null,
                    android.R.attr.progressBarStyleHorizontal,
                )
                .apply {
                    max = 100
                    this.progress = percentage
                    progressTintList = ColorStateList.valueOf(color)
                    progressBackgroundTintList = ColorStateList.valueOf(pale)
                }

        parent.addView(
            bar,
            LinearLayout.LayoutParams(-1, dp(9)).apply {
                topMargin = dp(7)
                bottomMargin = dp(7)
            },
        )
    }

    private fun percentage(a: Double, b: Double): Int {
        return if (b > 0) {
            (a / b * 100).coerceIn(0.0, 100.0).toInt()
        } else {
            0
        }
    }

    private fun currentDate(): String {
        return SimpleDateFormat(
                "EEE d MMM yyyy",
                if (french) Locale.CANADA_FRENCH else Locale.CANADA,
            )
            .format(Date())
    }

    private fun go(target: Int) {
        page = target
        build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = cream
        window.navigationBarColor = cream

        french = prefs.getBoolean("fr", true)
        page = savedInstanceState?.getInt("page") ?: 0

        if (savedInstanceState == null) {
            showSplash()
        } else {
            build()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("page", page)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (page != 0) {
            go(0)
        } else {
            super.onBackPressed()
        }
    }

    private fun showSplash() {
        val screen =
            column().apply {
                gravity = Gravity.CENTER
                setPadding(dp(26), dp(30), dp(26), dp(30))
                setBackgroundColor(cream)
            }

        val logo =
            txt("🏡", 76f, green).apply {
                gravity = Gravity.CENTER
            }

        val title =
            txt(
                    "Family Finance Hub",
                    28f,
                    darkGreen,
                    true,
                )
                .apply {
                    gravity = Gravity.CENTER
                }

        val subtitle =
            txt(
                    t(
                        "Votre richesse. Votre famille. Votre avenir.",
                        "Your wealth. Your family. Your future.",
                    ),
                    15f,
                    green,
                )
                .apply {
                    gravity = Gravity.CENTER
                }

        val previous = prefs.getInt("lastQuote", -1)

        val next =
            if (previous < 0) {
                quotesFr.indices.random()
            } else {
                (previous + 1) % quotesFr.size
            }

        prefs.edit().putInt("lastQuote", next).apply()

        val quote =
            txt(
                    "“${if (french) quotesFr[next] else quotesEn[next]}”",
                    19f,
                    ink,
                )
                .apply {
                    gravity = Gravity.CENTER
                    setPadding(0, dp(44), 0, dp(36))
                    setLineSpacing(dp(5).toFloat(), 1f)
                }

        val bar =
            ProgressBar(
                    this,
                    null,
                    android.R.attr.progressBarStyleHorizontal,
                )
                .apply {
                    max = 100
                    progress = 0
                    progressTintList = ColorStateList.valueOf(green)
                }

        val loading =
            txt(
                    t(
                        "Préparation de votre espace financier...",
                        "Preparing your financial space...",
                    ),
                    12f,
                    green,
                )
                .apply {
                    gravity = Gravity.CENTER
                }

        screen.addView(logo)
        screen.addView(title)
        screen.addView(subtitle)
        screen.addView(quote)

        screen.addView(
            bar,
            LinearLayout.LayoutParams(dp(220), dp(8)),
        )

        screen.addView(loading)

        setContentView(screen)

        logo.startAnimation(
            AlphaAnimation(0f, 1f).apply {
                duration = 1100
                fillAfter = true
            }
        )

        title.startAnimation(
            AlphaAnimation(0f, 1f).apply {
                duration = 1400
                startOffset = 200
                fillAfter = true
            }
        )

        quote.startAnimation(
            AlphaAnimation(0f, 1f).apply {
                duration = 1500
                startOffset = 500
                fillAfter = true
            }
        )

        val start = SystemClock.uptimeMillis()
        val duration = 3000L

        val updater =
            object : Runnable {
                override fun run() {
                    if (isFinishing || isDestroyed) return

                    val elapsed = SystemClock.uptimeMillis() - start

                    bar.progress = (elapsed * 100 / duration).coerceIn(0, 100).toInt()

                    if (elapsed < duration) {
                        handler.postDelayed(this, 30)
                    } else {
                        build()
                    }
                }
            }

        handler.post(updater)
    }

    private fun build() {
        val outer =
            column().apply {
                setBackgroundColor(cream)
            }

        val header =
            row().apply {
                setPadding(dp(14), dp(10), dp(10), dp(10))
                setBackgroundColor(cream)
            }

        val logo =
            txt("⌂", 30f, white, true).apply {
                gravity = Gravity.CENTER
                background = rounded(green, 50)
            }

        header.addView(
            logo,
            LinearLayout.LayoutParams(dp(50), dp(50)),
        )

        val titles =
            column().apply {
                setPadding(dp(10), 0, 0, 0)
            }

        titles.addView(txt("Family Finance Hub", 19f, darkGreen, true))

        titles.addView(
            txt(
                t(
                    "Ensemble vers un avenir meilleur ♡",
                    "Together toward a better future ♡",
                ),
                11f,
                green,
            )
        )

        header.addView(
            titles,
            LinearLayout.LayoutParams(0, -2, 1f),
        )

        val language =
            txt(
                    if (french) "FR" else "EN",
                    13f,
                    darkGreen,
                    true,
                )
                .apply {
                    gravity = Gravity.CENTER
                    background = rounded(white, 20, border)
                    setPadding(dp(9), dp(7), dp(9), dp(7))
                }

        language.setOnClickListener {
            french = !french
            prefs.edit().putBoolean("fr", french).apply()
            build()
        }

        header.addView(language)

        val menuButton =
            txt("⋮", 32f, darkGreen, true).apply {
                gravity = Gravity.CENTER
                contentDescription = t("Menu principal", "Main menu")
            }

        header.addView(
            menuButton,
            LinearLayout.LayoutParams(dp(44), dp(48)),
        )

        menuButton.setOnClickListener {
            showMenu(menuButton)
        }

        outer.addView(header)

        val scroll =
            ScrollView(this).apply {
                isFillViewport = true
                clipToPadding = false
            }

        body =
            column().apply {
                setPadding(dp(12), dp(8), dp(12), dp(25))
            }

        scroll.addView(body)

        outer.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f),
        )

        setContentView(outer)

        when (page) {
            0 -> dashboard()
            1 -> budgetPage()
            2 -> envelopesPage()
            3 -> goalsPage()
            4 -> transactionsPage()
            5 -> billsPage()
            6 -> familyPage()
            7 -> questionnairePage()
            8 -> assistantPage()
            9 -> educationPage()
            10 -> calculatorsPage()
            11 -> wealthPage()
            12 -> settingsPage()
            else -> dashboard()
        }
    }

    private fun showMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)

        val names =
            if (french) {
                listOf(
                    "🏠 Tableau de bord",
                    "💰 Budget",
                    "👛 Enveloppes",
                    "🎯 Objectifs",
                    "⇄ Transactions",
                    "🧾 Factures",
                    "👨‍👩‍👧 Famille",
                    "📋 Questionnaire personnalisé",
                    "🤖 Assistant financier",
                    "📚 Éducation financière",
                    "🧮 Calculateurs",
                    "📈 Patrimoine",
                    "⚙ Paramètres",
                )
            } else {
                listOf(
                    "🏠 Dashboard",
                    "💰 Budget",
                    "👛 Envelopes",
                    "🎯 Goals",
                    "⇄ Transactions",
                    "🧾 Bills",
                    "👨‍👩‍👧 Family",
                    "📋 Personalized questionnaire",
                    "🤖 Financial assistant",
                    "📚 Financial education",
                    "🧮 Calculators",
                    "📈 Net worth",
                    "⚙ Settings",
                )
            }

        names.forEachIndexed { index, label ->
            popup.menu.add(0, index, index, label)
        }

        popup.setOnMenuItemClickListener {
            go(it.itemId)
            true
        }

        popup.show()
    }

    private fun readArray(key: String): JSONArray {
        return try {
            JSONArray(prefs.getString(key, "[]") ?: "[]")
        } catch (e: Exception) {
            JSONArray()
        }
    }

    private fun saveArray(key: String, array: JSONArray) {
        prefs.edit().putString(key, array.toString()).apply()
    }

    private fun transactions(): MutableList<Transaction> {
        val array = readArray("transactions_v2")
        val list = mutableListOf<Transaction>()

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue

            list.add(
                Transaction(
                    item.optString("description"),
                    item.optDouble("amount"),
                    item.optString("category"),
                    item.optLong("date"),
                )
            )
        }

        return list
    }

    private fun saveTransactions(list: List<Transaction>) {
        val array = JSONArray()

        list.forEach {
            array.put(
                JSONObject().apply {
                    put("description", it.description)
                    put("amount", it.amount)
                    put("category", it.category)
                    put("date", it.date)
                }
            )
        }

        saveArray("transactions_v2", array)
    }

    private fun envelopes(): MutableList<Envelope> {
        val array = readArray("envelopes_v2")

        if (array.length() == 0 && !prefs.getBoolean("envelopes_initialized", false)) {
            val defaults =
                mutableListOf(
                    Envelope("Épicerie", 500.0, 320.0),
                    Envelope("Transport", 300.0, 120.0),
                    Envelope("Loisirs", 200.0, 85.0),
                    Envelope("Sorties", 150.0, 60.0),
                )

            saveEnvelopes(defaults)

            prefs.edit().putBoolean("envelopes_initialized", true).apply()

            return defaults
        }

        val list = mutableListOf<Envelope>()

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue

            list.add(
                Envelope(
                    item.optString("name"),
                    item.optDouble("limit"),
                    item.optDouble("spent"),
                )
            )
        }

        return list
    }

    private fun saveEnvelopes(list: List<Envelope>) {
        val array = JSONArray()

        list.forEach {
            array.put(
                JSONObject().apply {
                    put("name", it.name)
                    put("limit", it.limit)
                    put("spent", it.spent)
                }
            )
        }

        saveArray("envelopes_v2", array)
    }

    private fun goals(): MutableList<Goal> {
        val array = readArray("goals_v2")

        if (array.length() == 0 && !prefs.getBoolean("goals_initialized", false)) {
            val defaults =
                mutableListOf(
                    Goal("Vacances en famille", 2000.0, 1200.0),
                    Goal("Nouvelle voiture", 5000.0, 2500.0),
                    Goal("Fonds d'urgence", 3000.0, 1000.0),
                )

            saveGoals(defaults)

            prefs.edit().putBoolean("goals_initialized", true).apply()

            return defaults
        }

        val list = mutableListOf<Goal>()

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue

            list.add(
                Goal(
                    item.optString("name"),
                    item.optDouble("target"),
                    item.optDouble("saved"),
                )
            )
        }

        return list
    }

    private fun saveGoals(list: List<Goal>) {
        val array = JSONArray()

        list.forEach {
            array.put(
                JSONObject().apply {
                    put("name", it.name)
                    put("target", it.target)
                    put("saved", it.saved)
                }
            )
        }

        saveArray("goals_v2", array)
    }

    private fun bills(): MutableList<Bill> {
        val array = readArray("bills_v2")

        if (array.length() == 0 && !prefs.getBoolean("bills_initialized", false)) {
            val defaults =
                mutableListOf(
                    Bill("Électricité", 120.0, "24 avr.", "late"),
                    Bill("Internet", 45.0, "28 avr.", "soon"),
                    Bill("Assurance auto", 82.0, "5 mai", "ok"),
                )

            saveBills(defaults)

            prefs.edit().putBoolean("bills_initialized", true).apply()

            return defaults
        }

        val list = mutableListOf<Bill>()

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue

            list.add(
                Bill(
                    item.optString("name"),
                    item.optDouble("amount"),
                    item.optString("due"),
                    item.optString("status"),
                )
            )
        }

        return list
    }

    private fun saveBills(list: List<Bill>) {
        val array = JSONArray()

        list.forEach {
            array.put(
                JSONObject().apply {
                    put("name", it.name)
                    put("amount", it.amount)
                    put("due", it.due)
                    put("status", it.status)
                }
            )
        }

        saveArray("bills_v2", array)
    }

    private fun dashboard() {
        val income = get("income", 4850.0)
        val expenses = get("expenses", 3120.0)
        val savings = get("savings", 2450.0)
        val balance = income - expenses
        val budgetLimit = get("budgetLimit", 4500.0)

        val greeting = card(body, mint)
        val welcome = row()

        welcome.addView(
            txt("☀️", 42f, gold),
            LinearLayout.LayoutParams(dp(66), -2),
        )

        val greetingText = column()

        greetingText.addView(txt(t("Bonjour !", "Hello!"), 26f, darkGreen, true))

        greetingText.addView(
            txt(
                t(
                    "Voici un aperçu de vos finances aujourd'hui.",
                    "Here is your financial overview today.",
                ),
                13f,
                gray,
            )
        )

        welcome.addView(
            greetingText,
            LinearLayout.LayoutParams(0, -2, 1f),
        )

        greeting.addView(welcome)

        greeting.addView(txt("📅 ${currentDate()}", 12f, green, true))

        val firstRow = row()

        statTile(
            firstRow,
            "💵",
            t("Revenus", "Income"),
            money(income),
            mint,
            green,
        )

        statTile(
            firstRow,
            "💳",
            t("Dépenses", "Expenses"),
            money(expenses),
            pink,
            red,
        )

        body.addView(firstRow)

        val secondRow = row()

        statTile(
            secondRow,
            "👛",
            t("Solde disponible", "Available balance"),
            money(balance),
            lightBlue,
            blue,
        )

        statTile(
            secondRow,
            "🐷",
            t("Épargne totale", "Total savings"),
            money(savings),
            lavender,
            purple,
        )

        body.addView(secondRow)

        val budget = card()

        heading(
            budget,
            t("🎯 Budget du mois", "🎯 Monthly budget"),
            t("Détails →", "Details →"),
        ) {
            go(1)
        }

        val amountRow = row()

        amountRow.addView(
            txt(money(expenses), 15f, ink, true),
            LinearLayout.LayoutParams(0, -2, 1f),
        )

        amountRow.addView(txt("/ ${money(budgetLimit)}", 13f, gray))

        budget.addView(amountRow)

        progress(budget, expenses, budgetLimit)

        budget.addView(
            txt(
                "${percentage(expenses, budgetLimit)} % " +
                    t("du budget utilisé", "of budget used"),
                12f,
                green,
            )
        )

        val actions = card()

        heading(
            actions,
            t("⚡ Actions rapides", "⚡ Quick actions"),
        )

        val actions1 = row()

        actionTile(
            actions1,
            "＋",
            t("Ajouter\nun revenu", "Add\nincome"),
            mint,
            green,
        ) {
            transactionDialog(true)
        }

        actionTile(
            actions1,
            "−",
            t("Ajouter\nune dépense", "Add\nexpense"),
            pink,
            red,
        ) {
            transactionDialog(false)
        }

        actions.addView(actions1)

        val actions2 = row()

        actionTile(
            actions2,
            "▤",
            t("Ajouter\nune facture", "Add\na bill"),
            lavender,
            purple,
        ) {
            billDialog()
        }

        actionTile(
            actions2,
            "◎",
            t("Nouvel\nobjectif", "New\ngoal"),
            yellow,
            gold,
        ) {
            goalDialog()
        }

        actions.addView(actions2)

        val envelopeCard = card()

        heading(
            envelopeCard,
            t("▣ Enveloppes", "▣ Envelopes"),
            t("Voir tout →", "View all →"),
        ) {
            go(2)
        }

        envelopes().take(4).forEach {
            envelopeSummary(envelopeCard, it)
        }

        val goalCard = card()

        heading(
            goalCard,
            t("◎ Objectifs d'épargne", "◎ Savings goals"),
            t("Voir tout →", "View all →"),
        ) {
            go(3)
        }

        goals().take(3).forEach {
            goalSummary(goalCard, it)
        }

        val billCard = card()

        heading(
            billCard,
            t("▤ Factures à payer", "▤ Bills to pay"),
            t("Voir tout →", "View all →"),
        ) {
            go(5)
        }

        bills().take(4).forEach {
            billSummary(billCard, it)
        }

        val transactionCard = card()

        heading(
            transactionCard,
            t("⇄ Transactions récentes", "⇄ Recent transactions"),
            t("Voir tout →", "View all →"),
        ) {
            go(4)
        }

        val recent = transactions().takeLast(4).reversed()

        if (recent.isEmpty()) {
            transactionCard.addView(
                txt(
                    t(
                        "Aucune transaction enregistrée.",
                        "No transactions recorded.",
                    ),
                    13f,
                    gray,
                )
            )
        }

        recent.forEach {
            transactionSummary(transactionCard, it)
        }

        val familyCard = card(body, mint)

        heading(
            familyCard,
            t("👨‍👩‍👧 Famille", "👨‍👩‍👧 Family"),
        )

        familyCard.addView(
            txt(
                "${get("members", 4.0).toInt()} " +
                    t("membres", "members") +
                    " • ${envelopes().size} " +
                    t("enveloppes", "envelopes") +
                    " • ${goals().size} " +
                    t("objectifs", "goals"),
                12f,
                green,
            )
        )

        familyCard.addView(
            button(t("Voir le détail", "View details")) {
                go(6)
            }
        )

        val assistant = card(body, lightBlue)

        heading(
            assistant,
            t("🤖 Assistant financier", "🤖 Financial assistant"),
        )

        assistant.addView(
            txt(
                t(
                    "Un questionnaire personnalisé et un assistant conversationnel pour explorer vos choix financiers.",
                    "A personalized questionnaire and conversational assistant to explore financial choices.",
                ),
                13f,
                gray,
            )
        )

        assistant.addView(
            button(t("Faire mon questionnaire", "Start questionnaire")) {
                go(7)
            }
        )

        assistant.addView(
            secondary(t("Discuter avec l'assistant", "Chat with assistant")) {
                go(8)
            }
        )

        val education = card()

        heading(
            education,
            t("📚 Éducation financière", "📚 Financial education"),
        )

        education.addView(
            txt(
                t(
                    "Six séances : planification, bilan, crédit, fiscalité, régimes enregistrés et placements.",
                    "Six sessions: planning, financial statements, credit, taxes, registered accounts and investing.",
                ),
                13f,
                gray,
            )
        )

        education.addView(
            button(t("Explorer les cours", "Explore lessons")) {
                go(9)
            }
        )
    }

    private fun statTile(
        parent: LinearLayout,
        icon: String,
        title: String,
        amount: String,
        tileColor: Int,
        accent: Int,
    ) {
        val tile =
            column().apply {
                background = gradient(tileColor, white, 19)
                setPadding(dp(12), dp(12), dp(10), dp(14))
            }

        val badge =
            txt(icon, 25f, white).apply {
                gravity = Gravity.CENTER
                background = rounded(accent, 50)
            }

        tile.addView(
            badge,
            LinearLayout.LayoutParams(dp(49), dp(49)),
        )

        tile.addView(txt(title, 12f, darkGreen, true))
        tile.addView(txt(amount, 21f, darkGreen, true))

        tile.addView(txt(t("Vue actuelle", "Current overview"), 11f, accent))

        parent.addView(
            tile,
            LinearLayout.LayoutParams(0, -2, 1f).apply {
                setMargins(dp(3), dp(3), dp(3), dp(9))
            },
        )
    }

    private fun actionTile(
        parent: LinearLayout,
        icon: String,
        title: String,
        tileColor: Int,
        accent: Int,
        action: () -> Unit,
    ) {
        val tile =
            column().apply {
                gravity = Gravity.CENTER
                background = rounded(tileColor, 17)
                setPadding(dp(10), dp(12), dp(10), dp(12))
            }

        val badge =
            txt(icon, 27f, white, true).apply {
                gravity = Gravity.CENTER
                background = rounded(accent, 50)
            }

        tile.addView(
            badge,
            LinearLayout.LayoutParams(dp(50), dp(50)),
        )

        tile.addView(
            txt(title, 13f, darkGreen, true).apply {
                gravity = Gravity.CENTER
            }
        )

        tile.setOnClickListener { action() }

        parent.addView(
            tile,
            LinearLayout.LayoutParams(0, dp(135), 1f).apply {
                setMargins(dp(3), dp(5), dp(3), dp(5))
            },
        )
    }

    private fun envelopeSummary(
        parent: LinearLayout,
        envelope: Envelope,
    ) {
        val line = row()

        line.addView(
            txt("🛒", 21f, green),
            LinearLayout.LayoutParams(dp(40), -2),
        )

        val details = column()

        details.addView(txt(envelope.name, 13f, ink, true))

        details.addView(
            txt(
                "${money(envelope.spent)} / ${money(envelope.limit)}",
                12f,
                gray,
            )
        )

        progress(
            details,
            envelope.spent,
            envelope.limit,
            if (envelope.spent > envelope.limit) red else green,
        )

        line.addView(
            details,
            LinearLayout.LayoutParams(0, -2, 1f),
        )

        line.addView(
            txt(
                "${percentage(envelope.spent, envelope.limit)}%",
                12f,
                green,
                true,
            )
        )

        parent.addView(line)
        divider(parent)
    }

    private fun goalSummary(
        parent: LinearLayout,
        goal: Goal,
    ) {
        val line = row()

        line.addView(
            txt("🎯", 21f, green),
            LinearLayout.LayoutParams(dp(40), -2),
        )

        val details = column()

        details.addView(txt(goal.name, 13f, ink, true))

        details.addView(
            txt(
                "${money(goal.saved)} / ${money(goal.target)}",
                12f,
                gray,
            )
        )

        progress(details, goal.saved, goal.target)

        line.addView(
            details,
            LinearLayout.LayoutParams(0, -2, 1f),
        )

        line.addView(
            txt(
                "${percentage(goal.saved, goal.target)}%",
                12f,
                green,
                true,
            )
        )

        parent.addView(line)
        divider(parent)
    }

    private fun billSummary(
        parent: LinearLayout,
        bill: Bill,
    ) {
        val line = row()

        line.addView(
            txt("🧾", 20f, gold),
            LinearLayout.LayoutParams(dp(40), -2),
        )

        val details = column()

        details.addView(txt(bill.name, 13f, ink, true))
        details.addView(txt(bill.due, 11f, gray))

        line.addView(
            details,
            LinearLayout.LayoutParams(0, -2, 1f),
        )

        line.addView(txt(money(bill.amount), 13f, ink, true))

        parent.addView(line)

        val statusText =
            when (bill.status) {
                "late" -> t("En retard", "Overdue")
                "soon" -> t("Bientôt", "Soon")
                "paid" -> t("Payée", "Paid")
                else -> t("À venir", "Upcoming")
            }

        parent.addView(
            txt(
                statusText,
                11f,
                if (bill.status == "late") red else green,
            )
        )

        divider(parent)
    }

    private fun transactionSummary(
        parent: LinearLayout,
        transaction: Transaction,
    ) {
        val line = row()

        line.addView(
            txt(
                if (transaction.amount >= 0) "↗" else "🛒",
                21f,
                if (transaction.amount >= 0) green else red,
            ),
            LinearLayout.LayoutParams(dp(40), -2),
        )

        val details = column()

        details.addView(txt(transaction.description, 13f, ink, true))

        details.addView(txt(transaction.category, 11f, gray))

        line.addView(
            details,
            LinearLayout.LayoutParams(0, -2, 1f),
        )

        line.addView(
            txt(
                money(transaction.amount),
                13f,
                if (transaction.amount >= 0) green else red,
                true,
            )
        )

        parent.addView(line)
        divider(parent)
    }

    private fun transactionDialog(isIncome: Boolean) {
        val form =
            column().apply {
                setPadding(dp(20), dp(10), dp(20), dp(10))
            }

        val description =
            field(
                form,
                t("Description", "Description"),
            )

        val amount =
            field(
                form,
                t("Montant", "Amount"),
                "",
                true,
            )

        val categories =
            if (french) {
                arrayOf(
                    "Alimentation",
                    "Transport",
                    "Loisirs",
                    "Sorties",
                    "Logement",
                    "Santé",
                    "Factures",
                    "Famille",
                    "Autres",
                )
            } else {
                arrayOf(
                    "Groceries",
                    "Transport",
                    "Leisure",
                    "Outings",
                    "Housing",
                    "Health",
                    "Bills",
                    "Family",
                    "Other",
                )
            }

        val spinner = Spinner(this)

        spinner.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                categories,
            )

        form.addView(spinner)

        AlertDialog.Builder(this)
            .setTitle(
                if (isIncome) {
                    t("Ajouter un revenu", "Add income")
                } else {
                    t("Ajouter une dépense", "Add expense")
                }
            )
            .setView(form)
            .setNegativeButton(t("Annuler", "Cancel"), null)
            .setPositiveButton(t("Enregistrer", "Save")) { _, _ ->
                try {
                    val name = description.text.toString().trim()
                    val amountValue = read(amount)

                    require(name.isNotBlank() && amountValue > 0)

                    val actual =
                        if (isIncome) {
                            amountValue
                        } else {
                            -amountValue
                        }

                    val all = transactions()

                    all.add(
                        Transaction(
                            name,
                            actual,
                            if (isIncome) {
                                t("Revenus", "Income")
                            } else {
                                spinner.selectedItem.toString()
                            },
                            System.currentTimeMillis(),
                        )
                    )

                    saveTransactions(all)

                    val key = if (isIncome) "income" else "expenses"

                    put(
                        key,
                        get(
                            key,
                            if (isIncome) 4850.0 else 3120.0,
                        ) + amountValue,
                    )

                    if (!isIncome) {
                        val allEnvelopes = envelopes()
                        val selected = spinner.selectedItem.toString()

                        val index = allEnvelopes.indexOfFirst {
                            it.name.equals(selected, true)
                        }

                        if (index >= 0) {
                            val old = allEnvelopes[index]

                            allEnvelopes[index] = old.copy(spent = old.spent + amountValue)

                            saveEnvelopes(allEnvelopes)
                        }
                    }

                    toast(t("Transaction enregistrée", "Transaction saved"))
                    build()
                } catch (e: Exception) {
                    toast(t("Données invalides", "Invalid data"))
                }
            }
            .show()
    }

    private fun transactionsPage() {
        val intro = card(body, lightBlue)

        heading(
            intro,
            t("⇄ Transactions", "⇄ Transactions"),
        )

        intro.addView(
            button(t("＋ Ajouter un revenu", "＋ Add income")) {
                transactionDialog(true)
            }
        )

        intro.addView(
            button(
                t("− Ajouter une dépense", "− Add expense"),
                red,
            ) {
                transactionDialog(false)
            }
        )

        val list = card()
        val entries = transactions().reversed()

        if (entries.isEmpty()) {
            list.addView(
                txt(
                    t("Aucune transaction.", "No transactions."),
                    14f,
                    gray,
                )
            )
        }

        entries.forEach {
            transactionSummary(list, it)
        }
    }

    private fun budgetPage() {
        val intro = card(body, mint)

        heading(
            intro,
            t("💰 Mon budget", "💰 My budget"),
        )

        intro.addView(
            txt(
                t(
                    "Organisez vos revenus, vos dépenses et vos priorités.",
                    "Organize income, spending and priorities.",
                ),
                13f,
                gray,
            )
        )

        val form = card()

        val inputs =
            listOf(
                field(
                    form,
                    t("Revenu mensuel net", "Net monthly income"),
                    get("income", 4850.0).toString(),
                    true,
                ),
                field(
                    form,
                    t("Dépenses mensuelles", "Monthly expenses"),
                    get("expenses", 3120.0).toString(),
                    true,
                ),
                field(
                    form,
                    t("Budget maximum", "Budget limit"),
                    get("budgetLimit", 4500.0).toString(),
                    true,
                ),
                field(
                    form,
                    t("Épargne accumulée", "Accumulated savings"),
                    get("savings", 2450.0).toString(),
                    true,
                ),
                field(
                    form,
                    t("Factures communes", "Shared bills"),
                    get("bills", 1500.0).toString(),
                    true,
                ),
                field(
                    form,
                    t("Allocation personnelle A", "Personal allowance A"),
                    get("mine", 300.0).toString(),
                    true,
                ),
                field(
                    form,
                    t("Allocation personnelle B", "Personal allowance B"),
                    get("yours", 300.0).toString(),
                    true,
                ),
            )

        val keys =
            listOf(
                "income",
                "expenses",
                "budgetLimit",
                "savings",
                "bills",
                "mine",
                "yours",
            )

        form.addView(
            button(t("Enregistrer mon budget", "Save my budget")) {
                try {
                    val values = inputs.map { read(it) }

                    require(values.all { it >= 0 })

                    keys.forEachIndexed { index, key ->
                        put(key, values[index])
                    }

                    toast(t("Budget enregistré", "Budget saved"))
                    build()
                } catch (e: Exception) {
                    toast(t("Montants invalides", "Invalid amounts"))
                }
            }
        )

        val result = card()

        heading(
            result,
            t("📊 Analyse du budget", "📊 Budget analysis"),
        )

        val income = get("income", 4850.0)
        val expenses = get("expenses", 3120.0)

        stat(
            result,
            t("Marge mensuelle indicative", "Indicative monthly surplus"),
            money(income - expenses),
            if (income >= expenses) green else red,
        )

        progress(
            result,
            expenses,
            get("budgetLimit", 4500.0),
        )

        result.addView(
            txt(
                t(
                    "Vérifiez les dépenses annuelles et les remboursements de capital avant de considérer la marge comme disponible.",
                    "Check annual expenses and principal repayments before treating the surplus as available.",
                ),
                12f,
                gray,
            )
        )

        result.addView(
            button(t("Analyser ma situation", "Analyze my situation")) {
                go(7)
            }
        )
    }

    private fun envelopesPage() {
        val intro = card(body, mint)

        heading(
            intro,
            t("👛 Mes enveloppes", "👛 My envelopes"),
        )

        intro.addView(
            button(t("＋ Nouvelle enveloppe", "＋ New envelope")) {
                envelopeDialog()
            }
        )

        envelopes().forEachIndexed { index, envelope ->
            val box = card()

            heading(box, envelope.name)

            stat(
                box,
                t("Dépensé", "Spent"),
                money(envelope.spent),
                if (envelope.spent > envelope.limit) red else green,
            )

            box.addView(
                txt(
                    "${money(envelope.spent)} / ${money(envelope.limit)}",
                    13f,
                    gray,
                )
            )

            progress(
                box,
                envelope.spent,
                envelope.limit,
                if (envelope.spent > envelope.limit) red else green,
            )

            box.addView(
                secondary(t("Modifier", "Edit")) {
                    envelopeDialog(index)
                }
            )
        }
    }

    private fun envelopeDialog(index: Int = -1) {
        val old = envelopes().getOrNull(index)

        val form =
            column().apply {
                setPadding(dp(20), dp(10), dp(20), dp(10))
            }

        val name =
            field(
                form,
                t("Nom de l'enveloppe", "Envelope name"),
                old?.name ?: "",
            )

        val limit =
            field(
                form,
                t("Limite", "Limit"),
                old?.limit?.toString() ?: "",
                true,
            )

        val spent =
            field(
                form,
                t("Dépensé", "Spent"),
                old?.spent?.toString() ?: "0",
                true,
            )

        AlertDialog.Builder(this)
            .setTitle(t("Enveloppe", "Envelope"))
            .setView(form)
            .setNegativeButton(t("Annuler", "Cancel"), null)
            .setPositiveButton(t("Enregistrer", "Save")) { _, _ ->
                try {
                    val n = name.text.toString().trim()
                    val l = read(limit)
                    val s = read(spent)

                    require(n.isNotBlank() && l > 0 && s >= 0)

                    val all = envelopes()
                    val updated = Envelope(n, l, s)

                    if (index in all.indices) {
                        all[index] = updated
                    } else {
                        all.add(updated)
                    }

                    saveEnvelopes(all)
                    build()
                } catch (e: Exception) {
                    toast(t("Données invalides", "Invalid data"))
                }
            }
            .show()
    }

    private fun goalsPage() {
        val intro = card(body, yellow)

        heading(
            intro,
            t("🎯 Mes objectifs", "🎯 My goals"),
        )

        intro.addView(
            button(t("＋ Nouvel objectif", "＋ New goal")) {
                goalDialog()
            }
        )

        goals().forEachIndexed { index, goal ->
            val box = card()

            heading(box, "🎯 ${goal.name}")

            stat(
                box,
                t("Montant épargné", "Amount saved"),
                money(goal.saved),
            )

            box.addView(
                txt(
                    t("Objectif : ", "Target: ") + money(goal.target),
                    13f,
                    gray,
                )
            )

            progress(box, goal.saved, goal.target)

            box.addView(
                txt(
                    "${percentage(goal.saved, goal.target)}%",
                    13f,
                    green,
                    true,
                )
            )

            box.addView(
                txt(
                    t("Reste à épargner : ", "Remaining: ") +
                        money((goal.target - goal.saved).coerceAtLeast(0.0)),
                    13f,
                    gray,
                )
            )

            box.addView(
                secondary(t("Modifier", "Edit")) {
                    goalDialog(index)
                }
            )
        }
    }

    private fun goalDialog(index: Int = -1) {
        val old = goals().getOrNull(index)

        val form =
            column().apply {
                setPadding(dp(20), dp(10), dp(20), dp(10))
            }

        val name =
            field(
                form,
                t("Nom de l'objectif", "Goal name"),
                old?.name ?: "",
            )

        val target =
            field(
                form,
                t("Montant cible", "Target amount"),
                old?.target?.toString() ?: "",
                true,
            )

        val saved =
            field(
                form,
                t("Déjà épargné", "Already saved"),
                old?.saved?.toString() ?: "0",
                true,
            )

        AlertDialog.Builder(this)
            .setTitle(t("Objectif", "Goal"))
            .setView(form)
            .setNegativeButton(t("Annuler", "Cancel"), null)
            .setPositiveButton(t("Enregistrer", "Save")) { _, _ ->
                try {
                    val n = name.text.toString().trim()
                    val targetValue = read(target)
                    val savedValue = read(saved)

                    require(n.isNotBlank() && targetValue > 0 && savedValue >= 0)

                    val all = goals()
                    val updated = Goal(n, targetValue, savedValue)

                    if (index in all.indices) {
                        all[index] = updated
                    } else {
                        all.add(updated)
                    }

                    saveGoals(all)
                    build()
                } catch (e: Exception) {
                    toast(t("Données invalides", "Invalid data"))
                }
            }
            .show()
    }

    private fun billsPage() {
        val intro = card(body, lavender)

        heading(
            intro,
            t("🧾 Mes factures", "🧾 My bills"),
        )

        intro.addView(
            button(t("＋ Ajouter une facture", "＋ Add a bill")) {
                billDialog()
            }
        )

        bills().forEachIndexed { index, bill ->
            val box = card()

            heading(box, bill.name)

            stat(
                box,
                t("Montant", "Amount"),
                money(bill.amount),
            )

            box.addView(
                txt(
                    t("Échéance : ", "Due: ") + bill.due,
                    13f,
                    gray,
                )
            )

            box.addView(
                txt(
                    when (bill.status) {
                        "paid" -> t("Payée", "Paid")
                        "late" -> t("En retard", "Overdue")
                        "soon" -> t("Bientôt", "Soon")
                        else -> t("À venir", "Upcoming")
                    },
                    13f,
                    if (bill.status == "late") red else green,
                    true,
                )
            )

            box.addView(
                secondary(t("Modifier", "Edit")) {
                    billDialog(index)
                }
            )
        }
    }

    private fun billDialog(index: Int = -1) {
        val old = bills().getOrNull(index)

        val form =
            column().apply {
                setPadding(dp(20), dp(10), dp(20), dp(10))
            }

        val name =
            field(
                form,
                t("Nom de la facture", "Bill name"),
                old?.name ?: "",
            )

        val amount =
            field(
                form,
                t("Montant", "Amount"),
                old?.amount?.toString() ?: "",
                true,
            )

        val due =
            field(
                form,
                t("Échéance", "Due date"),
                old?.due ?: "",
            )

        val options =
            if (french) {
                arrayOf("À venir", "Bientôt", "En retard", "Payée")
            } else {
                arrayOf("Upcoming", "Soon", "Overdue", "Paid")
            }

        val codes = listOf("ok", "soon", "late", "paid")
        val status = Spinner(this)

        status.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                options,
            )

        status.setSelection(codes.indexOf(old?.status ?: "ok").coerceAtLeast(0))

        form.addView(status)

        AlertDialog.Builder(this)
            .setTitle(t("Facture", "Bill"))
            .setView(form)
            .setNegativeButton(t("Annuler", "Cancel"), null)
            .setPositiveButton(t("Enregistrer", "Save")) { _, _ ->
                try {
                    val n = name.text.toString().trim()
                    val a = read(amount)
                    val d = due.text.toString().trim()

                    require(n.isNotBlank() && a >= 0 && d.isNotBlank())

                    val all = bills()

                    val updated =
                        Bill(
                            n,
                            a,
                            d,
                            codes[status.selectedItemPosition],
                        )

                    if (index in all.indices) {
                        all[index] = updated
                    } else {
                        all.add(updated)
                    }

                    saveBills(all)
                    build()
                } catch (e: Exception) {
                    toast(t("Données invalides", "Invalid data"))
                }
            }
            .show()
    }

    private fun familyPage() {
        val intro = card(body, mint)

        heading(
            intro,
            t("👨‍👩‍👧 Notre famille", "👨‍👩‍👧 Our family"),
        )

        intro.addView(
            txt(
                t(
                    "Autonomie, coopération et respect des choix personnels.",
                    "Autonomy, cooperation and respect for personal choices.",
                ),
                13f,
                gray,
            )
        )

        val membersCard = card()

        val members =
            field(
                membersCard,
                t("Nombre de membres", "Number of members"),
                get("members", 4.0).toInt().toString(),
                true,
            )

        membersCard.addView(
            button(t("Enregistrer", "Save")) {
                try {
                    val count = read(members)

                    require(count in 1.0..30.0)

                    put("members", count)
                    build()
                } catch (e: Exception) {
                    toast(t("Nombre invalide", "Invalid number"))
                }
            }
        )

        val meeting = card()

        heading(
            meeting,
            t("💬 Money Date — 2 minutes", "💬 Money Date — 2 minutes"),
        )

        val questions =
            if (french) {
                listOf(
                    "1. Quelle réussite financière avons-nous eue ?",
                    "2. Quelle dépense devons-nous prévoir ?",
                    "3. Quel objectif familial est prioritaire ?",
                    "4. Quelle décision voulons-nous prendre ensemble ?",
                )
            } else {
                listOf(
                    "1. What financial win did we have?",
                    "2. Which expense should we prepare for?",
                    "3. Which family goal comes first?",
                    "4. What decision should we make together?",
                )
            }

        questions.forEach {
            meeting.addView(txt(it, 14f))
        }

        meeting.addView(
            button(t("Bilan terminé", "Meeting completed")) {
                put("checkins", get("checkins") + 1.0)
                build()
            }
        )

        stat(
            meeting,
            t("Bilans terminés", "Completed meetings"),
            get("checkins").toInt().toString(),
        )

        val education = card()

        heading(
            education,
            t("🎓 Études des enfants", "🎓 Children's education"),
        )

        education.addView(
            txt(
                t(
                    "Le REEE peut permettre de recevoir des subventions pour financer les études admissibles.",
                    "An RESP can provide grants toward eligible education.",
                ),
                13f,
                gray,
            )
        )

        education.addView(
            button(t("Découvrir le REEE", "Learn about RESPs")) {
                go(9)
            }
        )

        val protection = card()

        heading(
            protection,
            t("🛡 Protection familiale", "🛡 Family protection"),
        )

        protection.addView(
            txt(
                t(
                    "Vérifiez le fonds d'urgence, les assurances, les bénéficiaires et les documents juridiques.",
                    "Review emergency savings, insurance, beneficiaries and legal documents.",
                ),
                13f,
                gray,
            )
        )

        protection.addView(
            button(t("Faire une analyse", "Start assessment")) {
                go(7)
            }
        )
    }

    private fun questionnairePage() {
        val intro = card(body, mint)

        heading(
            intro,
            t(
                "📋 Questionnaire personnalisé",
                "📋 Personalized questionnaire",
            ),
        )

        intro.addView(
            txt(
                t(
                    "Répondez selon votre besoin pour obtenir un plan éducatif adapté.",
                    "Answer according to your needs to receive an educational action plan.",
                ),
                13f,
                gray,
            )
        )

        val topics =
            if (french) {
                arrayOf(
                    "Améliorer mon budget",
                    "Rembourser mes dettes",
                    "Créer un fonds d'urgence",
                    "Acheter une maison",
                    "Épargner pour un objectif",
                    "Choisir REER, CELI ou CELIAPP",
                    "Préparer ma retraite",
                    "Investir",
                    "Protéger ma famille",
                    "Comprendre ma fiscalité",
                )
            } else {
                arrayOf(
                    "Improve my budget",
                    "Pay off debt",
                    "Build an emergency fund",
                    "Buy a home",
                    "Save for a goal",
                    "Choose RRSP, TFSA or FHSA",
                    "Plan retirement",
                    "Invest",
                    "Protect my family",
                    "Understand taxes",
                )
            }

        val form = card()

        heading(
            form,
            t("Votre besoin principal", "Your main goal"),
        )

        val topicSpinner = Spinner(this)

        topicSpinner.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                topics,
            )

        form.addView(topicSpinner)

        val answersHolder = column()
        form.addView(answersHolder)

        val answerSpinners = mutableListOf<Spinner>()

        fun question(
            label: String,
            options: Array<String>,
        ) {
            answersHolder.addView(txt(label, 13f, ink, true))

            val spinner = Spinner(this)

            spinner.adapter =
                ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    options,
                )

            answersHolder.addView(spinner)
            answerSpinners.add(spinner)
        }

        fun renderQuestions(index: Int) {
            answersHolder.removeAllViews()
            answerSpinners.clear()

            when (index) {
                0 -> {
                    question(
                        t(
                            "Suivez-vous vos dépenses ?",
                            "Do you track expenses?",
                        ),
                        arrayOf(
                            t("Oui", "Yes"),
                            t("Non", "No"),
                            t("Partiellement", "Partially"),
                        ),
                    )

                    question(
                        t(
                            "Avez-vous des dépenses irrégulières ?",
                            "Do you have irregular expenses?",
                        ),
                        arrayOf(
                            t("Oui", "Yes"),
                            t("Non", "No"),
                            t("Incertain", "Not sure"),
                        ),
                    )
                }

                1 -> {
                    question(
                        t(
                            "Quel type de dette ?",
                            "What kind of debt?",
                        ),
                        arrayOf(
                            t("Carte de crédit", "Credit card"),
                            t("Prêt personnel", "Personal loan"),
                            t("Prêt automobile", "Car loan"),
                            t("Hypothèque", "Mortgage"),
                        ),
                    )

                    question(
                        t(
                            "Quel taux d'intérêt ?",
                            "What interest rate?",
                        ),
                        arrayOf(
                            "0 à 5 %",
                            "5 à 10 %",
                            "10 à 20 %",
                            "20 % et plus",
                        ),
                    )
                }

                2 -> {
                    question(
                        t(
                            "Votre revenu est-il stable ?",
                            "Is your income stable?",
                        ),
                        arrayOf(
                            t("Stable", "Stable"),
                            t("Variable", "Variable"),
                            t("Incertain", "Uncertain"),
                        ),
                    )

                    question(
                        t(
                            "Avez-vous des personnes à charge ?",
                            "Do you have dependants?",
                        ),
                        arrayOf(
                            t("Oui", "Yes"),
                            t("Non", "No"),
                        ),
                    )
                }

                3 -> {
                    question(
                        t(
                            "Première habitation admissible ?",
                            "Eligible first home?",
                        ),
                        arrayOf(
                            t("Oui", "Yes"),
                            t("Non", "No"),
                            t("Incertain", "Not sure"),
                        ),
                    )

                    question(
                        t(
                            "Quand souhaitez-vous acheter ?",
                            "When do you want to buy?",
                        ),
                        arrayOf(
                            t("Moins de 2 ans", "Under 2 years"),
                            t("2 à 5 ans", "2 to 5 years"),
                            t("Plus de 5 ans", "Over 5 years"),
                        ),
                    )
                }

                4 -> {
                    question(
                        t(
                            "Votre objectif est-il flexible ?",
                            "Is your goal flexible?",
                        ),
                        arrayOf(
                            t("Oui", "Yes"),
                            t("Non", "No"),
                        ),
                    )

                    question(
                        t(
                            "Épargnez-vous chaque mois ?",
                            "Do you save monthly?",
                        ),
                        arrayOf(
                            t("Oui", "Yes"),
                            t("Non", "No"),
                        ),
                    )
                }

                5 -> {
                    question(
                        t(
                            "Revenu imposable élevé actuellement ?",
                            "High taxable income now?",
                        ),
                        arrayOf(
                            t("Oui", "Yes"),
                            t("Non", "No"),
                            t("Incertain", "Not sure"),
                        ),
                    )

                    question(
                        t(
                            "Projet de première habitation ?",
                            "First home plans?",
                        ),
                        arrayOf(
                            t("Oui", "Yes"),
                            t("Non", "No"),
                        ),
                    )
                }

                6 -> {
                    question(
                        t(
                            "Régime de retraite d'employeur ?",
                            "Employer pension plan?",
                        ),
                        arrayOf(
                            t("Oui", "Yes"),
                            t("Non", "No"),
                            t("Incertain", "Not sure"),
                        ),
                    )

                    question(
                        t(
                            "Horizon de retraite ?",
                            "Retirement horizon?",
                        ),
                        arrayOf(
                            t("Moins de 5 ans", "Under 5 years"),
                            t("5 à 15 ans", "5 to 15 years"),
                            t("Plus de 15 ans", "Over 15 years"),
                        ),
                    )
                }

                7 -> {
                    question(
                        t(
                            "Réaction à une baisse de 20 % ?",
                            "Reaction to a 20% decline?",
                        ),
                        arrayOf(
                            t("Je vendrais", "I would sell"),
                            t("J'attendrais", "I would wait"),
                            t("Je suivrais mon plan", "I would follow my plan"),
                        ),
                    )

                    question(
                        t(
                            "Quand aurez-vous besoin des fonds ?",
                            "When will you need the funds?",
                        ),
                        arrayOf(
                            t("Moins de 3 ans", "Under 3 years"),
                            t("3 à 10 ans", "3 to 10 years"),
                            t("Plus de 10 ans", "Over 10 years"),
                        ),
                    )
                }

                8 -> {
                    question(
                        t(
                            "Enfants ou personnes à charge ?",
                            "Children or dependants?",
                        ),
                        arrayOf(
                            t("Oui", "Yes"),
                            t("Non", "No"),
                        ),
                    )

                    question(
                        t(
                            "Vos protections sont-elles à jour ?",
                            "Is your coverage current?",
                        ),
                        arrayOf(
                            t("Oui", "Yes"),
                            t("Non", "No"),
                            t("Incertain", "Not sure"),
                        ),
                    )
                }

                else -> {
                    question(
                        t(
                            "Produisez-vous une déclaration québécoise ?",
                            "Do you file a Quebec return?",
                        ),
                        arrayOf(
                            t("Oui", "Yes"),
                            t("Non", "No"),
                        ),
                    )

                    question(
                        t(
                            "Voulez-vous examiner crédits et déductions ?",
                            "Review credits and deductions?",
                        ),
                        arrayOf(
                            t("Oui", "Yes"),
                            t("Non", "No"),
                        ),
                    )
                }
            }
        }

        topicSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    renderQuestions(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        renderQuestions(0)

        val income =
            field(
                form,
                t("Revenu mensuel net", "Monthly net income"),
                get("income", 4850.0).toString(),
                true,
            )

        val expenses =
            field(
                form,
                t("Dépenses mensuelles", "Monthly expenses"),
                get("expenses", 3120.0).toString(),
                true,
            )

        val debt =
            field(
                form,
                t("Dettes totales", "Total debt"),
                get("debt", 0.0).toString(),
                true,
            )

        val liquid =
            field(
                form,
                t("Épargne liquide", "Liquid savings"),
                get("liquidSavings", 1000.0).toString(),
                true,
            )

        val horizon =
            field(
                form,
                t("Horizon (années)", "Horizon (years)"),
                "5",
                true,
            )

        val output = card()

        form.addView(
            button(t("Obtenir mes suggestions", "Get my suggestions")) {
                try {
                    val i = read(income)
                    val e = read(expenses)
                    val d = read(debt)
                    val s = read(liquid)
                    val h = read(horizon)

                    require(i >= 0 && e >= 0 && d >= 0 && s >= 0 && h >= 0)

                    put("liquidSavings", s)
                    put("debt", d)

                    val answers = answerSpinners.map {
                        it.selectedItemPosition
                    }

                    val recommendation =
                        makeRecommendations(
                            topicSpinner.selectedItemPosition,
                            i,
                            e,
                            d,
                            s,
                            h,
                            answers,
                        )

                    prefs.edit().putString("last_recommendation", recommendation).apply()

                    output.removeAllViews()

                    heading(
                        output,
                        t(
                            "✅ Votre plan personnalisé",
                            "✅ Your personalized plan",
                        ),
                    )

                    output.addView(txt(recommendation, 14f))

                    output.addView(
                        button(t("Discuter du plan", "Discuss the plan")) {
                            go(8)
                        }
                    )
                } catch (e: Exception) {
                    toast(t("Montants invalides", "Invalid amounts"))
                }
            }
        )

        val last = prefs.getString("last_recommendation", "") ?: ""

        if (last.isNotBlank()) {
            heading(
                output,
                t("Dernière analyse", "Last assessment"),
            )

            output.addView(txt(last))
        }
    }

    private fun makeRecommendations(
        topic: Int,
        income: Double,
        expenses: Double,
        debt: Double,
        savings: Double,
        horizon: Double,
        answers: List<Int>,
    ): String {
        val surplus = income - expenses
        val response = StringBuilder()

        response.append(
            t(
                "REVUE DE VOTRE SITUATION\n\n",
                "YOUR FINANCIAL REVIEW\n\n",
            )
        )

        response.append(
            t(
                "Revenu : ${money(income)}\n" +
                    "Dépenses : ${money(expenses)}\n" +
                    "Marge indicative : ${money(surplus)}\n" +
                    "Dettes : ${money(debt)}\n" +
                    "Liquidités : ${money(savings)}\n\n",
                "Income: ${money(income)}\n" +
                    "Expenses: ${money(expenses)}\n" +
                    "Indicative surplus: ${money(surplus)}\n" +
                    "Debt: ${money(debt)}\n" +
                    "Liquid savings: ${money(savings)}\n\n",
            )
        )

        if (surplus < 0) {
            response.append(
                t(
                    "PRIORITÉ : vos dépenses déclarées dépassent vos revenus. Révisez d'abord le budget et les engagements fixes.\n\n",
                    "PRIORITY: reported spending exceeds income. Review your budget and fixed obligations first.\n\n",
                )
            )
        }

        val first = answers.getOrElse(0) { 0 }
        val second = answers.getOrElse(1) { 0 }

        when (topic) {
            0 -> {
                response.append(
                    t(
                        "BUDGET\n\n" +
                            "1. Distinguez dépenses fixes, variables et annuelles.\n" +
                            "2. Comparez la méthode directe et la méthode résiduelle du coût de vie.\n" +
                            "3. Réservez les montants des dépenses irrégulières.\n" +
                            "4. Automatisez une épargne compatible avec votre marge.\n",
                        "BUDGET\n\n" +
                            "1. Separate fixed, variable and annual expenses.\n" +
                            "2. Compare direct and residual living-cost calculations.\n" +
                            "3. Set aside funds for irregular bills.\n" +
                            "4. Automate affordable savings.\n",
                    )
                )

                if (first == 1 || first == 2) {
                    response.append(
                        t(
                            "\nCommencez par noter chaque transaction pendant un mois.\n",
                            "\nStart by recording every transaction for one month.\n",
                        )
                    )
                }

                if (second == 0) {
                    response.append(
                        t(
                            "\nCréez des enveloppes pour les dépenses annuelles.\n",
                            "\nCreate envelopes for annual expenses.\n",
                        )
                    )
                }
            }

            1 -> {
                response.append(
                    t(
                        "DETTES\n\n" +
                            "1. Listez chaque solde, taux et paiement minimum.\n" +
                            "2. L'avalanche priorise le taux le plus élevé.\n" +
                            "3. La boule de neige priorise le plus petit solde.\n" +
                            "4. Comparez le coût total d'une consolidation.\n" +
                            "5. Préservez une réserve de liquidités.\n",
                        "DEBT\n\n" +
                            "1. List balances, interest rates and minimum payments.\n" +
                            "2. Avalanche prioritizes the highest rate.\n" +
                            "3. Snowball prioritizes the smallest balance.\n" +
                            "4. Compare total consolidation costs.\n" +
                            "5. Keep a cash buffer.\n",
                    )
                )

                if (second >= 2) {
                    response.append(
                        t(
                            "\nVotre catégorie de taux déclaré est élevée. Examinez le remboursement prioritaire de cette dette.\n",
                            "\nYour reported interest-rate category is high. Consider prioritizing repayment.\n",
                        )
                    )
                }
            }

            2 -> {
                val target = expenses * 3
                val missing = (target - savings).coerceAtLeast(0.0)

                response.append(
                    t(
                        "FONDS D'URGENCE\n\n" +
                            "Repère de trois mois : ${money(target)}.\n" +
                            "Montant supplémentaire indicatif : ${money(missing)}.\n\n" +
                            "Conservez une réserve liquide et facilement accessible.\n",
                        "EMERGENCY FUND\n\n" +
                            "Three-month benchmark: ${money(target)}.\n" +
                            "Indicative shortfall: ${money(missing)}.\n\n" +
                            "Keep the reserve liquid and accessible.\n",
                    )
                )

                if (first != 0 || second == 0) {
                    response.append(
                        t(
                            "\nUn revenu variable ou des personnes à charge peuvent justifier une réserve plus élevée.\n",
                            "\nVariable income or dependants may justify a larger reserve.\n",
                        )
                    )
                }
            }

            3 -> {
                response.append(
                    t(
                        "ACHAT IMMOBILIER\n\n" +
                            "1. Estimez la mise de fonds et les frais de clôture.\n" +
                            "2. Préservez le fonds d'urgence.\n" +
                            "3. Étudiez CELIAPP et RAP si admissibles.\n" +
                            "4. Calculez ABD et ATD.\n" +
                            "5. Comparez les coûts de louer et acheter.\n",
                        "HOME PURCHASE\n\n" +
                            "1. Estimate down payment and closing costs.\n" +
                            "2. Preserve emergency savings.\n" +
                            "3. Review FHSA and HBP eligibility.\n" +
                            "4. Calculate housing and debt ratios.\n" +
                            "5. Compare renting and ownership costs.\n",
                    )
                )

                if (second == 0) {
                    response.append(
                        t(
                            "\nUn achat rapproché favorise généralement la disponibilité et la préservation de la mise de fonds.\n",
                            "\nFor a near-term purchase, accessibility and preservation of the down payment usually matter most.\n",
                        )
                    )
                }
            }

            4 -> {
                response.append(
                    t(
                        "OBJECTIF D'ÉPARGNE\n\n" +
                            "1. Déterminez le montant et la date cible.\n" +
                            "2. Calculez les versements mensuels nécessaires.\n" +
                            "3. Automatisez les cotisations.\n" +
                            "4. Adaptez le risque à l'horizon de ${fmt(horizon)} années.\n",
                        "SAVINGS GOAL\n\n" +
                            "1. Set a target amount and date.\n" +
                            "2. Calculate the monthly contribution required.\n" +
                            "3. Automate deposits.\n" +
                            "4. Match risk to the ${fmt(horizon)}-year horizon.\n",
                    )
                )
            }

            5 -> {
                response.append(
                    t(
                        "REER / CELI / CELIAPP\n\n" +
                            "1. Vérifiez les droits de cotisation auprès de l'ARC.\n" +
                            "2. Comparez le TEMI actuel et futur.\n" +
                            "3. Le CELI offre de la flexibilité.\n" +
                            "4. Le REER permet une déduction admissible.\n" +
                            "5. Le CELIAPP concerne les premiers acheteurs admissibles.\n",
                        "RRSP / TFSA / FHSA\n\n" +
                            "1. Verify contribution room with the CRA.\n" +
                            "2. Compare current and future effective marginal rates.\n" +
                            "3. TFSA offers flexibility.\n" +
                            "4. RRSP may provide a deduction.\n" +
                            "5. FHSA is intended for eligible first-time buyers.\n",
                    )
                )
            }

            6 -> {
                response.append(
                    t(
                        "RETRAITE\n\n" +
                            "1. Estimez votre coût de vie futur.\n" +
                            "2. Inventoriez RRQ, PSV, RPA, REER, FERR et CELI.\n" +
                            "3. Évaluez les impôts futurs.\n" +
                            "4. Examinez le risque de séquence.\n" +
                            "5. Préparez une stratégie de décaissement.\n",
                        "RETIREMENT\n\n" +
                            "1. Estimate future living costs.\n" +
                            "2. Inventory pensions and registered accounts.\n" +
                            "3. Estimate future taxes.\n" +
                            "4. Review sequence-of-returns risk.\n" +
                            "5. Plan withdrawals.\n",
                    )
                )
            }

            7 -> {
                response.append(
                    t(
                        "PLACEMENTS\n\n" +
                            "1. Évaluez votre capacité, tolérance et besoin de risque.\n" +
                            "2. Diversifiez les catégories d'actifs et les régions.\n" +
                            "3. Comparez les frais.\n" +
                            "4. Tenez compte des impôts et de l'inflation.\n" +
                            "5. Écrivez une politique de placement.\n",
                        "INVESTING\n\n" +
                            "1. Assess risk capacity, tolerance and need.\n" +
                            "2. Diversify across assets and regions.\n" +
                            "3. Compare fees.\n" +
                            "4. Consider taxes and inflation.\n" +
                            "5. Write an investment policy.\n",
                    )
                )

                if (first == 0 || second == 0) {
                    response.append(
                        t(
                            "\nVotre réponse suggère une tolérance faible ou un horizon court. Évitez de dépendre de placements volatils pour vos besoins proches.\n",
                            "\nYour answer suggests lower tolerance or a short horizon. Avoid relying on volatile investments for near-term needs.\n",
                        )
                    )
                }
            }

            8 -> {
                response.append(
                    t(
                        "PROTECTION FAMILIALE\n\n" +
                            "1. Vérifiez le fonds d'urgence.\n" +
                            "2. Examinez les risques de perte de revenu.\n" +
                            "3. Révisez les assurances pertinentes.\n" +
                            "4. Vérifiez les bénéficiaires et documents légaux.\n" +
                            "5. Considérez REEE et planification successorale.\n",
                        "FAMILY PROTECTION\n\n" +
                            "1. Review emergency savings.\n" +
                            "2. Assess income-loss risks.\n" +
                            "3. Review relevant insurance.\n" +
                            "4. Check beneficiaries and legal documents.\n" +
                            "5. Consider education savings and estate planning.\n",
                    )
                )
            }

            else -> {
                response.append(
                    t(
                        "FISCALITÉ\n\n" +
                            "1. Examinez les déclarations fédérale et québécoise.\n" +
                            "2. Distinguez TM, TMI et TEMI.\n" +
                            "3. Vérifiez crédits et déductions.\n" +
                            "4. Étudiez CE3D.\n" +
                            "5. Vérifiez les règles actuelles avant d'agir.\n",
                        "TAXES\n\n" +
                            "1. Review federal and Quebec returns.\n" +
                            "2. Distinguish average, marginal and effective marginal tax rates.\n" +
                            "3. Review credits and deductions.\n" +
                            "4. Explore the CE3D framework.\n" +
                            "5. Verify current rules before acting.\n",
                    )
                )
            }
        }

        response.append(
            t(
                "\nPROCHAINES ÉTAPES\n" +
                    "• Vérifiez vos chiffres.\n" +
                    "• Choisissez une priorité.\n" +
                    "• Fixez une action et une date de révision.\n\n" +
                    "Suggestions éducatives seulement. Vérifiez les paramètres actuels auprès des sources officielles.",
                "\nNEXT STEPS\n" +
                    "• Verify your figures.\n" +
                    "• Choose a priority.\n" +
                    "• Set an action and review date.\n\n" +
                    "Educational suggestions only. Verify current rules with official sources.",
            )
        )

        return response.toString()
    }

    private fun assistantPage() {
        val intro = card(body, mint)

        heading(
            intro,
            t("🤖 Assistant financier", "🤖 Financial assistant"),
        )

        intro.addView(
            txt(
                t(
                    "Posez vos questions sur le budget, les dettes, l'épargne, le crédit, les placements ou la fiscalité.",
                    "Ask about budgeting, debt, savings, credit, investing or taxes.",
                ),
                13f,
                gray,
            )
        )

        intro.addView(
            txt(
                t(
                    "Assistant conversationnel local fondé sur des règles éducatives. Aucun modèle IA en ligne n'est connecté.",
                    "Local rule-based educational assistant. No online AI model is connected.",
                ),
                12f,
                green,
            )
        )

        val suggestions = card()

        heading(
            suggestions,
            t("Questions suggérées", "Suggested questions"),
        )

        val prompts =
            if (french) {
                listOf(
                    "Comment améliorer mon budget ?",
                    "Quelle dette rembourser en premier ?",
                    "Combien garder en fonds d'urgence ?",
                    "REER ou CELI ?",
                    "Comment acheter une maison ?",
                    "Comment commencer à investir ?",
                    "Comment épargner pour mes enfants ?",
                )
            } else {
                listOf(
                    "How can I improve my budget?",
                    "Which debt should I pay first?",
                    "How much emergency savings do I need?",
                    "RRSP or TFSA?",
                    "How can I buy a home?",
                    "How can I start investing?",
                    "How can I save for my children?",
                )
            }

        prompts.forEach { prompt ->
            suggestions.addView(
                secondary(prompt) {
                    addChat(prompt, true)
                    addChat(financialReply(prompt), false)
                    build()
                }
            )
        }

        val chat = card()

        heading(
            chat,
            t("💬 Conversation", "💬 Conversation"),
        )

        val history = readArray("chat_v2")

        if (history.length() == 0) {
            chatBubble(
                chat,
                t(
                    "Bonjour ! Quel est votre objectif financier aujourd'hui ?",
                    "Hello! What is your financial goal today?",
                ),
                false,
            )
        }

        for (i in 0 until history.length()) {
            val item = history.optJSONObject(i) ?: continue

            chatBubble(
                chat,
                item.optString("text"),
                item.optBoolean("user"),
            )
        }

        val composer = card()

        val input =
            EditText(this).apply {
                hint =
                    t(
                        "Écrivez votre question...",
                        "Write your question...",
                    )
                minLines = 2
                maxLines = 5
                setSingleLine(false)
            }

        composer.addView(input)

        composer.addView(
            button(t("➤ Envoyer", "➤ Send")) {
                val question = input.text.toString().trim()

                if (question.isNotBlank()) {
                    addChat(question, true)
                    addChat(financialReply(question), false)
                    build()
                }
            }
        )

        composer.addView(
            secondary(t("Effacer la conversation", "Clear conversation")) {
                AlertDialog.Builder(this)
                    .setTitle(t("Effacer la conversation ?", "Clear chat?"))
                    .setNegativeButton(t("Annuler", "Cancel"), null)
                    .setPositiveButton(t("Effacer", "Clear")) { _, _ ->
                        saveArray("chat_v2", JSONArray())
                        build()
                    }
                    .show()
            }
        )
    }

    private fun chatBubble(
        parent: LinearLayout,
        content: String,
        isUser: Boolean,
    ) {
        val bubble =
            column().apply {
                background =
                    rounded(
                        if (isUser) lightBlue else mint,
                        16,
                    )
                setPadding(dp(12), dp(10), dp(12), dp(10))
            }

        bubble.addView(
            txt(
                if (isUser) t("Vous", "You") else t("Assistant", "Assistant"),
                12f,
                if (isUser) blue else green,
                true,
            )
        )

        bubble.addView(txt(content, 14f, ink))

        parent.addView(
            bubble,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(10)

                if (isUser) {
                    leftMargin = dp(28)
                } else {
                    rightMargin = dp(20)
                }
            },
        )
    }

    private fun addChat(content: String, isUser: Boolean) {
        val array = readArray("chat_v2")

        array.put(
            JSONObject().apply {
                put("text", content)
                put("user", isUser)
                put("date", System.currentTimeMillis())
            }
        )

        saveArray("chat_v2", array)
    }

    private fun financialReply(question: String): String {
        val q = question.lowercase(Locale.ROOT)

        val income = get("income", 4850.0)
        val expenses = get("expenses", 3120.0)
        val debt = get("debt", 0.0)
        val savings = get("liquidSavings", 1000.0)

        val answer =
            when {
                q.contains("bonjour") || q.contains("hello") || q.contains("salut") -> {
                    t(
                        "Bonjour ! Nous pouvons examiner votre budget, vos dettes, votre épargne ou un objectif.",
                        "Hello! We can review your budget, debt, savings or a goal.",
                    )
                }

                q.contains("budget") ||
                    q.contains("dépense") ||
                    q.contains("expense") ||
                    q.contains("spend") -> {
                    t(
                        "Pour améliorer votre budget :\n\n" +
                            "1. Distinguez dépenses fixes, variables et annuelles.\n" +
                            "2. Vérifiez vos relevés.\n" +
                            "3. Identifiez une dépense flexible.\n" +
                            "4. Automatisez une épargne soutenable.\n\n" +
                            "Revenu : ${money(income)}\n" +
                            "Dépenses : ${money(expenses)}\n" +
                            "Marge indicative : ${money(income - expenses)}.",
                        "To improve your budget:\n\n" +
                            "1. Separate fixed, variable and annual expenses.\n" +
                            "2. Review statements.\n" +
                            "3. Identify flexible spending.\n" +
                            "4. Automate affordable savings.\n\n" +
                            "Income: ${money(income)}\n" +
                            "Expenses: ${money(expenses)}\n" +
                            "Indicative surplus: ${money(income - expenses)}.",
                    )
                }

                q.contains("dette") ||
                    q.contains("debt") ||
                    q.contains("crédit") ||
                    q.contains("credit") -> {
                    t(
                        "Listez vos dettes avec leurs taux.\n\n" +
                            "L'avalanche favorise le taux le plus élevé. " +
                            "La boule de neige favorise le plus petit solde.\n\n" +
                            "Vérifiez le coût total avant une consolidation.\n\n" +
                            "Dettes déclarées : ${money(debt)}.",
                        "List debts and interest rates.\n\n" +
                            "Avalanche prioritizes the highest rate. " +
                            "Snowball prioritizes the smallest balance.\n\n" +
                            "Compare total costs before consolidation.\n\n" +
                            "Reported debt: ${money(debt)}.",
                    )
                }

                q.contains("urgence") || q.contains("emergency") || q.contains("coussin") -> {
                    val target = expenses * 3.0

                    t(
                        "Un repère initial est trois mois de dépenses.\n\n" +
                            "Cible indicative : ${money(target)}.\n" +
                            "Liquidités indiquées : ${money(savings)}.\n\n" +
                            "Gardez cette réserve facilement accessible. Adaptez-la à votre stabilité de revenu.",
                        "An initial benchmark is three months of expenses.\n\n" +
                            "Indicative target: ${money(target)}.\n" +
                            "Reported liquid savings: ${money(savings)}.\n\n" +
                            "Keep the reserve accessible and adjust it to income stability.",
                    )
                }

                q.contains("reer") ||
                    q.contains("rrsp") ||
                    q.contains("celi") ||
                    q.contains("tfsa") ||
                    q.contains("fhsa") -> {
                    t(
                        "REER : déduction possible aujourd'hui et retrait normalement imposable.\n\n" +
                            "CELI : cotisation non déductible et retrait généralement non imposable.\n\n" +
                            "CELIAPP : destiné aux premiers acheteurs admissibles.\n\n" +
                            "Comparez le TEMI actuel et futur et vérifiez vos droits auprès de l'ARC.",
                        "RRSP: possible deduction today and generally taxable withdrawals.\n\n" +
                            "TFSA: no contribution deduction and generally tax-free withdrawals.\n\n" +
                            "FHSA: intended for eligible first-time home buyers.\n\n" +
                            "Compare current and future effective tax rates and verify CRA contribution room.",
                    )
                }

                q.contains("maison") ||
                    q.contains("hypothèque") ||
                    q.contains("mortgage") ||
                    q.contains("home") ||
                    q.contains("house") -> {
                    t(
                        "Avant l'achat :\n\n" +
                            "1. Calculez la mise de fonds.\n" +
                            "2. Ajoutez les frais de clôture.\n" +
                            "3. Préservez une réserve d'urgence.\n" +
                            "4. Vérifiez ABD et ATD.\n" +
                            "5. Comparez louer et acheter.\n" +
                            "6. Examinez CELIAPP et RAP si admissibles.",
                        "Before buying:\n\n" +
                            "1. Calculate the down payment.\n" +
                            "2. Include closing costs.\n" +
                            "3. Preserve emergency savings.\n" +
                            "4. Check housing and debt ratios.\n" +
                            "5. Compare renting and buying.\n" +
                            "6. Review FHSA and HBP eligibility.",
                    )
                }

                q.contains("invest") ||
                    q.contains("placement") ||
                    q.contains("fnb") ||
                    q.contains("etf") -> {
                    t(
                        "Avant d'investir :\n\n" +
                            "1. Définissez votre horizon.\n" +
                            "2. Évaluez votre capacité et tolérance au risque.\n" +
                            "3. Diversifiez.\n" +
                            "4. Comparez les frais.\n" +
                            "5. Tenez compte des impôts et de l'inflation.\n\n" +
                            "Les rendements ne sont pas garantis.",
                        "Before investing:\n\n" +
                            "1. Define your time horizon.\n" +
                            "2. Assess risk capacity and tolerance.\n" +
                            "3. Diversify.\n" +
                            "4. Compare fees.\n" +
                            "5. Consider taxes and inflation.\n\n" +
                            "Returns are not guaranteed.",
                    )
                }

                q.contains("retraite") || q.contains("retire") || q.contains("pension") -> {
                    t(
                        "Préparez la retraite à partir du coût de vie souhaité.\n\n" +
                            "Inventoriez les régimes, estimez les impôts futurs et étudiez le risque de séquence des rendements.",
                        "Plan retirement from desired living costs.\n\n" +
                            "Inventory retirement accounts, estimate future taxes and consider sequence-of-returns risk.",
                    )
                }

                q.contains("enfant") ||
                    q.contains("children") ||
                    q.contains("reee") ||
                    q.contains("resp") ||
                    q.contains("famille") ||
                    q.contains("family") -> {
                    t(
                        "Définissez les objectifs familiaux, vérifiez le fonds d'urgence et les protections.\n\n" +
                            "Pour les études admissibles, explorez le REEE et les subventions disponibles.",
                        "Define family goals, review emergency savings and protection.\n\n" +
                            "For eligible education, explore RESPs and available grants.",
                    )
                }

                q.contains("impôt") || q.contains("fiscal") || q.contains("tax") -> {
                    t(
                        "Distinguez taux moyen, taux marginal et taux effectif marginal.\n\n" +
                            "Le cadre CE3D examine crédits, exonérations, déductions, report et division légale des revenus.\n\n" +
                            "Vérifiez les règles auprès de Revenu Québec et de l'ARC.",
                        "Distinguish average, marginal and effective marginal tax rates.\n\n" +
                            "Review credits, exemptions, deductions, deferral and lawful income splitting.\n\n" +
                            "Verify rules with tax authorities.",
                    )
                }

                else -> {
                    t(
                        "Je peux expliquer le budget, les dettes, les placements, le REER, le CELI, le CELIAPP, les hypothèques, la retraite et la fiscalité.\n\n" +
                            "Précisez votre objectif et votre échéance, ou utilisez le questionnaire.",
                        "I can explain budgeting, debt, investing, RRSPs, TFSAs, FHSAs, mortgages, retirement and taxes.\n\n" +
                            "Describe your goal and timeline, or use the questionnaire.",
                    )
                }
            }

        return answer +
            "\n\n" +
            t(
                "Information éducative seulement.",
                "Educational information only.",
            )
    }

    private fun courseTopics(): List<Pair<String, List<Pair<String, String>>>> {
        return listOf(
            Pair(
                "Séance 1 — Démarche intégrée et comportement",
                listOf(
                    Pair(
                        "Planification financière personnelle",
                        "Processus structuré reliant ressources et objectifs de vie. Les décisions touchent plusieurs domaines simultanément.",
                    ),
                    Pair(
                        "Sept domaines",
                        "Aspects légaux, assurance et risques, finances, fiscalité, placements, retraite et succession.",
                    ),
                    Pair(
                        "Six étapes",
                        "1. Relation et mandat.\n2. Information et objectifs.\n3. Analyse.\n4. Recommandations.\n5. Mise en œuvre.\n6. Suivi.",
                    ),
                    Pair(
                        "Grille d'analyse",
                        "Étudier famille, finances, placements, fiscalité, protection, études, retraite et décès. Comparer situation actuelle et objectifs.",
                    ),
                    Pair(
                        "Encadrement au Québec",
                        "LDPSF, AMF, titre réservé de planificateur financier, mandat écrit, rémunération et conflits d'intérêts.",
                    ),
                    Pair(
                        "Cycle de vie",
                        "Emprunt au début, accumulation pendant la vie active et décaissement à la retraite. Le coût de vie guide les objectifs.",
                    ),
                    Pair(
                        "Théorie moderne du portefeuille",
                        "Diversification, frontière efficiente, risque systématique et non systématique. La répartition des actifs précède la sélection des titres.",
                    ),
                    Pair(
                        "Efficience des marchés",
                        "Les prix reflètent rapidement l'information. Les frais, les impôts et le comportement sont des facteurs contrôlables.",
                    ),
                    Pair(
                        "Finance comportementale",
                        "Biais, cadrage, heuristiques, excès de confiance, confirmation, disponibilité, ancrage, statu quo, coûts irrécupérables, aversion à la perte et préférence pour le présent.",
                    ),
                    Pair(
                        "Nudges",
                        "Automatisation de l'épargne, hausse programmée des cotisations, règles écrites et revue annuelle.",
                    ),
                ),
            ),
            Pair(
                "Séance 2 — États financiers personnels",
                listOf(
                    Pair(
                        "Bilan personnel",
                        "Valeur nette = total des actifs − total des passifs, y compris les impôts futurs pertinents.",
                    ),
                    Pair(
                        "Catégories d'actifs",
                        "Actifs liquides et semi-liquides, biens personnels, biens productifs et régimes à imposition différée.",
                    ),
                    Pair(
                        "Droits futurs",
                        "Les droits établis et évaluables de façon fiable peuvent faire partie du bilan. Sinon, les documenter en note.",
                    ),
                    Pair(
                        "Passifs et fiscalité future",
                        "Hypothèques, prêts, comptes payables et impôts latents. Un CELI et un REER de valeur brute égale n'ont pas nécessairement le même pouvoir d'achat.",
                    ),
                    Pair(
                        "Coût de la vie",
                        "Méthode directe : additionner les dépenses.\nMéthode résiduelle : revenu net − épargne vérifiable − capital remboursé.",
                    ),
                    Pair(
                        "Disponibilités",
                        "Revenus nets − coût de vie − engagements fixes.",
                    ),
                    Pair(
                        "Budget",
                        "Revenus, dépenses fixes, dépenses variables, épargne programmée et budget de caisse.",
                    ),
                    Pair(
                        "BUND",
                        "Comparer les utilisations du prochain dollar : dette, fonds d'urgence, subventions et comptes fiscalement avantageux.",
                    ),
                    Pair(
                        "Fonds de résilience",
                        "Repère général : au moins trois mois de dépenses, adapté à la stabilité d'emploi et aux risques.",
                    ),
                    Pair(
                        "Évolution de la valeur nette",
                        "VN finale = VN initiale + épargne + capital remboursé + variation des actifs − nouvelles dettes nettes.",
                    ),
                ),
            ),
            Pair(
                "Séance 3 — Crédit et habitation",
                listOf(
                    Pair(
                        "Bonne et mauvaise dette",
                        "Évaluer l'actif financé, le coût, la durée et la capacité de remboursement.",
                    ),
                    Pair(
                        "Rembourser une dette",
                        "Rendement brut requis en non-enregistré = taux de dette / (1 − TMI), sous hypothèses simplifiées.",
                    ),
                    Pair(
                        "Avalanche et boule de neige",
                        "Avalanche : taux le plus élevé. Boule de neige : plus petit solde. Comparer les coûts avant consolidation.",
                    ),
                    Pair(
                        "Vocabulaire hypothécaire",
                        "Capital, terme, amortissement, prêt ouvert, prêt fermé et pénalités.",
                    ),
                    Pair(
                        "Composition semestrielle",
                        "Taux mensuel équivalent = (1 + taux nominal / 2)^(1/6) − 1.",
                    ),
                    Pair(
                        "Amortissement",
                        "Intérêt = solde × taux périodique.\nCapital = paiement − intérêt.\nNouveau solde = ancien solde − capital.",
                    ),
                    Pair(
                        "Paiements accélérés",
                        "Une demi-mensualité payée 26 fois par an correspond à 13 mensualités, selon les conditions du contrat.",
                    ),
                    Pair(
                        "Fixe et variable",
                        "Comparer stabilité des paiements, incertitude, taux et pénalités contractuelles.",
                    ),
                    Pair(
                        "Mise de fonds",
                        "Examiner apport minimal, assurance prêt, frais de clôture, liquidités et admissibilité. Les seuils réglementaires doivent être vérifiés.",
                    ),
                    Pair(
                        "ABD et ATD",
                        "ABD = charges habitation / revenu brut.\nATD = (habitation + autres paiements de dettes) / revenu brut.",
                    ),
                    Pair(
                        "Louer ou acheter",
                        "Comparer les coûts non récupérables. Le capital remboursé augmente la valeur nette.",
                    ),
                    Pair(
                        "Automobile",
                        "Comparer comptant, financement et location selon le coût total de possession.",
                    ),
                ),
            ),
            Pair(
                "Séance 4 — Fiscalité personnelle",
                listOf(
                    Pair(
                        "Déclarations au Québec",
                        "Déclarations fédérale et québécoise. Barèmes, crédits et règles distincts.",
                    ),
                    Pair(
                        "Calcul de l'impôt",
                        "Revenu total − déductions = revenu net.\nRevenu net − autres déductions = revenu imposable.\nImpôt selon barèmes − crédits + ajustements = impôt payable.",
                    ),
                    Pair(
                        "Déductions et crédits",
                        "Une déduction réduit le revenu. Un crédit non remboursable réduit l'impôt jusqu'à zéro. Un crédit remboursable peut être versé même sans impôt.",
                    ),
                    Pair(
                        "TM, TMI et TEMI",
                        "TM : impôt total / revenu total.\nTMI : impôt sur le prochain dollar.\nTEMI : impôt additionnel et prestations perdues.",
                    ),
                    Pair(
                        "Courbes de Laferrière",
                        "Illustrent les effets combinés de l'impôt et des pertes de prestations.",
                    ),
                    Pair(
                        "Revenus de placement",
                        "Intérêts, dividendes canadiens et gains en capital ont des traitements distincts.",
                    ),
                    Pair(
                        "Gains en capital",
                        "Gain = produit de disposition − PBR − frais. Vérifier les règles d'inclusion de l'année fiscale.",
                    ),
                    Pair(
                        "Pertes apparentes",
                        "Des règles peuvent refuser temporairement une perte lors du rachat de biens identiques dans la période visée.",
                    ),
                    Pair(
                        "Fractionnement",
                        "REER de conjoint, pension admissible, partage RRQ et prêts prescrits selon les conditions applicables.",
                    ),
                    Pair(
                        "CE3D",
                        "Crédits, Exonérer, Déduire, Différer, Diviser.",
                    ),
                ),
            ),
            Pair(
                "Séance 5 — Régimes enregistrés",
                listOf(
                    Pair(
                        "REER",
                        "Droits liés au revenu gagné, plafond annuel, facteur d'équivalence et droits reportés. Vérifier l'avis de cotisation.",
                    ),
                    Pair(
                        "Fiscalité du REER",
                        "Cotisation potentiellement déductible, croissance à l'abri de l'impôt courant et retrait généralement imposable.",
                    ),
                    Pair(
                        "REER de conjoint",
                        "Le cotisant utilise ses droits. Des règles d'attribution s'appliquent à certains retraits.",
                    ),
                    Pair(
                        "RAP et REEP",
                        "Retraits admissibles pour habitation ou études selon des limites et obligations de remboursement spécifiques.",
                    ),
                    Pair(
                        "REER ou CELI",
                        "Comparer le TEMI de cotisation et de retrait, les besoins de liquidité et les prestations.",
                    ),
                    Pair(
                        "CELI",
                        "Cotisations non déductibles, retraits généralement non imposables et droits de retrait récupérés l'année civile suivante.",
                    ),
                    Pair(
                        "CELIAPP",
                        "Régime pour première habitation admissible. Déduction potentielle et retrait admissible libre d'impôt.",
                    ),
                    Pair(
                        "REEE",
                        "Épargne-études, SCEE, BEC et IQEE. Les subventions dépendent de l'admissibilité.",
                    ),
                    Pair(
                        "REEI",
                        "Régime lié à l'admissibilité au CIPH. Subventions, bons et règles de remboursement.",
                    ),
                    Pair(
                        "BUND fiscal",
                        "Dette coûteuse, appariement employeur, REEE, REEI, CELIAPP, REER/CELI et autres usages du surplus.",
                    ),
                    Pair(
                        "Chronologie de vie",
                        "Les priorités financières changent avec l'emploi, le logement, les enfants, la retraite et le décaissement.",
                    ),
                ),
            ),
            Pair(
                "Séance 6 — Placements après impôts",
                listOf(
                    Pair(
                        "Hypothèses de projection",
                        "Utiliser des hypothèses cohérentes de rendement, inflation et risque plutôt que prédire le marché.",
                    ),
                    Pair(
                        "Frais",
                        "Rendement après frais = rendement brut − frais de gestion − autres frais.",
                    ),
                    Pair(
                        "Inflation",
                        "Rendement réel = (1 + rendement nominal) / (1 + inflation) − 1.",
                    ),
                    Pair(
                        "Fiscalité des placements",
                        "Comparer intérêts, dividendes et gains en capital selon le type de compte et les règles applicables.",
                    ),
                    Pair(
                        "Volatilité après impôts",
                        "Le risque réel concerne le pouvoir d'achat net, pas uniquement le solde affiché.",
                    ),
                    Pair(
                        "Fonds communs et FNB",
                        "Comparer frais, mandat, indice, rotation, distributions et diversification.",
                    ),
                    Pair(
                        "Profil d'investisseur",
                        "Trois dimensions : capacité à supporter les pertes, tolérance psychologique et besoin de rendement.",
                    ),
                    Pair(
                        "Phases de vie",
                        "Accumulation, consolidation et décaissement.",
                    ),
                    Pair(
                        "Risque de séquence",
                        "Les pertes précoces sont particulièrement importantes quand des retraits réguliers sont effectués.",
                    ),
                    Pair(
                        "Politique de placement",
                        "Décrire objectifs, horizon, profil, répartition, liquidités, contraintes et rééquilibrage.",
                    ),
                    Pair(
                        "Rééquilibrage",
                        "À date fixe, à seuil ou hybride, en considérant les frais et impôts.",
                    ),
                    Pair(
                        "Levier",
                        "L'emprunt pour investir amplifie gains et pertes alors que la dette reste exigible.",
                    ),
                    Pair(
                        "Diversification",
                        "Répartition géographique, sectorielle, par catégorie et par devise.",
                    ),
                    Pair(
                        "Achats périodiques",
                        "Contributions régulières favorisant la discipline sans garantie de rendement.",
                    ),
                    Pair(
                        "Revue annuelle",
                        "Vérifier répartition, droits fiscaux, frais, PBR, objectifs et bénéficiaires.",
                    ),
                ),
            ),
        )
    }

    private fun educationPage() {
        val intro = card(body, mint)

        heading(
            intro,
            t("📚 Éducation financière", "📚 Financial education"),
        )

        intro.addView(
            txt(
                "FINA 30210 — Cahier complet de révision, séances 1 à 6",
                14f,
                darkGreen,
                true,
            )
        )

        intro.addView(
            txt(
                t(
                    "Synthèse pédagogique des notions du document fourni. Vérifiez les règles fiscales et plafonds actuels.",
                    "Educational summary of the supplied document. Verify current tax rules and limits.",
                ),
                12f,
                gray,
            )
        )

        courseTopics().forEachIndexed { index, session ->
            val box = card()

            heading(box, session.first)

            box.addView(
                txt(
                    "${session.second.size} " + t("notions", "topics"),
                    12f,
                    gray,
                )
            )

            box.addView(
                button(t("Ouvrir la séance", "Open lesson")) {
                    lessonDialog(index)
                }
            )
        }

        val formulas = card(body, yellow)

        heading(
            formulas,
            t("🧮 Formulaire synthèse", "🧮 Formula reference"),
        )

        formulas.addView(
            txt(
                "Valeur nette = actif − passif\n\n" +
                    "Coût de vie résiduel = revenu net − épargne − capital remboursé\n\n" +
                    "Disponibilités = revenus nets − coût de vie − engagements fixes\n\n" +
                    "VN finale = VN initiale + épargne + capital remboursé + variation actifs − nouvelles dettes nettes\n\n" +
                    "Rendement brut requis = taux dette / (1 − TMI)\n\n" +
                    "Taux hypothécaire mensuel = (1 + taux nominal / 2)^(1/6) − 1\n\n" +
                    "ABD = charges habitation / revenu brut\n\n" +
                    "ATD = (habitation + autres dettes) / revenu brut\n\n" +
                    "TM = impôt total / revenu total\n\n" +
                    "Gain en capital = produit − PBR − frais\n\n" +
                    "Rendement réel = (1 + nominal) / (1 + inflation) − 1\n\n" +
                    "Rendement net sur intérêts = r × (1 − TMI)",
                13f,
                ink,
            )
        )

        formulas.addView(
            button(t("Ouvrir les calculateurs", "Open calculators")) {
                go(10)
            }
        )
    }

    private fun lessonDialog(index: Int) {
        val session = courseTopics().getOrNull(index) ?: return

        val content =
            column().apply {
                setPadding(dp(18), dp(14), dp(18), dp(14))
            }

        session.second.forEach { topic ->
            content.addView(txt(topic.first, 18f, darkGreen, true))

            content.addView(txt(topic.second, 14f, ink))

            divider(content)
        }

        val scroll = ScrollView(this)
        scroll.addView(content)

        AlertDialog.Builder(this)
            .setTitle(session.first)
            .setView(scroll)
            .setPositiveButton(t("Fermer", "Close"), null)
            .show()
    }

    private fun payment(
        amount: Double,
        rate: Double,
        periods: Double,
    ): Double {
        require(amount > 0 && periods > 0)

        return if (rate == 0.0) {
            amount / periods
        } else {
            amount * rate / (1.0 - (1.0 + rate).pow(-periods))
        }
    }

    private fun simulate(
        amount: Double,
        periodicRate: Double,
        installment: Double,
        frequency: Int,
    ): LoanResult {
        var balance = amount
        var totalInterest = 0.0
        val rows = mutableListOf<LoanRow>()

        for (n in 1..(frequency * 100)) {
            val interest = balance * periodicRate
            val paid = min(installment, balance + interest)
            val principal = paid - interest

            require(principal > 0.000001)

            balance = (balance - principal).coerceAtLeast(0.0)
            totalInterest += interest

            rows.add(
                LoanRow(
                    n,
                    (n - 1) / frequency + 1,
                    paid,
                    interest,
                    principal,
                    balance,
                )
            )

            if (balance < 0.005) {
                return LoanResult(
                    installment,
                    totalInterest,
                    frequency,
                    rows,
                )
            }
        }

        throw IllegalArgumentException("Payment too small")
    }

    private fun mortgage(
        amount: Double,
        annualRate: Double,
        years: Double,
        mode: Int,
    ): LoanResult {
        val frequency =
            when (mode) {
                0 -> 12
                1 -> 24
                else -> 26
            }

        val periodic = (1.0 + annualRate / 2.0).pow(2.0 / frequency) - 1.0

        val monthlyRate = (1.0 + annualRate / 2.0).pow(2.0 / 12.0) - 1.0

        val monthly =
            payment(
                amount,
                monthlyRate,
                years * 12.0,
            )

        val installment =
            if (mode == 3) {
                monthly / 2.0
            } else {
                payment(
                    amount,
                    periodic,
                    years * frequency,
                )
            }

        return simulate(amount, periodic, installment, frequency)
    }

    private fun calculatorsPage() {
        val intro = card(body, lightBlue)

        heading(
            intro,
            t("🧮 Calculateurs financiers", "🧮 Financial calculators"),
        )

        intro.addView(
            txt(
                t(
                    "Simulations éducatives sans garantie de résultat.",
                    "Educational simulations with no guaranteed outcome.",
                ),
                12f,
                gray,
            )
        )

        val options =
            if (french) {
                arrayOf(
                    "Hypothèque",
                    "Carte de crédit",
                    "Prêt personnel",
                    "Rendement réel",
                    "Intérêt composé",
                    "Fonds d'urgence",
                )
            } else {
                arrayOf(
                    "Mortgage",
                    "Credit card",
                    "Personal loan",
                    "Real return",
                    "Compound interest",
                    "Emergency fund",
                )
            }

        val menu = card()
        val spinner = Spinner(this)

        spinner.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                options,
            )

        menu.addView(spinner)

        val form = card()
        val output = card()

        fun render(position: Int) {
            form.removeAllViews()
            output.removeAllViews()

            when (position) {
                0 -> mortgageForm(form, output)
                1 -> creditForm(form, output)
                2 -> personalLoanForm(form, output)
                3 -> inflationForm(form, output)
                4 -> compoundForm(form, output)
                5 -> emergencyForm(form, output)
            }
        }

        spinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    render(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        render(0)
    }

    private fun mortgageForm(
        form: LinearLayout,
        output: LinearLayout,
    ) {
        heading(form, t("🏠 Hypothèque", "🏠 Mortgage"))

        val amount =
            field(
                form,
                t("Montant emprunté", "Loan amount"),
                "350000",
                true,
            )

        val rate =
            field(
                form,
                t("Taux annuel (%)", "Annual rate (%)"),
                "4.5",
                true,
            )

        val years =
            field(
                form,
                t("Amortissement (années)", "Amortization (years)"),
                "25",
                true,
            )

        form.addView(
            button(t("Calculer", "Calculate")) {
                output.removeAllViews()

                try {
                    val a = read(amount)
                    val r = read(rate) / 100.0
                    val y = read(years)

                    require(a > 0 && r in 0.0..1.0 && y > 0 && y <= 50)

                    val loans =
                        (0..3).map {
                            mortgage(a, r, y, it)
                        }

                    val labels =
                        if (french) {
                            listOf(
                                "Mensuel",
                                "Deux fois par mois",
                                "Aux deux semaines",
                                "Aux deux semaines accéléré",
                            )
                        } else {
                            listOf(
                                "Monthly",
                                "Twice monthly",
                                "Biweekly",
                                "Accelerated biweekly",
                            )
                        }

                    loans.forEachIndexed { index, loan ->
                        heading(output, labels[index])

                        stat(
                            output,
                            t("Paiement", "Payment"),
                            money(loan.payment),
                        )

                        output.addView(
                            txt(t("Intérêts totaux : ", "Total interest: ") + money(loan.interest))
                        )

                        output.addView(
                            txt(
                                t("Nombre de versements : ", "Number of payments: ") +
                                    loan.rows.size
                            )
                        )

                        divider(output)
                    }

                    stat(
                        output,
                        t(
                            "Économie d'intérêts accélérée",
                            "Accelerated interest savings",
                        ),
                        money(loans[0].interest - loans[3].interest),
                    )

                    heading(
                        output,
                        t(
                            "📋 Tableau d'amortissement",
                            "📋 Amortization schedule",
                        ),
                    )

                    val frequency = Spinner(this)

                    frequency.adapter =
                        ArrayAdapter(
                            this,
                            android.R.layout.simple_spinner_dropdown_item,
                            labels,
                        )

                    output.addView(frequency)

                    val yearSelect = Spinner(this)
                    output.addView(yearSelect)

                    val tableScroll = HorizontalScrollView(this)
                    val table = column()

                    tableScroll.addView(table)
                    output.addView(tableScroll)

                    fun renderTable() {
                        table.removeAllViews()

                        val loan = loans[frequency.selectedItemPosition.coerceIn(0, 3)]

                        val selectedYear = yearSelect.selectedItemPosition

                        fun tableRow(
                            cells: List<String>,
                            header: Boolean,
                        ) {
                            val line = row()

                            line.setBackgroundColor(if (header) green else pale)

                            cells.forEachIndexed { index, content ->
                                val cell =
                                    txt(
                                        content,
                                        12f,
                                        if (header) white else ink,
                                        header,
                                    )

                                cell.setPadding(
                                    dp(8),
                                    dp(9),
                                    dp(8),
                                    dp(9),
                                )

                                line.addView(
                                    cell,
                                    LinearLayout.LayoutParams(
                                        dp(if (index == 0) 46 else 120),
                                        -2,
                                    ),
                                )
                            }

                            table.addView(line)
                        }

                        tableRow(
                            listOf(
                                "#",
                                t("Paiement", "Payment"),
                                t("Intérêt", "Interest"),
                                t("Capital", "Principal"),
                                t("Solde", "Balance"),
                            ),
                            true,
                        )

                        val visibleRows =
                            if (selectedYear == 0) {
                                loan.rows.take(120)
                            } else {
                                loan.rows.filter {
                                    it.year == selectedYear
                                }
                            }

                        visibleRows.forEach {
                            tableRow(
                                listOf(
                                    it.number.toString(),
                                    money(it.payment),
                                    money(it.interest),
                                    money(it.principal),
                                    money(it.balance),
                                ),
                                false,
                            )
                        }
                    }

                    frequency.onItemSelectedListener =
                        object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: AdapterView<*>?,
                                view: View?,
                                position: Int,
                                id: Long,
                            ) {
                                val lastYear = loans[position].rows.last().year

                                val yearsList =
                                    mutableListOf(
                                        t(
                                            "120 premiers versements",
                                            "First 120 payments",
                                        )
                                    )

                                for (year in 1..lastYear) {
                                    yearsList.add(t("Année $year", "Year $year"))
                                }

                                yearSelect.adapter =
                                    ArrayAdapter(
                                        this@MainActivity,
                                        android.R.layout.simple_spinner_dropdown_item,
                                        yearsList,
                                    )

                                renderTable()
                            }

                            override fun onNothingSelected(parent: AdapterView<*>?) {}
                        }

                    yearSelect.onItemSelectedListener =
                        object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: AdapterView<*>?,
                                view: View?,
                                position: Int,
                                id: Long,
                            ) {
                                renderTable()
                            }

                            override fun onNothingSelected(parent: AdapterView<*>?) {}
                        }

                    output.addView(
                        txt(
                            t(
                                "Taux fixe constant et composition semestrielle canadienne. Frais, renouvellements et assurances exclus.",
                                "Constant fixed rate and Canadian semiannual compounding. Fees, renewals and insurance excluded.",
                            ),
                            12f,
                            gray,
                        )
                    )
                } catch (e: Exception) {
                    output.addView(
                        txt(
                            t("Vérifiez les valeurs.", "Check your values."),
                            14f,
                            red,
                        )
                    )
                }
            }
        )
    }

    private fun creditForm(
        form: LinearLayout,
        output: LinearLayout,
    ) {
        heading(
            form,
            t("💳 Carte de crédit", "💳 Credit card"),
        )

        val amount =
            field(
                form,
                t("Solde", "Balance"),
                "5000",
                true,
            )

        val rate =
            field(
                form,
                t("Taux annuel (%)", "Annual rate (%)"),
                "19.99",
                true,
            )

        val monthly =
            field(
                form,
                t("Paiement mensuel", "Monthly payment"),
                "200",
                true,
            )

        form.addView(
            button(t("Calculer", "Calculate")) {
                output.removeAllViews()

                try {
                    val a = read(amount)
                    val r = read(rate) / 1200.0
                    val p = read(monthly)

                    require(a > 0 && r >= 0 && p > a * r)

                    val loan = simulate(a, r, p, 12)

                    stat(
                        output,
                        t("Intérêts totaux", "Total interest"),
                        money(loan.interest),
                        red,
                    )

                    stat(
                        output,
                        t("Nombre de mois", "Number of months"),
                        loan.rows.size.toString(),
                    )

                    stat(
                        output,
                        t("Total payé", "Total paid"),
                        money(a + loan.interest),
                    )
                } catch (e: Exception) {
                    output.addView(
                        txt(
                            t(
                                "Le paiement doit dépasser les intérêts.",
                                "Payment must exceed interest.",
                            ),
                            13f,
                            red,
                        )
                    )
                }
            }
        )
    }

    private fun personalLoanForm(
        form: LinearLayout,
        output: LinearLayout,
    ) {
        heading(
            form,
            t("💵 Prêt personnel", "💵 Personal loan"),
        )

        val amount =
            field(
                form,
                t("Montant", "Amount"),
                "10000",
                true,
            )

        val rate =
            field(
                form,
                t("Taux annuel (%)", "Annual rate (%)"),
                "8",
                true,
            )

        val years =
            field(
                form,
                t("Durée (années)", "Duration (years)"),
                "5",
                true,
            )

        form.addView(
            button(t("Calculer", "Calculate")) {
                output.removeAllViews()

                try {
                    val a = read(amount)
                    val r = read(rate) / 1200.0
                    val y = read(years)

                    require(a > 0 && r >= 0 && y > 0 && y <= 50)

                    val p = payment(a, r, y * 12)
                    val loan = simulate(a, r, p, 12)

                    stat(
                        output,
                        t("Paiement mensuel", "Monthly payment"),
                        money(p),
                    )

                    stat(
                        output,
                        t("Intérêts totaux", "Total interest"),
                        money(loan.interest),
                    )
                } catch (e: Exception) {
                    output.addView(txt(t("Valeurs invalides", "Invalid values"), 13f, red))
                }
            }
        )
    }

    private fun inflationForm(
        form: LinearLayout,
        output: LinearLayout,
    ) {
        heading(
            form,
            t("📈 Rendement réel", "📈 Real return"),
        )

        val nominal =
            field(
                form,
                t("Rendement nominal (%)", "Nominal return (%)"),
                "6.5",
                true,
            )

        val inflation =
            field(
                form,
                t("Inflation (%)", "Inflation (%)"),
                "2.5",
                true,
            )

        val fees =
            field(
                form,
                t("Frais annuels (%)", "Annual fees (%)"),
                "0.5",
                true,
            )

        form.addView(
            button(t("Calculer", "Calculate")) {
                output.removeAllViews()

                try {
                    val n = read(nominal) / 100.0
                    val i = read(inflation) / 100.0
                    val f = read(fees) / 100.0

                    require(i > -1 && f >= 0)

                    val afterFees = n - f
                    val real = (1 + afterFees) / (1 + i) - 1

                    stat(
                        output,
                        t("Rendement après frais", "Return after fees"),
                        "${fmt(afterFees * 100)} %",
                    )

                    stat(
                        output,
                        t("Rendement réel", "Real return"),
                        "${fmt(real * 100)} %",
                    )
                } catch (e: Exception) {
                    output.addView(txt(t("Valeurs invalides", "Invalid values"), 13f, red))
                }
            }
        )
    }

    private fun compoundForm(
        form: LinearLayout,
        output: LinearLayout,
    ) {
        heading(
            form,
            t("🌱 Intérêt composé", "🌱 Compound interest"),
        )

        val principal =
            field(
                form,
                t("Capital initial", "Starting principal"),
                "5000",
                true,
            )

        val monthly =
            field(
                form,
                t("Contribution mensuelle", "Monthly contribution"),
                "200",
                true,
            )

        val rate =
            field(
                form,
                t("Rendement annuel hypothétique (%)", "Hypothetical annual return (%)"),
                "5",
                true,
            )

        val years =
            field(
                form,
                t("Années", "Years"),
                "10",
                true,
            )

        form.addView(
            button(t("Calculer", "Calculate")) {
                output.removeAllViews()

                try {
                    val p = read(principal)
                    val m = read(monthly)
                    val r = read(rate) / 1200.0
                    val y = read(years)

                    require(p >= 0 && m >= 0 && r > -1 && y > 0 && y <= 100)

                    val months = (y * 12).toInt()
                    var total = p

                    repeat(months) {
                        total = total * (1 + r) + m
                    }

                    val contributions = p + m * months

                    stat(
                        output,
                        t("Valeur projetée", "Projected value"),
                        money(total),
                    )

                    stat(
                        output,
                        t("Contributions", "Contributions"),
                        money(contributions),
                    )

                    stat(
                        output,
                        t("Croissance hypothétique", "Hypothetical growth"),
                        money(total - contributions),
                    )

                    output.addView(
                        txt(
                            t(
                                "Projection illustrative sans rendement garanti.",
                                "Illustrative projection without guaranteed returns.",
                            ),
                            12f,
                            gray,
                        )
                    )
                } catch (e: Exception) {
                    output.addView(txt(t("Valeurs invalides", "Invalid values"), 13f, red))
                }
            }
        )
    }

    private fun emergencyForm(
        form: LinearLayout,
        output: LinearLayout,
    ) {
        heading(
            form,
            t("🛡 Fonds d'urgence", "🛡 Emergency fund"),
        )

        val expenses =
            field(
                form,
                t("Dépenses mensuelles", "Monthly expenses"),
                get("expenses", 3120.0).toString(),
                true,
            )

        val months =
            field(
                form,
                t("Nombre de mois", "Number of months"),
                "3",
                true,
            )

        val saved =
            field(
                form,
                t("Épargne liquide", "Liquid savings"),
                get("liquidSavings", 1000.0).toString(),
                true,
            )

        form.addView(
            button(t("Calculer", "Calculate")) {
                output.removeAllViews()

                try {
                    val e = read(expenses)
                    val m = read(months)
                    val s = read(saved)

                    require(e >= 0 && m > 0 && s >= 0)

                    val target = e * m

                    stat(
                        output,
                        t("Réserve cible", "Target reserve"),
                        money(target),
                    )

                    stat(
                        output,
                        t("Montant à compléter", "Remaining amount"),
                        money((target - s).coerceAtLeast(0.0)),
                    )

                    progress(output, s, target)

                    output.addView(
                        txt(
                            t(
                                "Adaptez ce repère à la stabilité de votre revenu.",
                                "Adjust this benchmark to income stability.",
                            ),
                            12f,
                            gray,
                        )
                    )
                } catch (e: Exception) {
                    output.addView(txt(t("Valeurs invalides", "Invalid values"), 13f, red))
                }
            }
        )
    }

    private fun wealthPage() {
        val intro = card(body, mint)

        heading(
            intro,
            t("📈 Mon patrimoine", "📈 My net worth"),
        )

        intro.addView(
            txt(
                t(
                    "Valeur nette = actifs − dettes − estimation des impôts futurs pertinents.",
                    "Net worth = assets − debts − relevant estimated future taxes.",
                ),
                13f,
                gray,
            )
        )

        val form = card()

        val assets =
            field(
                form,
                t("Valeur des actifs", "Asset value"),
                get("assets", 0.0).toString(),
                true,
            )

        val debt =
            field(
                form,
                t("Total des dettes", "Total debt"),
                get("debt", 0.0).toString(),
                true,
            )

        val taxes =
            field(
                form,
                t("Impôts futurs estimés", "Estimated future taxes"),
                get("futureTax", 0.0).toString(),
                true,
            )

        form.addView(
            button(t("Enregistrer", "Save")) {
                try {
                    val a = read(assets)
                    val d = read(debt)
                    val f = read(taxes)

                    require(a >= 0 && d >= 0 && f >= 0)

                    put("assets", a)
                    put("debt", d)
                    put("futureTax", f)

                    build()
                } catch (e: Exception) {
                    toast(t("Montants invalides", "Invalid amounts"))
                }
            }
        )

        val result = card()

        heading(
            result,
            t("Valeur nette estimée", "Estimated net worth"),
        )

        val net = get("assets") - get("debt") - get("futureTax")

        stat(
            result,
            t("Valeur nette", "Net worth"),
            money(net),
            if (net >= 0) green else red,
        )

        result.addView(
            secondary(t("Discuter de mon patrimoine", "Discuss my net worth")) {
                go(8)
            }
        )
    }

    private fun settingsPage() {
        val intro = card(body, mint)

        heading(
            intro,
            t("⚙ Paramètres", "⚙ Settings"),
        )

        val language = card()

        heading(language, t("Langue", "Language"))

        language.addView(
            button(if (french) "English" else "Français") {
                french = !french
                prefs.edit().putBoolean("fr", french).apply()
                build()
            }
        )

        val privacy = card()

        heading(
            privacy,
            t("🔒 Confidentialité", "🔒 Privacy"),
        )

        privacy.addView(
            txt(
                t(
                    "Cette version conserve ses données dans les préférences locales de l'application. L'assistant conversationnel fonctionne hors ligne, sans connexion à un modèle IA externe.",
                    "This version stores data in local app preferences. The conversational assistant works offline without an external AI model.",
                ),
                13f,
                gray,
            )
        )

        val sources = card()

        heading(
            sources,
            t("📚 Sources pédagogiques", "📚 Educational sources"),
        )

        sources.addView(
            txt(
                "FINA 30210 — Cahier complet de révision, séances 1 à 6.\n\n" +
                    t(
                        "Synthèse pédagogique du document fourni. Les règles et plafonds doivent être revérifiés.",
                        "Educational summary of the supplied document. Rules and limits must be rechecked.",
                    ),
                13f,
                gray,
            )
        )

        val reset = card()

        heading(
            reset,
            t("Gestion des données", "Data management"),
        )

        reset.addView(
            secondary(t("Effacer la conversation", "Clear conversation")) {
                AlertDialog.Builder(this)
                    .setTitle(t("Effacer la conversation ?", "Clear chat?"))
                    .setNegativeButton(t("Annuler", "Cancel"), null)
                    .setPositiveButton(t("Effacer", "Clear")) { _, _ ->
                        saveArray("chat_v2", JSONArray())
                        build()
                    }
                    .show()
            }
        )

        reset.addView(
            button(
                t("Réinitialiser l'application", "Reset application"),
                red,
            ) {
                AlertDialog.Builder(this)
                    .setTitle(t("Tout effacer ?", "Delete everything?"))
                    .setMessage(
                        t(
                            "Toutes les données locales seront effacées.",
                            "All local data will be deleted.",
                        )
                    )
                    .setNegativeButton(t("Annuler", "Cancel"), null)
                    .setPositiveButton(t("Effacer", "Delete")) { _, _ ->
                        prefs.edit().clear().apply()
                        french = true
                        page = 0
                        build()
                    }
                    .show()
            }
        )
    }
}
