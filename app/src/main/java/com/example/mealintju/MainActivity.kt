package com.example.mealintju

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.twotone.Egg
import androidx.compose.material.icons.twotone.Park
import androidx.compose.material.icons.twotone.SetMeal
import androidx.compose.material.icons.twotone.Star
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
val maxWindowNumber= arrayOf(20,20,20,20,20,20,25)
/* ////////////////////////////////////////////////
mealInfo用户每餐
mealData窗口信息
//////////////////////////////////////////////// */
class MainActivity : ComponentActivity() {
    @SuppressLint("CoroutineCreationDuringComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MealInTJUTheme {
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
/////////////////////////////////////////////Room数据库
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
    var result: Int = -1,
    @ColumnInfo(name = "meat")
    var meat: Boolean = false,
    @ColumnInfo(name = "egg")
    var egg: Boolean = false,
    @ColumnInfo(name = "vegetable")
    var vegetable: Boolean = false,
    @ColumnInfo(name = "location")
    var location: Int = 0,
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
        composable("resultPage"){
            resultPage(navController = navController, context = context)
        }
    }
}
@Composable
fun mainPage(modifier: Modifier = Modifier, navController: NavController, context: Context) {
    if(firstRun(context)){ writeMealData(context, stringResource(R.string.defaultMealDataText)) }
    var status by remember { mutableStateOf(checkStatus(context)) }//0:无记录  1:已有记录从数据库读  2:已有记录不从数据库读数据
    val mealInfo= queryMealInfo(context)
    var canteenNumber by remember { mutableStateOf(if (status==1)mealInfo.canteenNumber else 0) }
    var location by remember { mutableStateOf(if (status==1)mealInfo.location else 0) }
    val canteenTextId=canteenNumberToCanteenTextId(canteenNumber)
    var windowText by remember { mutableStateOf(if (status==1)mealInfo.windowText else context.getString(R.string.window1Text)+"1"+context.getString(R.string.window2Text)) }
    ConstraintLayout(
        Modifier.fillMaxWidth()
    ){
        val (text1,text2,text3,text4,text5,icon1,icon2,icon3,icon4,icon5,icon6,icon7,iconGroup) = createRefs()
        ///////////////////////////////////////////////////导航栏
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
            modifier = Modifier
                .constrainAs(icon2) {
                    top.linkTo(parent.top, margin = 20.dp)
                    end.linkTo(parent.end, margin = 60.dp)
                }
        ){
            Icon(Icons.Filled.DateRange, null)
        }
        IconButton(
            onClick = {
                navController.navigate("editPage")
            },
            modifier = Modifier
                .constrainAs(icon3) {
                    top.linkTo(parent.top, margin = 20.dp)
                    end.linkTo(parent.end, margin = 100.dp)
                }
        ){
            Icon(Icons.Filled.Edit, null)
        }
        ///////////////////////////////////////////////////展示界面
        AnimatedVisibility (status!=0,
            modifier = Modifier.constrainAs(icon7) {
                top.linkTo(parent.top, margin = 20.dp)
                start.linkTo(parent.start,margin=20.dp)
            }
        ){
            IconButton(
                onClick = { status=0 },
            ){
                Icon(Icons.Filled.ArrowBack, null)
            }
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
                fontSize = 30.sp,
                textAlign = TextAlign.Center,
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
                fontSize = 60.sp,
                textAlign = TextAlign.Center,
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
                fontSize = 60.sp,
                textAlign = TextAlign.Center,
            )
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
                fontSize = 60.sp,
                textAlign = TextAlign.Center,
            )
        }
        AnimatedVisibility (status!=0,
            modifier = Modifier.constrainAs(icon4) {
                top.linkTo(parent.top, margin = 600.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ){
            IconButton(
                onClick = {
                    navController.navigate("resultPage")
                }
            ){
                Icon(Icons.Filled.ArrowForward, null, modifier = Modifier.size(60.dp), tint =MaterialTheme.colorScheme.primary)
            }
        }
        AnimatedVisibility (status!=0,
            modifier = Modifier.constrainAs(icon5) {
                top.linkTo(parent.top, margin = 700.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ){
            IconButton(
                onClick = {
                    status=2
                    canteenNumber=randomCanteen(location)
                    windowText=randomWindow(canteenNumber, context = context)
                    update(canteenNumber,windowText,context)
                }
            ){
                Icon(Icons.Filled.Loop, null, modifier = Modifier.size(60.dp))
            }
        }
        ///////////////////////////////////////////////////选择界面
        AnimatedVisibility (status==0,
            modifier = Modifier.constrainAs(iconGroup) {
                top.linkTo(parent.top, margin = 300.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ){
            Row {
                IconButton(
                    onClick = {location=setLocation(location,12)}
                ){
                    Text(stringResource(R.string.teachingBuilding12Text), color = if (location==12) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.inverseSurface, fontSize = 24.sp )
                }
                IconButton(
                    onClick = {location=setLocation(location,19)}
                ){
                    Text(stringResource(R.string.teachingBuilding19Text), color = if (location==19) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.inverseSurface, fontSize = 24.sp )
                }
                IconButton(
                    onClick = {location=setLocation(location,23)}
                ){
                    Text(stringResource(R.string.teachingBuilding23Text), color = if (location==23) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.inverseSurface, fontSize = 24.sp )
                }
                IconButton(
                    onClick = {location=setLocation(location,26)}
                ){
                    Text(stringResource(R.string.teachingBuilding26Text), color = if (location==26) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.inverseSurface, fontSize = 24.sp )
                }
            }
        }
        AnimatedVisibility (status==0,
            modifier = Modifier.constrainAs(text5) {
                top.linkTo(parent.top, margin = 200.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ){
            Text(stringResource(R.string.locationText), fontSize = 50.sp)
        }
        AnimatedVisibility (status==0,
            modifier = Modifier.constrainAs(icon6) {
                top.linkTo(parent.top, margin = 400.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ){
            IconButton(
                onClick = {
                    updateLocation(location, context)
                    status=2
                    canteenNumber=randomCanteen(location)
                    windowText=randomWindow(canteenNumber, context = context)
                    update(canteenNumber,windowText,context)
                }
            ){
                Icon(Icons.Filled.RestaurantMenu, null, modifier = Modifier.size(100.dp))
            }
        }
    }
}
fun setLocation(location: Int, target: Int): Int {
    val result:Int
if (location==target) result=0 else result=target
    return result
}
@Composable
fun resultPage(modifier: Modifier = Modifier, navController: NavController, context: Context) {
    val mealInfo= queryMealInfo(context)
    var result by remember {
        mutableStateOf(mealInfo.result)
    }
    var meat by remember {
        mutableStateOf(mealInfo.meat)
    }
    var egg by remember {
        mutableStateOf(mealInfo.egg)
    }
    var vegetable by remember {
        mutableStateOf(mealInfo.vegetable)
    }
    ConstraintLayout(
        Modifier.fillMaxWidth()
    ){
        val (text1,icon1,ratingBar,infoBar) = createRefs()
        Text(
            text = stringResource(R.string.resultsText),
            fontSize = 60.sp,
            modifier = Modifier.constrainAs(text1) {
                top.linkTo(parent.top, margin = 150.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )
        Row (
            modifier = Modifier.constrainAs(ratingBar) {
                top.linkTo(parent.top, margin = 300.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ){
            Icon(if (result-1 >= 0) Icons.Filled.Star else Icons.TwoTone.Star, null, modifier = Modifier
                .clickable { result = 1;updateResult(result, context) }
                .size(36.dp) , tint =MaterialTheme.colorScheme.primary )
            Icon(if (result-2 >= 0) Icons.Filled.Star else Icons.TwoTone.Star, null, modifier = Modifier
                .clickable { result = 2;updateResult(result, context) }
                .size(36.dp) , tint =MaterialTheme.colorScheme.primary )
            Icon(if (result-3 >= 0) Icons.Filled.Star else Icons.TwoTone.Star, null, modifier = Modifier
                .clickable { result = 3;updateResult(result, context) }
                .size(36.dp) , tint =MaterialTheme.colorScheme.primary )
            Icon(if (result-4 >= 0) Icons.Filled.Star else Icons.TwoTone.Star, null, modifier = Modifier
                .clickable { result = 4;updateResult(result, context) }
                .size(36.dp) , tint =MaterialTheme.colorScheme.primary )
            Icon(if (result-5 >= 0) Icons.Filled.Star else Icons.TwoTone.Star, null, modifier = Modifier
                .clickable { result = 5;updateResult(result, context) }
                .size(36.dp) , tint =MaterialTheme.colorScheme.primary )
        }
        Row (
            modifier = Modifier.constrainAs(infoBar) {
                top.linkTo(parent.top, margin = 400.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ){
            Icon(if(meat) Icons.Filled.SetMeal else Icons.TwoTone.SetMeal , null, modifier = Modifier
                .clickable { meat = !meat;updateMeat(meat, context) }
                .size(36.dp), tint =MaterialTheme.colorScheme.primary)
            Icon(Icons.Filled.Star,"", tint =MaterialTheme.colorScheme.background)
            Icon(if(egg) Icons.Filled.Egg else Icons.TwoTone.Egg , null, modifier = Modifier
                .clickable { egg = !egg;updateEgg(egg, context) }
                .size(36.dp), tint =MaterialTheme.colorScheme.primary)
            Icon(Icons.Filled.Star,"", tint =MaterialTheme.colorScheme.background)
            Icon(if(vegetable) Icons.Filled.Park else Icons.TwoTone.Park , null, modifier = Modifier
                .clickable { vegetable = !vegetable;updateVegetable(vegetable, context) }
                .size(36.dp), tint =MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = { navController.navigate("mainPage") },
            modifier = Modifier.constrainAs(icon1) {
                top.linkTo(parent.top, margin = 600.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ){
            Icon(Icons.Filled.Done, "", modifier = Modifier.size(60.dp), tint =MaterialTheme.colorScheme.primary)
        }
    }
}
fun queryMealInfo(context: Context): mealInfo= runBlocking{
    val mCalendar= getCalendar()
    var mealInfo=mealInfo()
    val query = launch{
        mealInfo=mealInfoDatabase.getDatabase(context).mealInfoDao().get(mCalendar.get(Calendar.YEAR),mCalendar.get(Calendar.MONTH)+1,mCalendar.get(Calendar.DAY_OF_MONTH),getAmPm(mCalendar))
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
    val mealInfo= queryMealInfo(context)
    mealInfo.result=result
    val job = launch {mealInfoDatabase.getDatabase(context).mealInfoDao().insert(mealInfo)}
    job.join()
    return@runBlocking
}
fun updateEgg(egg: Boolean,context: Context)= runBlocking{
    val mealInfo= queryMealInfo(context)
    mealInfo.egg=egg
    val job = launch {mealInfoDatabase.getDatabase(context).mealInfoDao().insert(mealInfo)}
    job.join()
    return@runBlocking
}
fun updateMeat(meat: Boolean,context: Context)= runBlocking{
    val mealInfo= queryMealInfo(context)
    mealInfo.meat=meat
    val job = launch {mealInfoDatabase.getDatabase(context).mealInfoDao().insert(mealInfo)}
    job.join()
    return@runBlocking
}
fun updateVegetable(vegetable: Boolean,context: Context)= runBlocking {
    val mealInfo = queryMealInfo(context)
    mealInfo.vegetable = vegetable
    val job = launch { mealInfoDatabase.getDatabase(context).mealInfoDao().insert(mealInfo) }
    job.join()
    return@runBlocking
}
fun updateLocation(location:Int,context: Context)= runBlocking {
    val mCalendar= getCalendar()
    val job = launch{
        val mealInfo = mealInfo()
        mealInfo.year = mCalendar.get(Calendar.YEAR)
        mealInfo.month = mCalendar.get(Calendar.MONTH)+1
        mealInfo.day = mCalendar.get(Calendar.DAY_OF_MONTH)
        mealInfo.mealNumber = getAmPm(mCalendar)
        mealInfo.canteenNumber = 0
        mealInfo.windowText = ""
        mealInfo.result = 0
        mealInfo.egg = false
        mealInfo.vegetable = false
        mealInfo.meat = false
        mealInfo.location = location
        mealInfoDatabase.getDatabase(context).mealInfoDao().insert(mealInfo)
    }
    job.join()
    return@runBlocking
}
fun getAmPm(mCalendar: Calendar):Int{
    if(mCalendar.get(Calendar.HOUR_OF_DAY)<=13){
        return 0
    }else return 1
}
fun checkStatus(context: Context):Int= runBlocking{
    val mCalendar= getCalendar()
    var mealInfo: mealInfo
    var flag=0
    val query = launch{
        try {
            mealInfo=mealInfoDatabase.getDatabase(context).mealInfoDao().get(mCalendar.get(Calendar.YEAR),mCalendar.get(Calendar.MONTH)+1,mCalendar.get(Calendar.DAY_OF_MONTH),getAmPm(mCalendar))
            if (mealInfo.result==-1)flag=0 else flag=1
        }catch(_:Exception){}
    }
    query.join()
    return@runBlocking flag
}
fun update(canteenNumber: Int, windowText: String,context: Context)=runBlocking{
    val mealInfo = queryMealInfo(context)
    mealInfo.canteenNumber = canteenNumber
    mealInfo.windowText = windowText
    val job = launch { mealInfoDatabase.getDatabase(context).mealInfoDao().insert(mealInfo) }
    job.join()
    return@runBlocking
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun settingPage(modifier: Modifier = Modifier, navController: NavController,context:Context ){
    ConstraintLayout(
        Modifier.fillMaxWidth()
    ){
        val (button,text1,text2,text3,text4,icon1,icon2) = createRefs()
        var text by remember{ mutableStateOf("") }
        val scrollState= rememberScrollState()
        var status by remember { mutableStateOf(false) }
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
        IconButton(
            onClick = {status=!status},
            modifier=Modifier
                .constrainAs(icon2) {
                    top.linkTo(parent.top, margin = 20.dp)
                    end.linkTo(parent.end, margin = 20.dp)
                }
        ) {
            Icon(Icons.Outlined.Help, contentDescription ="" )
        }
        AnimatedVisibility (status,
            modifier = Modifier.constrainAs(text4) {
                top.linkTo(parent.top, margin = 350.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ){
            val webText= buildAnnotatedString{
                append(stringResource(R.string.help1Text))
                pushStringAnnotation("URL","https://tongyueguo.github.io/mealData")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)){
                    append(stringResource(R.string.help2Text))
                }
                append(stringResource(R.string.help3Text))
                pop()
            }
            val uriHandler= LocalUriHandler.current
            ClickableText(
                text = webText,
                onClick = {
                webText.getStringAnnotations("URL",it,it)
                    .firstOrNull()
                    ?.let { uriHandler.openUri(it.item) }
                }
            )
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(R.string.mealInfoStringText)) },
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
            maxLines = 5)
        IconButton(
            onClick = {
                writeMealData(context = context,text)
                Toast.makeText(context, context.getString(R.string.changeSuceessText),Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.constrainAs(button) {
                top.linkTo(parent.top, margin = 400.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ){
            Icon(Icons.Filled.Done, null, modifier = Modifier.size(60.dp))
        }
        Column (
            modifier= Modifier
                .verticalScroll(scrollState)
                .fillMaxSize()
                .constrainAs(text3) {
                    top.linkTo(parent.top, margin = 900.dp)
                    start.linkTo(parent.start, margin = 60.dp)
                }
        ){
            Text(text = "Meal In TJU")
            Text(text = "© 2023 TongyueGuo")
        }
    }
}
@Composable
fun historyPage(modifier: Modifier = Modifier, navController: NavController, context: Context){
    ConstraintLayout(
        Modifier.fillMaxWidth()
    ){
        val (text1,text2,icon1,icon2) = createRefs()
        val scrollState1= rememberScrollState()
        val scrollState2= rememberScrollState()
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
                .verticalScroll(scrollState1)
                .horizontalScroll(scrollState2)
                .fillMaxSize(0.9F)
                .constrainAs(text2) {
                    top.linkTo(parent.top, margin = 80.dp)
                }
        ){
            historyDisplayText(context)
        }
    }
}
@Composable
fun historyDisplayText(context: Context){
    val all:List<mealInfo> = remember {
        getHistoryList(context)
    }
    val timeText=StringBuilder()
    val canteenText=StringBuilder()
    val mealNumberText=StringBuilder()
    val windowText=StringBuilder()
    val resultText=StringBuilder()
    val windowDetailText=StringBuilder()
    val eggList = mutableListOf<Boolean>()
    val meatList = mutableListOf<Boolean>()
    val vegetableList = mutableListOf<Boolean>()
    timeText.append(stringResource(R.string.timeText)+"\n")
    canteenText.append(stringResource(R.string.canteenText)+"\n")
    mealNumberText.append(stringResource(R.string.mealNumberText)+"\n")
    windowText.append(stringResource(R.string.windowText)+"\n")
    windowDetailText.append(stringResource(R.string.windowDetailText)+"\n")
    resultText.append(stringResource(R.string.resultText)+"\n")
    for(mealInfo in all){
        timeText.append(mealInfo.year.toString() +"."+mealInfo.month.toString()+"."+mealInfo.day.toString()+"\n")
        canteenText.append(context.getString(canteenNumberToCanteenTextId(mealInfo.canteenNumber))+"\n")
        mealNumberText.append(context.getString(mealNumberToTextId(mealInfo.mealNumber))+"\n")
        windowText.append(windowTextToWindowNumber(mealInfo.windowText).toString() +"\n")
        windowDetailText.append(readMealData(context,mealInfo.windowText,mealInfo.canteenNumber)+"\n")
        resultText.append(if(mealInfo.result!=0)mealInfo.result.toString()+"\n" else context.getString(R.string.noResultText)+"\n")
        eggList.add(mealInfo.egg)
        meatList.add(mealInfo.meat)
        vegetableList.add(mealInfo.vegetable)
    }
    canteenText.append("\n".repeat(2))
    Row {
        Text(stringResource(R.string.timeText),color=MaterialTheme.colorScheme.background)
        Text(
            text = timeText.toString(),
            fontSize = 18.sp,
            lineHeight = 30.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = mealNumberText.toString(),
            fontSize = 18.sp,
            lineHeight = 30.sp,
            modifier = Modifier.padding(start = 10.dp,top=0.dp,end=0.dp,bottom=0.dp)
        )
        Text(
            text = canteenText.toString(),
            fontSize = 18.sp,
            lineHeight = 30.sp,
            modifier = Modifier.padding(start = 10.dp,top=0.dp,end=0.dp,bottom=0.dp)
        )
        Text(
            text = windowText.toString(),
            fontSize = 18.sp,
            lineHeight = 30.sp,
            modifier = Modifier.padding(start = 10.dp,top=0.dp,end=0.dp,bottom=0.dp)
        )
        Text(
            text = windowDetailText.toString(),
            fontSize = 18.sp,
            lineHeight = 30.sp,
            modifier = Modifier.padding(start = 10.dp,top=0.dp,end=0.dp,bottom=0.dp)
        )
        Text(
            text = resultText.toString(),
            fontSize = 18.sp,
            lineHeight = 30.sp,
            modifier = Modifier.padding(start = 10.dp,top=0.dp,end=0.dp,bottom=0.dp)
        )
        Column {
            Icon(Icons.Filled.SetMeal, "", modifier = Modifier
                .height(30.dp)
                .padding(start = 10.dp, top = 0.dp, end = 0.dp, bottom = 0.dp) , tint = MaterialTheme.colorScheme.inverseSurface )
            for (temp in meatList){
                Icon(if(temp) Icons.Filled.SetMeal else Icons.TwoTone.SetMeal , "", modifier = Modifier
                    .height(30.dp)
                    .padding(start = 10.dp, top = 0.dp, end = 0.dp, bottom = 0.dp) , tint = MaterialTheme.colorScheme.primary )
            }
        }
        Column {
            Icon(Icons.Filled.Egg, "", modifier = Modifier
                .height(30.dp)
                .padding(start = 10.dp, top = 0.dp, end = 0.dp, bottom = 0.dp) , tint = MaterialTheme.colorScheme.inverseSurface )
            for (temp in eggList){
                Icon(if(temp) Icons.Filled.Egg else Icons.TwoTone.Egg , "", modifier = Modifier
                    .height(30.dp)
                    .padding(start = 10.dp, top = 0.dp, end = 0.dp, bottom = 0.dp) , tint = MaterialTheme.colorScheme.primary )
            }
        }
        Column {
            Icon(Icons.Filled.Park, "", modifier = Modifier
                .height(30.dp)
                .padding(start = 10.dp, top = 0.dp, end = 0.dp, bottom = 0.dp) , tint = MaterialTheme.colorScheme.inverseSurface )
            for (temp in vegetableList){
                Icon(if(temp) Icons.Filled.Park else Icons.TwoTone.Park , "", modifier = Modifier
                    .height(30.dp)
                    .padding(start = 10.dp, top = 0.dp, end = 0.dp, bottom = 0.dp) , tint = MaterialTheme.colorScheme.primary )
            }
        }
        Text(stringResource(R.string.timeText),color=MaterialTheme.colorScheme.background)
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
        val (text1,text2,icon1) = createRefs()
        val scrollState1= rememberScrollState()
        val scrollState2= rememberScrollState()
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
                .verticalScroll(scrollState1)
                .horizontalScroll(scrollState2)
                .fillMaxSize(0.9F)
                .constrainAs(text2) {
                    top.linkTo(parent.top, margin = 80.dp)
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
    canteenText.append(stringResource(R.string.canteenText)+"\n")
    windowText.append(stringResource(R.string.windowText)+"\n")
    windowDetailText.append(stringResource(R.string.windowDetailText)+"\n")
    timesText.append(stringResource(R.string.timesText)+"\n")
    resultText.append(stringResource(R.string.resultText)+"\n")
    for (i in 0 until maxCanteenNumber){
        totalNumber += maxWindowNumber[i]
    }
    val timesArray= Array(totalNumber){ i->0}
    val resultArray= Array(totalNumber){ i->0}
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
        Text(stringResource(R.string.timeText),color=MaterialTheme.colorScheme.background)
        Text(
            text = canteenText.toString(),
            fontSize = 20.sp,
            lineHeight = 40.sp,
            modifier = Modifier.padding(start = 10.dp,top=0.dp,end=0.dp,bottom=0.dp)
        )
        Text(
            text = windowText.toString(),
            fontSize = 20.sp,
            lineHeight = 40.sp,
            modifier = Modifier.padding(start = 10.dp,top=0.dp,end=0.dp,bottom=0.dp)
        )
        Text(
            text = windowDetailText.toString(),
            fontSize = 20.sp,
            lineHeight = 40.sp,
            modifier = Modifier.padding(start = 10.dp,top=0.dp,end=0.dp,bottom=0.dp)
        )
        Text(
            text = timesText.toString(),
            fontSize = 20.sp,
            lineHeight = 40.sp,
            modifier = Modifier.padding(start = 10.dp,top=0.dp,end=0.dp,bottom=0.dp)
        )
        Text(
            text = resultText.toString(),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 20.sp,
            lineHeight = 40.sp,
            modifier = Modifier.padding(start = 10.dp,top=0.dp,end=0.dp,bottom=0.dp)
        )
        Text(stringResource(R.string.timeText),color=MaterialTheme.colorScheme.background)
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
        IconButton(
            onClick = {
                update(canteenText.toInt(), context.getString(R.string.window1Text)+windowText+context.getString(R.string.window2Text),context)
                navController.navigate("mainPage")
            },modifier = Modifier.constrainAs(button) {
                top.linkTo(parent.top, margin = 350.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ){
            Icon(Icons.Filled.Done, null, modifier = Modifier.size(60.dp))
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
fun randomCanteen(location:Int): Int {
    var result=0
    when(location){
        19->result=(3..5).random()
        23->result=(6..7).random()
        26->result=(3..5).random()
        12->{result=(1..4).random();if (result>2) result+=3}
        else->result=(1..maxCanteenNumber).random()
    }
    return result
}
fun randomWindow(canteenNumber:Int,context: Context): String {
    val result:Int
    result=(1..maxWindowNumber[canteenNumber-1]).random()
    return windowNumberToWindowText(result,context)
}
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
fun firstRun(context: Context):Boolean{
    var flag=true
    try {
        context.openFileInput("mealData").bufferedReader().forEachLine {}
        flag=false
    }catch (_:Exception){}
    return flag
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