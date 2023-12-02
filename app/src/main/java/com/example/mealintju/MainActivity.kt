package com.example.mealintju

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.WindowManager
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.mealintju.ui.theme.MealInTJUTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

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
    var windowText: String? = null ,
    @ColumnInfo(name = "result")
    var result: Int? = 0
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
        fun getInstance(context: Context): mealInfoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext, mealInfoDatabase::class.java, "kot.db")
                .build()
    }
}




@Composable
fun MainView(context: Context){
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "mainPage"){
        composable("mainPage",){
            mainPage(navController = navController, context = context)
        }
        composable("settingPage"){
            settingPage(navController = navController)
        }
        composable("historyPage"){
            historyPage(navController = navController)
        }
    }
}

@Composable
fun mainPage(modifier: Modifier = Modifier,
             navController: NavController,
             context: Context
) {
    var canteenNumber by remember { mutableStateOf(0) }
    var change by remember { mutableStateOf(false) }
    val canteenText=when(canteenNumber){
        1->R.string.canteen1Text
        2->R.string.canteen3Text
        3->R.string.canteen4Text
        4->R.string.canteen5Text
        else->R.string.nullText
    }
    val timeGetTime = Date().time
    var windowText:String by remember { mutableStateOf("") }
    val buttonText = if(change)R.string.changeText else R.string.whatToEatText
    val buttonHeight by animateDpAsState(
        targetValue = if(change)750.dp else 500.dp ,
        animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing),
        label = ""
    )
    ConstraintLayout(
        Modifier.fillMaxWidth()
    ){
        val (button,text1,text2,text3,icon1,icon2) = createRefs()
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
        if (change){
            Text(
                text = stringResource(R.string.beforeCanteenText),
                //color = Color.Blue,
                fontSize = 30.sp,
                textAlign= TextAlign.Center,
                //fontFamily = FontFamily.Serif,
                modifier=Modifier
                    .constrainAs(text1) {
                        top.linkTo(parent.top, margin = 220.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
            )
        }
        Text(
            text = stringResource(canteenText),
            //color = Color.Blue,
            fontSize = 60.sp,
            textAlign= TextAlign.Center,
            //fontFamily = FontFamily.Serif,
            modifier=Modifier
                .constrainAs(text2) {
                    top.linkTo(parent.top, margin = 300.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )
        Text(
            text = windowText,
            //color = Color.Blue,
            fontSize = 60.sp,
            textAlign= TextAlign.Center,
            //fontFamily = FontFamily.Serif,
            modifier=Modifier
                .constrainAs(text3) {
                    top.linkTo(parent.top, margin = 400.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )
        Button(
            onClick =
            {
                change=true
                canteenNumber=randomCanteen()
                windowText=randomWindow(canteenNumber)
                var time = System.currentTimeMillis();
                var mCalendar:Calendar = Calendar.getInstance();
                mCalendar.setTimeInMillis(time);
                var year = mCalendar.get(Calendar.YEAR)
                var month = mCalendar.get(Calendar.MONTH)+1
                var day = mCalendar.get(Calendar.DAY_OF_MONTH)
                var apm = mCalendar.get(Calendar.AM_PM);
                GlobalScope.launch(Dispatchers.IO) {
                    val mealInfo = mealInfo()
                    mealInfo.year = year
                    mealInfo.month = month
                    mealInfo.day = day
                    mealInfo.mealNumber = apm
                    mealInfo.canteenNumber = canteenNumber
                    mealInfo.windowText = windowText
                    mealInfo.result = 5
                    mealInfoDatabase.getInstance(context).mealInfoDao().insert(mealInfo)}

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
@Composable
fun settingPage(modifier: Modifier = Modifier,
                navController: NavController
){
    ConstraintLayout(
        Modifier.fillMaxWidth()
    ){
        val (button,text1,text2,text3,icon1) = createRefs()
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
        Column (
            modifier= Modifier
                .verticalScroll(scrollState)
                .fillMaxSize()
                .constrainAs(text2) {
                    top.linkTo(parent.top, margin = 80.dp)
                    start.linkTo(parent.start, margin = 60.dp)
                }
        ){

        }
    }
}
@Composable
fun historyPage(modifier: Modifier = Modifier,
                navController: NavController
){
    ConstraintLayout(
        Modifier.fillMaxWidth()
    ){
        val (button,text1,text2,text3,icon1) = createRefs()
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

        }
    }
}
fun randomCanteen(): Int {
    var result:Int
    result=(1..4).random()
    return result
}


fun randomWindow(canteenNumber:Int): String {
    var result:Int
    result=(1..20).random()
    return "第"+result+"窗口"
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MealInTJUTheme {
        val navController= rememberNavController()
       // mainPage(navController = navController)
    }
}