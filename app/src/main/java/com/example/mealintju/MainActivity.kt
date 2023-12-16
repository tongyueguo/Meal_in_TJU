package com.example.mealintju

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.twotone.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.mealintju.ui.theme.MealInTJUTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Calendar

const val maxCanteenNumber=7
val maxWindowNumber= arrayOf(20,20,20,20,20,20,20)

class MainActivity : ComponentActivity() {
    @SuppressLint("CoroutineCreationDuringComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MealInTJUTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainView(applicationContext)
                }
            }
        }
    }
}
////////////////////////////
//Room数据库
@Entity(primaryKeys = ["year", "month","day","mealNumber"])
data class mealInfo(
    @ColumnInfo(name = "year")
    var year: Int = 0,
    @ColumnInfo(name = "month")
    var month: Int = 0,
    @ColumnInfo(name = "day")
    var day: Int = 0,
    @ColumnInfo(name = "mealNumber")
    var mealNumber: Int = 0 ,
    @ColumnInfo(name = "canteenNumber")
    var canteenNumber: Int = 0,
    @ColumnInfo(name = "windowText")
    var windowText: String = "" ,
    @ColumnInfo(name = "result")
    var result: Int = -1//Int?=0
)
@Dao
interface mealInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mealInfo: mealInfo)
    @Delete
    suspend fun delete(mealInfo: mealInfo)
    @Update
    suspend fun update(mealInfo: mealInfo)
    @Query("SELECT * FROM mealInfo")
    suspend fun getAll(): List<mealInfo>
    @Query("SELECT * FROM mealInfo WHERE year=:year AND month=:month AND day=:day AND mealNumber=:mealNumber")
    suspend fun get(year: Int,month: Int,day: Int,mealNumber: Int):mealInfo
}
@Database(
    entities = [mealInfo::class],
    version = 1,
    exportSchema = false
)
abstract class mealInfoDatabase : RoomDatabase() {
    abstract fun mealInfoDao(): mealInfoDao
    companion object {
        @Volatile
        private var INSTANCE: mealInfoDatabase? = null
        fun getDatabase(context: Context): mealInfoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    mealInfoDatabase::class.java,
                    "mealInfoDatabase"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
@Composable
fun MainView(context: Context){
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "mainPage"){
        composable("mainPage"){
            mainPage(navController = navController, context = context)
        }
        composable("settingPage"){
            settingPage(navController = navController, context = context)
        }
        composable("historyPage"){
            historyPage(navController = navController, context = context)
        }
        composable("analysePage"){
            analysePage(navController = navController, context = context)
        }
        composable("editPage"){
            editPage(navController = navController, context = context)
        }
    }
}
@Composable
fun mainPage(modifier: Modifier = Modifier, navController: NavController, context: Context) {
    //writeFile(context,"")
    var status by remember {mutableStateOf(checkStatus(context))}//0:无记录  1:已有记录从数据库读  2:已有记录不从数据库读数据
    val mealInfo= queryMealInfo(context)
    var canteenNumber by remember { mutableStateOf(if (status==1)mealInfo.canteenNumber else 0) }
    val canteenTextId=canteenNumberToCanteenTextId(canteenNumber)
    var windowText by remember { mutableStateOf(if (status==1)mealInfo.windowText else context.getString(R.string.window1Text)+"1"+context.getString(R.string.window2Text)) }
    val buttonText = if(status!=0)R.string.changeText else R.string.whatToEatText
    val buttonHeight by animateDpAsState(
        targetValue = if(status==0)450.dp else 650.dp ,
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
        label = ""
    )
    var result by remember {
        mutableStateOf(if (status==1)mealInfo.result else 0)
    }
    ConstraintLayout(
        Modifier.fillMaxWidth()
    ){
        val (button,text1,text2,text3,text4,icon1,icon2,icon3,ratingBar) = createRefs()
        IconButton(
            onClick = {
                navController.navigate("settingPage")
            },
            modifier=Modifier
                .constrainAs(icon1) {
                    top.linkTo(parent.top, margin = 20.dp)
                    end.linkTo(parent.end, margin = 20.dp)
                }
        ){
            Icon(Icons.Filled.Settings, null)
        }
        IconButton(
            onClick = {
                navController.navigate("historyPage")
            },
            modifier=Modifier
                .constrainAs(icon2) {
                    top.linkTo(parent.top, margin = 20.dp)
                    end.linkTo(parent.end, margin = 60.dp)
                }
        ){
            Icon(Icons.Filled.DateRange, null)
        }
        IconButton(
            onClick =
            {
                navController.navigate("editPage")
            },
            modifier=Modifier
                .constrainAs(icon3) {
                    top.linkTo(parent.top, margin = 20.dp)
                    end.linkTo(parent.end, margin = 100.dp)
                }
        ){
            Icon(Icons.Filled.Edit, null)
        }
        AnimatedVisibility (status!=0,
            modifier = Modifier
                .constrainAs(text1) {
                top.linkTo(parent.top, margin = 210.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ){
            Text(
                text = stringResource(R.string.beforeCanteenText),
                //color = Color.Blue,
                fontSize = 30.sp,
                textAlign= TextAlign.Center,
                //fontFamily = FontFamily.Serif,
            )
        }
        AnimatedVisibility (status!=0,
            modifier = Modifier.constrainAs(text2) {
                top.linkTo(parent.top, margin = 280.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ){
            Text(
                text = stringResource(canteenTextId),
                //color = Color.Blue,
                fontSize = 60.sp,
                textAlign= TextAlign.Center,
                //fontFamily = FontFamily.Serif,
            )
        }
        AnimatedVisibility (status!=0,
            modifier = Modifier.constrainAs(text3) {
                top.linkTo(parent.top, margin = 370.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ){
            Text(
                text = windowText,
                //color = Color.Blue,
                fontSize = 60.sp,
                textAlign= TextAlign.Center,
                //fontFamily = FontFamily.Serif,
            )
        }
        AnimatedVisibility (status!=0,
            modifier = Modifier.constrainAs(ratingBar) {
                top.linkTo(parent.top, margin = 575.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ){
            Row {
                Icon(if (result-1 >= 0) Icons.Filled.Star else Icons.TwoTone.Star, null, modifier = Modifier.clickable { result=1;updateResult(result,context) } , tint =MaterialTheme.colorScheme.primary )
                Icon(if (result-2 >= 0) Icons.Filled.Star else Icons.TwoTone.Star, null, modifier = Modifier.clickable { result=2;updateResult(result,context) } , tint =MaterialTheme.colorScheme.primary )
                Icon(if (result-3 >= 0) Icons.Filled.Star else Icons.TwoTone.Star, null, modifier = Modifier.clickable { result=3;updateResult(result,context) } , tint =MaterialTheme.colorScheme.primary )
                Icon(if (result-4 >= 0) Icons.Filled.Star else Icons.TwoTone.Star, null, modifier = Modifier.clickable { result=4;updateResult(result,context) } , tint =MaterialTheme.colorScheme.primary )
                Icon(if (result-5 >= 0) Icons.Filled.Star else Icons.TwoTone.Star, null, modifier = Modifier.clickable { result=5;updateResult(result,context) } , tint =MaterialTheme.colorScheme.primary )
            }
        }
        AnimatedVisibility (status!=0,
            modifier = Modifier.constrainAs(text4) {
                top.linkTo(parent.top, margin = 460.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ){
            Text(
                text = readMealData(context,windowText,canteenNumber),
                //color = Color.Blue,
                fontSize = 60.sp,
                textAlign= TextAlign.Center,
                //fontFamily = FontFamily.Serif,
            )
        }
        Button(
            onClick =
            {
                status=2
                canteenNumber=randomCanteen()
                windowText=randomWindow(canteenNumber, context = context)
                insertDatabase(canteenNumber,windowText,context)
            },
            modifier= Modifier
                .defaultMinSize()
                .constrainAs(button) {
                    top.linkTo(parent.top, margin = buttonHeight)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        ) {
            Text(
                text = stringResource(buttonText),
                fontSize = 40.sp
            )
        }
    }
}

fun queryMealInfo(context: Context): mealInfo= runBlocking{
    val mCalendar= getCalendar()
    var mealInfo=mealInfo()
    val query = launch{
        mealInfo=mealInfoDatabase.getDatabase(context).mealInfoDao().get(mCalendar.get(Calendar.YEAR),mCalendar.get(Calendar.MONTH)+1,mCalendar.get(Calendar.DAY_OF_MONTH),mCalendar.get(Calendar.AM_PM))
    }
    query.join()
    return@runBlocking mealInfo
}

fun getCalendar():Calendar{
    val time = System.currentTimeMillis()
    val mCalendar:Calendar = Calendar.getInstance()
    mCalendar.setTimeInMillis(time)
    return mCalendar
}
fun updateResult(result: Int,context: Context)= runBlocking{
    var mealInfo= queryMealInfo(context)
    mealInfo.result=result
    val job = launch {mealInfoDatabase.getDatabase(context).mealInfoDao().insert(mealInfo)}
    job.join()
    return@runBlocking
}
fun setTime(mealInfo: mealInfo):mealInfo{
    val mCalendar= getCalendar()
    mealInfo.year = mCalendar.get(Calendar.YEAR)
    mealInfo.month = mCalendar.get(Calendar.MONTH)+1
    mealInfo.day = mCalendar.get(Calendar.DAY_OF_MONTH)
    mealInfo.mealNumber = mCalendar.get(Calendar.AM_PM)
    return mealInfo
}
fun checkStatus(context: Context):Int= runBlocking{
    val mCalendar= getCalendar()
    var mealInfo: mealInfo
    var flag=0
    val query = launch{
        try {
            mealInfo=mealInfoDatabase.getDatabase(context).mealInfoDao().get(mCalendar.get(Calendar.YEAR),mCalendar.get(Calendar.MONTH)+1,mCalendar.get(Calendar.DAY_OF_MONTH),mCalendar.get(Calendar.AM_PM))
            if (mealInfo.result==-1)flag=0 else flag=1
        }catch(_:Exception){}
    }
    query.join()
    return@runBlocking flag
}
fun insertDatabase(canteenNumber: Int, windowText: String,context: Context)=runBlocking{
    val mCalendar= getCalendar()
    val insert = launch{
        val mealInfo = mealInfo()
        mealInfo.year = mCalendar.get(Calendar.YEAR)
        mealInfo.month = mCalendar.get(Calendar.MONTH)+1
        mealInfo.day = mCalendar.get(Calendar.DAY_OF_MONTH)
        mealInfo.mealNumber = mCalendar.get(Calendar.AM_PM)
        mealInfo.canteenNumber = canteenNumber
        mealInfo.windowText = windowText
        mealInfo.result = 0
        mealInfoDatabase.getDatabase(context).mealInfoDao().insert(mealInfo)
    }
    insert.join()
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun settingPage(modifier: Modifier = Modifier, navController: NavController,context:Context ){
    ConstraintLayout(
        Modifier.fillMaxWidth()
    ){
        val (button,text1,text2,text3,icon1) = createRefs()
        var text by remember{ mutableStateOf("")}
        var scrollState= rememberScrollState()
        IconButton(
            onClick = {
                navController.navigate("mainPage")
            },
            modifier=Modifier
                .constrainAs(icon1) {
                    top.linkTo(parent.top, margin = 20.dp)
                    start.linkTo(parent.start,margin=20.dp)
                }
        ){
            Icon(Icons.Filled.ArrowBack, null)
        }
        Text(
            text = stringResource(R.string.settingText),
            fontSize = 30.sp,
            modifier=Modifier
                .constrainAs(text1) {
                    top.linkTo(parent.top, margin = 23.dp)
                    start.linkTo(parent.start,margin = 60.dp)
                }
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("字符串") },
            modifier=Modifier
                .constrainAs(text2) {
                    top.linkTo(parent.top, margin = 150.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            trailingIcon = {
                IconButton(onClick = {text=""}) {
                Icon(Icons.Filled.Clear,"")
                }
            },
            maxLines = 3)
        Button(
            onClick = {
                writeMealData(context = context,text)
            },
            modifier=Modifier
                .constrainAs(button) {
                    top.linkTo(parent.top, margin = 350.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        ){
            Text(
                text = stringResource(R.string.confirmText),
                fontSize = 30.sp
            )
        }
        Column (
            modifier= Modifier
                .verticalScroll(scrollState)
                .fillMaxSize()
                .constrainAs(text3) {
                    top.linkTo(parent.top, margin = 800.dp)
                    start.linkTo(parent.start, margin = 60.dp)
                }
        ){
            Text(text = "Meal In TJU")
            Text(text = "© 2023 TongyueGuo")
            //TODO 设置界面等
        }
    }
}
@Composable
fun historyPage(modifier: Modifier = Modifier, navController: NavController, context: Context){
    ConstraintLayout(
        Modifier.fillMaxWidth()
    ){
        val (text1,text2,icon1,icon2) = createRefs()
        var scrollState= rememberScrollState()
        IconButton(
            onClick = {
                navController.navigate("mainPage")
            },
            modifier=Modifier
                .constrainAs(icon1) {
                    top.linkTo(parent.top, margin = 20.dp)
                    start.linkTo(parent.start,margin=20.dp)
                }
        ){
            Icon(Icons.Filled.ArrowBack, null)
        }
        IconButton(
            onClick = {
                navController.navigate("analysePage")
            },
            modifier=Modifier
                .constrainAs(icon2) {
                    top.linkTo(parent.top, margin = 20.dp)
                    end.linkTo(parent.end,margin = 20.dp)
                }
        ){
            Icon(Icons.Filled.Info, null)
        }
        Text(
            text = stringResource(R.string.historyText),
            fontSize = 30.sp,
            modifier=Modifier
                .constrainAs(text1) {
                    top.linkTo(parent.top, margin = 23.dp)
                    start.linkTo(parent.start,margin = 60.dp)
                }
        )
        Column (
            modifier= Modifier
                .verticalScroll(scrollState)
                .fillMaxSize()
                .constrainAs(text2) {
                    top.linkTo(parent.top, margin = 80.dp)
                    start.linkTo(parent.start, margin = 60.dp)
                }
        ){
            historyDisplayText(context)
        }
    }
}
@Composable
fun historyDisplayText(context: Context){
    /*var str by remember {
        mutableStateOf(StringBuilder())
    }
    LaunchedEffect(null) {
        var all=mealInfoDatabase.getDatabase(context).mealInfoDao().getAll()
        for (mealInfo in all){
            str.append(mealInfo.year.toString()
                    +context.getString(R.string.yearText)
                    +mealInfo.month.toString()
                    +context.getString(R.string.monthText)
                    +mealInfo.day.toString()
                    +context.getString(R.string.dayText)
                    +context.getString(mealNumberToTextId(mealInfo.mealNumber))
                    +context.getString(canteenNumberToCanteenTextId(mealInfo.canteenNumber))
                    +mealInfo.windowText
                    +"\n")
        }
    }*/
    val all:List<mealInfo> = remember {
        getHistoryList(context)
    }
    val timeText=StringBuilder()
    val canteenText=StringBuilder()
    val mealNumberText=StringBuilder()
    val windowText=StringBuilder()
    val resultText=StringBuilder()
    val windowDetailText=StringBuilder()
    for(mealInfo in all){
        timeText.append(mealInfo.year.toString() +"."+mealInfo.month.toString()+"."+mealInfo.day.toString()+"\n")
        canteenText.append(context.getString(canteenNumberToCanteenTextId(mealInfo.canteenNumber))+"\n")
        mealNumberText.append(context.getString(mealNumberToTextId(mealInfo.mealNumber))+"\n")
        windowText.append(mealInfo.windowText+"\n")
        windowDetailText.append(readMealData(context,mealInfo.windowText,mealInfo.canteenNumber)+"\n")
        resultText.append(if(mealInfo.result!=0)mealInfo.result.toString()+"\n" else context.getString(R.string.noResultText)+"\n")
    }
    canteenText.append("\n".repeat(2))
    Row {
        Text(
            text = timeText.toString(),
            fontSize = 18.sp,
            //fontFamily = FontFamily.Default,
            lineHeight = 30.sp,
            color = MaterialTheme.colorScheme.primary,
            //modifier = Modifier.padding(20.dp)
        )
        Text(
            text = mealNumberText.toString(),
            fontSize = 18.sp,
            //fontFamily = FontFamily.Monospace,
            lineHeight = 30.sp,
            modifier = Modifier.padding(start = 20.dp,top=0.dp,end=0.dp,bottom=0.dp)
        )
        Text(
            text = canteenText.toString(),
            fontSize = 18.sp,
            //fontFamily = FontFamily.Monospace,
            lineHeight = 30.sp,
            modifier = Modifier.padding(start = 10.dp,top=0.dp,end=0.dp,bottom=0.dp)
        )
        Text(
            text = windowText.toString(),
            fontSize = 18.sp,
            //fontFamily = FontFamily.Monospace,
            lineHeight = 30.sp,
            modifier = Modifier.padding(start = 10.dp,top=0.dp,end=0.dp,bottom=0.dp)
        )
        Text(
            text = windowDetailText.toString(),
            fontSize = 18.sp,
            //fontFamily = FontFamily.Monospace,
            lineHeight = 30.sp,
            modifier = Modifier.padding(start = 10.dp,top=0.dp,end=0.dp,bottom=0.dp)
        )
        Text(
            text = resultText.toString(),
            fontSize = 18.sp,
            //fontFamily = FontFamily.Monospace,
            lineHeight = 30.sp,
            modifier = Modifier.padding(start = 10.dp,top=0.dp,end=0.dp,bottom=0.dp)
        )
    }
}
fun getHistoryList(context: Context):List<mealInfo> = runBlocking {
    val temp=mealInfo()
    var all: List<mealInfo> = mutableListOf(temp)
    val job=launch{
        all=mealInfoDatabase.getDatabase(context).mealInfoDao().getAll()
    }
    job.join()
    return@runBlocking all
}
@Composable
fun analysePage(navController: NavController,context: Context){
    ConstraintLayout(
        Modifier.fillMaxWidth()
    ){
        val (text1,text2,icon1,icon2) = createRefs()
        var scrollState= rememberScrollState()
        IconButton(
            onClick = {
                navController.navigate("historyPage")
            },
            modifier=Modifier
                .constrainAs(icon1) {
                    top.linkTo(parent.top, margin = 20.dp)
                    start.linkTo(parent.start,margin=20.dp)
                }
        ){
            Icon(Icons.Filled.ArrowBack, null)
        }
        Text(
            text = stringResource(R.string.analyseText),
            fontSize = 30.sp,
            modifier=Modifier
                .constrainAs(text1) {
                    top.linkTo(parent.top, margin = 23.dp)
                    start.linkTo(parent.start,margin = 60.dp)
                }
        )
        Column (
            modifier= Modifier
                .verticalScroll(scrollState)
                .fillMaxSize()
                .constrainAs(text2) {
                    top.linkTo(parent.top, margin = 80.dp)
                    start.linkTo(parent.start, margin = 60.dp)
                }
        ){
            analyseDisplayText(context = context)
        }
    }
}
@Composable
fun analyseDisplayText(context: Context){
    val all:List<mealInfo> = remember {
        getHistoryList(context)
    }
    val canteenText=StringBuilder()
    val windowText=StringBuilder()
    val windowDetailText=StringBuilder()
    val timesText=StringBuilder()
    val resultText=StringBuilder()
    var totalNumber=0
    for (i in 0 until maxCanteenNumber){
        totalNumber += maxWindowNumber[i]
    }
    var timesArray= Array(totalNumber){i->0}
    var resultArray= Array(totalNumber){i->0}
    for (mealInfo in all){
        var totalNumber= windowTextToWindowNumber(mealInfo.windowText)
        for (i in 1 until mealInfo.canteenNumber){
            totalNumber += maxWindowNumber[i - 1]
        }
        timesArray[totalNumber-1]+=1
        resultArray[totalNumber-1]+=(if(mealInfo.result<1) 3 else mealInfo.result)
    }
    for (i in 1 until maxCanteenNumber+1){
        canteenText.append(context.getString(canteenNumberToCanteenTextId(i))+"\n")
        windowText.append(windowNumberToWindowText(1,context)+"\n")
        windowDetailText.append(readMealData(context,windowNumberToWindowText(1,context),i)+"\n")
        for (j in 1 until maxWindowNumber[i-1]){
            canteenText.append("\n")
            windowText.append(windowNumberToWindowText(j+1,context)+"\n")
            windowDetailText.append(readMealData(context,windowNumberToWindowText(j+1,context),i)+"\n")
        }
    }
    for (i in 0 until totalNumber){
        timesText.append(timesArray[i].toString()+"\n")
        resultText.append((resultArray[i].toDouble()/(if (timesArray[i]==0) 1 else timesArray[i])).toString()+"\n")
    }
    canteenText.append("\n".repeat(2))
    Row {
        Text(
            text = canteenText.toString(),
            fontSize = 20.sp,
            //fontFamily = FontFamily.Monospace,
            lineHeight = 40.sp,
            modifier = Modifier.padding(start = 10.dp,top=0.dp,end=0.dp,bottom=0.dp)
        )
        Text(
            text = windowText.toString(),
            fontSize = 20.sp,
            //fontFamily = FontFamily.Monospace,
            lineHeight = 40.sp,
            modifier = Modifier.padding(start = 10.dp,top=0.dp,end=0.dp,bottom=0.dp)
        )
        Text(
            text = windowDetailText.toString(),
            fontSize = 20.sp,
            //fontFamily = FontFamily.Monospace,
            lineHeight = 40.sp,
            modifier = Modifier.padding(start = 10.dp,top=0.dp,end=0.dp,bottom=0.dp)
        )
        Text(
            text = timesText.toString(),
            fontSize = 20.sp,
            //fontFamily = FontFamily.Monospace,
            lineHeight = 40.sp,
            modifier = Modifier.padding(start = 10.dp,top=0.dp,end=0.dp,bottom=0.dp)
        )
        Text(
            text = resultText.toString(),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 20.sp,
            //fontFamily = FontFamily.Monospace,
            lineHeight = 40.sp,
            modifier = Modifier.padding(start = 10.dp,top=0.dp,end=0.dp,bottom=0.dp)
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun editPage(modifier: Modifier = Modifier, navController: NavController, context: Context){
    var canteenText by remember { mutableStateOf("") }
    var windowText by remember { mutableStateOf("") }
    ConstraintLayout(
        Modifier.fillMaxWidth()
    ){
        val (text1,text2,text3,icon1,button) = createRefs()
        IconButton(
            onClick = {
                navController.navigate("mainPage")
            },
            modifier=Modifier
                .constrainAs(icon1) {
                    top.linkTo(parent.top, margin = 20.dp)
                    start.linkTo(parent.start,margin=20.dp)
                }
        ){
            Icon(Icons.Filled.ArrowBack, null)
        }
        Text(
            text = stringResource(R.string.editText),
            fontSize = 30.sp,
            modifier=Modifier
                .constrainAs(text1) {
                    top.linkTo(parent.top, margin = 23.dp)
                    start.linkTo(parent.start,margin = 60.dp)
                }
        )
        OutlinedTextField(
            value = canteenText,
            onValueChange = { canteenText = it },
            label = { Text(stringResource(R.string.canteenText)) },
            modifier=Modifier
                .constrainAs(text2) {
                    top.linkTo(parent.top, margin = 150.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )//TODO 改进输入方式
        OutlinedTextField(
            value = windowText,
            onValueChange = { windowText = it },
            label = { Text(stringResource(R.string.windowText)) },
            modifier=Modifier
                .constrainAs(text3) {
                    top.linkTo(parent.top, margin = 225.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )
        Button(
            onClick = {
                insertDatabase(canteenText.toInt(),"第"+windowText+"窗口",context)
            },
            modifier=Modifier
                .constrainAs(button) {
                    top.linkTo(parent.top, margin = 350.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        ){
            Text(
                text = stringResource(R.string.confirmText),
                fontSize = 30.sp
            )
        }
    }
}
fun mealNumberToTextId(mealNumber: Int): Int {
    val result=when(mealNumber){
        0->R.string.lunchText
        1->R.string.dinnerText
        else->R.string.nullText
    }
    return result
}
fun randomCanteen(): Int {
    val result:Int
    result=(1..maxCanteenNumber).random()
    return result
}
fun randomWindow(canteenNumber:Int,context: Context): String {
    val result:Int
    result=(1..20).random()
    return windowNumberToWindowText(result,context)
}//TODO 窗口更新
fun canteenNumberToCanteenTextId(canteenNumber: Int):Int {
    val result=when(canteenNumber){
        1->R.string.canteen1floor1Text
        2->R.string.canteen1floor2Text
        3->R.string.canteen3floor1Text
        4->R.string.canteen4floor1Text
        5->R.string.canteen4floor2Text
        6->R.string.canteen5floor2Text
        7->R.string.canteen5floor3Text
        else->R.string.nullText
    }
    return result
}
fun writeMealData(context: Context, str:String){
    context.openFileOutput("mealData",Context.MODE_PRIVATE).use {
        it.write(str.toByteArray())
    }
}
fun readMealData(context: Context, windowText: String, canteenNumber: Int):String{
    val str=StringBuilder()
    try {
        val windowNumber= windowTextToWindowNumber(windowText)
        context.openFileInput("mealData").bufferedReader().forEachLine {
            if (it.filter { it.isDigit() }.toInt()==canteenNumber*100+windowNumber)
                str.append(it.substring(3))
        }
    }catch (_:Exception){}
    return str.toString()
}
fun windowTextToWindowNumber(windowText: String):Int{
    return windowText.filter { it.isDigit() }.toInt()
}
fun windowNumberToWindowText(windowNumber: Int,context: Context):String{
    return context.getString(R.string.window1Text)+windowNumber.toString()+context.getString(R.string.window2Text)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MealInTJUTheme {
        val navController= rememberNavController()
        // mainPage(navController = navController)
    }
}