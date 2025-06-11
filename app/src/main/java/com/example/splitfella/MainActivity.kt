package com.example.splitfella

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.splitfella.ui.theme.SplitFellaTheme
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.serialization.Serializable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import kotlin.math.abs




class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SplitFellaTheme {
                Scaffold( modifier = Modifier.fillMaxSize() ) { innerPadding ->
                    val context = applicationContext
                    val users = remember { mutableStateListOf<User>() }
                    val events = remember { mutableStateListOf<Event>() }

                    LaunchedEffect(true) {
                        users.addAll(DataStoreManager.loadUsers(context))
                        events.addAll(DataStoreManager.loadEvents(context))
                    }

                    App(
                        modifier = Modifier.padding(innerPadding),
                        users = users,
                        events = events,
                        context = context
                    )
                }
            }
        }
    }
}

@Serializable
data class User(val name: String, val id: Int, val debts: MutableMap<Int, Float> = mutableMapOf<Int, Float>()){
    fun simpleDebtSummary(users: List<User>, events: List<Event>, currencies: MutableMap<String, Float>): String{
        var result = ""
        var debt: Float

        users.forEach{
            if(it.id!=id) {
                debt = 0f
                for (e in events) {
                    if (it.id == e.payerID) {
                        debt += (e.values[id] ?: 0f) * (currencies[e.currency] ?: 1f)
                    }
                }
                result += "   ${it.name}: ${debt}\n"
            }
        }
        return result
    }
    fun extractDebts(users: List<User>, events: List<Event>, currecies: MutableMap<String, Float>): String{
        debts.clear()
        users.forEach{ u ->
            debts[u.id] = 0f
            if(u.id!=id){
                events.forEach{ e ->
                    if(e.payerID == u.id){
                        debts[u.id] = debts.getOrDefault(u.id, 0f) + ((e.values[id] ?: 0f) * (currecies[e.currency] ?: 1f))
                    }
                }
            }
        }
        return debts.toString()
    }
    fun debtsToString(users: List<User>): String{
        var result = "${name}:"
        debts.forEach{(u2ID, v) ->
            if(u2ID!=id) {
                result += "\n   ${users.find { it.id == u2ID }?.name ?: "Nie znaleziono"}: ${v}zł"
            }
        }
        return result
    }

}

@Serializable
data class Event(val name:String, val values: MutableMap<Int, Float> = mutableMapOf(), val payerID: Int, val date:String = "", val currency: String = "PLN"){
    fun print(users: List<User>, addTab: Boolean = false): String{
        var userSummary= ""
        values.entries.forEach{
            val currUserID = it
            userSummary += "\n${if(addTab)" " else ""}  ${users.find{it.id == currUserID.key}?.name ?: "Nieznaleziono"}: ${it.value} $currency ${if(payerID==currUserID.key) "(płaci)" else ""}"
        }
        return("${if(addTab)" " else ""}${name}    ${date}${userSummary}\n")
    }
}

fun summarizeDebts(users: SnapshotStateList<User>, events: SnapshotStateList<Event>, currecies: MutableMap<String, Float>){
    users.forEach{ u->
        u.extractDebts(users, events, currecies)
    }
    users.forEach{ u->
        users.forEach{ u2 ->
            if((u.debts[u2.id] ?: 0f) > 0f){
                if((u2.debts[u.id] ?: 0f) > 0f){
                    if((u.debts[u2.id] ?: 0f) > (u2.debts[u.id] ?: 0f)){
                        u.debts[u2.id] = u.debts.getOrDefault(u2.id, 0f) - u2.debts.getOrDefault(u.id, 0f)
                        u2.debts[u.id] = 0f
                    }
                    else{
                        u2.debts[u.id] = u2.debts.getOrDefault(u.id, 0f) - u.debts.getOrDefault(u2.id, 0f)
                        u.debts[u2.id] = 0f
                    }
                }
            }
        }
    }
    users.forEach{ u ->
        users.forEach{ u2 ->
            val a = u.debts[u2.id] ?: 0f
            if(a > 0f) {
                u2.debts.forEach { (u3ID, a2) ->
                    if(a<a2 && u3ID != u.id && u3ID != u2.id){
                        val a3 = u.debts.getOrDefault(u3ID, 0f)
                        if(a3 > 0f){
                            u.debts[u3ID] = u.debts.getOrDefault(u3ID, 0f) + u.debts.getOrDefault(u2.id, 0f)
                            u2.debts[u3ID] = u2.debts.getOrDefault(u3ID, 0f) - u.debts.getOrDefault(u2.id, 0f)
                            u.debts[u2.id] = 0f
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MyAlert(title: String, text: String, showDialog: Boolean, onDismiss: () -> Unit) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            },
            title = { Text(title) },
            text = { Text(text) }
        )
    }
}


@Composable
fun App(modifier: Modifier = Modifier,
        users: SnapshotStateList<User>,
        events: SnapshotStateList<Event>,
        context: android.content.Context
        ) {

    val coroutineScope = rememberCoroutineScope()

    var screen by remember { mutableIntStateOf(1)}

    var newUserName by remember {mutableStateOf("")}

    var showAlert by remember { mutableStateOf(false)}
    var alertTitle by remember { mutableStateOf("Alert")}
    var alertText by remember { mutableStateOf("test alert")}
    var alertDismiss by remember { mutableStateOf({showAlert=false}) }

    MyAlert(alertTitle, alertText, showAlert, alertDismiss)

    val currencies = remember { mutableStateMapOf("PLN" to 1f)}
    var currCurrency by remember { mutableStateOf("PLN") }

    fun setAlert( text: String, title: String = "Alert", dismissFoo: () -> Unit = {showAlert=false}){
        alertTitle = title
        alertText = text
        showAlert = true
        alertDismiss = dismissFoo
    }

    when(screen){
        0 -> {
            Column(
                modifier = modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Top
            ){
                Button(onClick = {
                    screen = 1
                }){
                    Text("Dodawanie płatności")
                }
                Button(onClick = {
                    screen = 3
                }){
                    Text("Podsumowanie długów")
                }
                Spacer(modifier = Modifier.height(30.dp))
                Button(onClick = {
                    screen = 2
                }){
                    Text("Lista użytkowników")
                }
                Spacer(modifier = Modifier.height(30.dp))
                Button(onClick = {
                    screen = 4
                }){
                    Text("Kursy walut")
                }


            }
        }
        1 -> {
            var isSeperateValues by remember{mutableStateOf(false)}
            val userValues = remember { mutableStateListOf<String>() }
            users.forEach(){
                userValues.add("")
            }
            var expanded by remember { mutableStateOf(false) }
            var expanded2 by remember { mutableStateOf(false) }
            var expanded3 by remember { mutableStateOf(false) }
            var payer by remember { mutableIntStateOf(-1) }
            var eventName by remember {mutableStateOf("Obiad - Rzym")}
            var fullPrice by remember {mutableStateOf("")}

            Column(
                modifier = modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Top
            ) {
                TextField(
                    value = eventName,
                    onValueChange = {eventName = it},
                    label = {Text("Nazwa zdarzenia")}
                )
                Row(verticalAlignment = Alignment.CenterVertically){
                    Text("osobne wartości  ")
                    Switch(
                        checked = isSeperateValues,
                        onCheckedChange = {
                            isSeperateValues = it
                            if(isSeperateValues){
                                val equalSplit = 100f/users.size
                                users.forEachIndexed{index, value ->
                                    userValues[index] = equalSplit.toString()
                                }
                            }
                            else{
                                users.forEachIndexed{index, value ->
                                    userValues[index] = ""
                                }
                            }
                        }
                    )
                    Text("  podział procentowy")
                }

                if(isSeperateValues) {
                    TextField(
                        value = fullPrice,
                        onValueChange = {fullPrice = it},
                        label = {Text("Całość")},
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal)
                    )
                }

                users.forEachIndexed{index, value ->
                    TextField(
                        value=userValues[index],
                        onValueChange = {userValues[index] = it},
                        label = {Text(value.name)},
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal)
                    )
                }
                Row() {
                    Box {
                        Button(onClick = { expanded = true }) {
                            if (payer != -1) {
                                Text("Płacący: ${users.find { it.id == payer }?.name ?: "Nieznaleziono"}")
                            } else {
                                Text("Wybierz płacącego")
                            }
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            users.forEach { user ->
                                DropdownMenuItem(
                                    text = { Text(user.name) },
                                    onClick = {
                                        payer = user.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    Box {
                        Button(onClick = { expanded3 = true }) {
                            Text("Waluta: ${currCurrency}")
                        }

                        DropdownMenu(
                            expanded = expanded3,
                            onDismissRequest = { expanded3 = false }
                        ) {
                            currencies.forEach { (code, rate) ->
                                DropdownMenuItem(
                                    text = { Text("$code: $rate") },
                                    onClick = {
                                        currCurrency = code
                                        expanded3 = false
                                    }
                                )
                            }
                        }
                    }
                }
                Button(onClick = {

                    var check = true
                    for (i in 0 until users.size) {
                        val floatValue = userValues[i].toFloatOrNull()
                        if(floatValue == null || floatValue < 0f){
                            check = false
                            setAlert("Wpisz prawidłowe wartości")
                        }
                        else if(payer < 0){
                            check = false
                            setAlert("Wybierz płacącego")
                        }
                    }
                    val fullFloatOrNull = fullPrice.toFloatOrNull()
                    if(users.size == 0){
                        check = false
                        setAlert("Dodaj użytkowników")
                    }
                    else if(isSeperateValues){
                        var precentSum = 0f
                        for(i in 0 until users.size){
                            precentSum += userValues[i].toFloatOrNull() ?: 0f
                        }
                        if(fullFloatOrNull == null || fullFloatOrNull <= 0f) {
                            check = false
                            setAlert("Wpisz poprawną kwotę")
                        }
                        else if(abs(precentSum-100) > 1) {
                            check = false
                            setAlert("Suma procentów nie wynosi 100")
                        }
                    }



                    if(check) {
                        val map = mutableMapOf<Int, Float>()
                        if (isSeperateValues) {
                            for (i in users.indices) {
                                map[users[i].id] =
                                    fullPrice.toFloat() / 100 * userValues[i].toFloat()
                            }
                        } else {
                            for (i in users.indices) {
                                map[users[i].id] = userValues[i].toFloat()
                            }
                        }

                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val currentDate = sdf.format(Date())

                        events.add(Event(eventName, map, payer, currentDate, currCurrency))
                        coroutineScope.launch {
                            DataStoreManager.saveUsers(context, users)
                            DataStoreManager.saveEvents(context, events)
                        }
                    }
                }){
                    Text("Zatwierdź")
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(if(events.lastOrNull()==null) "" else "Ostatnia płatność:")
                Text(events.lastOrNull()?.print(users,true)  ?:"")


                Box {
                    Button(onClick = { expanded2 = true }) {
                        Text("Pokaż wszystkie płatności")
                    }

                    DropdownMenu(
                        expanded = expanded2,
                        onDismissRequest = { expanded2 = false }
                    ) {
                        events.forEachIndexed{index, currEvent ->
                            Row{
                                Text(currEvent.print(users))
                                Button(onClick = {
                                    events.removeAt(index)
                                    coroutineScope.launch {
                                        DataStoreManager.saveUsers(context, users)
                                        DataStoreManager.saveEvents(context, events)
                                    }
                                }){
                                    Text("X")
                                }
                            }
                        }
                    }
                }
            }
            Column(
                modifier = modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Bottom
            ){
                Button(onClick = {
                    screen = 0
                }) {
                    Text("☰")
                }
            }
        }
        2 -> {
            Column(
                modifier = modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Top
            ) {
                users.forEachIndexed{index, value ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Text(value.name)
                        Button(onClick = {
                            users.removeAt(index)
                            coroutineScope.launch {
                                DataStoreManager.saveUsers(context, users)
                                DataStoreManager.saveEvents(context, events)
                            }
                        }){
                            Text("X")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
                TextField(
                    value = newUserName,
                    onValueChange = {newUserName=it},
                    label = {Text("Nazwa nowego użytkownika")}
                )
                Button(onClick = {
                    users.add(User(newUserName,(users.lastOrNull()?.id ?: -1) + 1))
                    coroutineScope.launch {
                        DataStoreManager.saveUsers(context, users)
                        DataStoreManager.saveEvents(context, events)
                    }
                }) {
                    Text(
                        text="Dodaj użytkownika"
                    )
                }
                Button(onClick = {
                    users.add(User("Mario",(users.lastOrNull()?.id ?: -1) + 1))
                    users.add(User("Luigi",(users.lastOrNull()?.id ?: -1) + 1))
                    users.add(User("Steve",(users.lastOrNull()?.id ?: -1) + 1))
                    coroutineScope.launch {
                        DataStoreManager.saveUsers(context, users)
                        DataStoreManager.saveEvents(context, events)
                    }
                }) {
                    Text(
                        text="Dodaj testowych użytkowników"
                    )
                }
            }
            Column(
                modifier = modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Bottom
            ){
                Button(onClick = {
                    screen = 0
                }){
                    Text("☰")
                }
            }
        }
        3 -> {
            Column(
                modifier = modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Top
            ) {
                Text("Podsumowanie")
                Spacer(modifier = Modifier.height(30.dp))
                Text("          Podstawowe")
                users.forEach{
                    Text(it.name)
                    Text(it.simpleDebtSummary(users, events, currencies))
                }
                Spacer(modifier = Modifier.height(50.dp))
                summarizeDebts(users, events, currencies)
                Text("          Uproszczone")
                users.forEach{
                    Text(it.debtsToString(users))
                    Spacer(modifier = Modifier.height(30.dp))
                }

            }
            Column(
                modifier = modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Bottom
            ){
                Button(onClick = {
                    screen = 0
                }){
                    Text("☰")
                }

            }
        }
        4 -> {
            Column(
                modifier = modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Top
            ){
                Text("Kursy walut")
                currencies.forEach{(code, rate) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Text("$code    ")
                        if(code == "PLN"){
                            Text("1.00")
                        }
                        else {
                            TextField(
                                value = rate.toString(),
                                onValueChange = {
                                    val newRate = it.toFloatOrNull()
                                    if (newRate != null) {
                                        currencies[code] = newRate
                                    }
                                },
                                label = { Text("kurs do 1PLN") },
                                modifier = Modifier.width(150.dp),
                                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal)
                            )
                        }
                        if(code!="PLN") {
                            Button(
                                onClick = {
                                    currencies.remove(code)
                                }
                            ) {
                                Text("X")
                            }
                        }
                    }
                }

                var newCurrencyName by remember { mutableStateOf("")}
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ){
                    TextField(
                        value = newCurrencyName,
                        onValueChange = { newCurrencyName = it },
                        label = { Text("kod waluty") },
                        modifier = Modifier.width(180.dp)
                    )
                    Button(onClick = {
                        currencies[newCurrencyName] = 1.0f
                    }){Text("Dodaj nową walutę")}

                }

            }
            Column(
                modifier = modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Bottom
            ){
                Button(onClick = {
                    screen = 0
                }){
                    Text("☰")
                }

            }
        }
    }
}