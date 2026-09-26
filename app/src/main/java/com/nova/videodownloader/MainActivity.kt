package com.nova.videodownloader

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity:ComponentActivity(){
 override fun onCreate(b:Bundle?){
  super.onCreate(b)
  val shared=if(intent?.action==Intent.ACTION_SEND)intent.getStringExtra(Intent.EXTRA_TEXT) else null
  setContent{NovaApp(shared)}
 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun NovaApp(shared:String?,vm:DownloaderViewModel=viewModel()){
 var url by remember{mutableStateOf(shared.orEmpty())}
 var tab by remember{mutableIntStateOf(0)}
 val items by vm.items.collectAsState()
 val context=LocalContext.current
 val browser=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
  result.data?.getStringExtra("media_url")?.let{vm.add(it)}
 }

 LaunchedEffect(Unit){
  if(url.isBlank()){
   val cb=context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
   val text=cb.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
   if(text.startsWith("http://")||text.startsWith("https://"))url=text
  }
 }

 Scaffold(
  topBar={TopAppBar(title={Text("Nova Downloader")},actions={
   IconButton({url=""}){Icon(Icons.Default.Clear,"Clear")}
  })},
  bottomBar={NavigationBar{
   NavigationBarItem(tab==0,{tab=0},icon={Icon(Icons.Default.Home,null)},label={Text("Home")})
   NavigationBarItem(tab==1,{tab=1},icon={Icon(Icons.Default.Download,null)},label={Text("Downloads")})
   NavigationBarItem(tab==2,{tab=2},icon={Icon(Icons.Default.History,null)},label={Text("History")})
   NavigationBarItem(tab==3,{tab=3},icon={Icon(Icons.Default.Settings,null)},label={Text("Settings")})
  }}
 ){pad->
  Column(Modifier.padding(pad).padding(16.dp).fillMaxSize()){
   when(tab){
    0->Home(url,{url=it},{vm.analyzeAndAdd(url);url=""},{vm.add(url);url=""},{browser.launch(Intent(context,BrowserActivity::class.java).apply{putExtra("url",url)})})
    1->DownloadList(items,vm)
    2->DownloadList(items,vm)
    3->SettingsScreen(vm)
   }
  }
 }
}

@Composable private fun Home(url:String,onUrl:(String)->Unit,onAnalyze:()->Unit,onDirect:()->Unit,onBrowser:()->Unit){
 Text("Download video & audio",style=MaterialTheme.typography.headlineSmall)
 Text("Paste a page URL and Nova will try to discover public media sources.",style=MaterialTheme.typography.bodyMedium)
 Spacer(Modifier.height(14.dp))
 OutlinedTextField(url,onUrl,Modifier.fillMaxWidth(),label={Text("Video/page URL")},singleLine=true)
 Spacer(Modifier.height(10.dp))
 Button(onAnalyze,enabled=url.startsWith("http://")||url.startsWith("https://"),modifier=Modifier.fillMaxWidth()){
  Icon(Icons.Default.Search,null);Spacer(Modifier.width(8.dp));Text("Analyze & Download")
 }
 Spacer(Modifier.height(8.dp))
 OutlinedButton(onDirect,enabled=url.startsWith("http://")||url.startsWith("https://"),modifier=Modifier.fillMaxWidth()){
  Icon(Icons.Default.Download,null);Spacer(Modifier.width(8.dp));Text("Direct Download")
 }
 OutlinedButton(onBrowser,modifier=Modifier.fillMaxWidth()){
  Icon(Icons.Default.Language,null);Spacer(Modifier.width(8.dp));Text("Open In-App Browser & Detect")
 }
 Spacer(Modifier.height(18.dp))
 Feature("Smart extraction","Looks for og:video, video/source tags and common MP4/WebM/HLS URLs.")
 Feature("Queue","Run multiple downloads with progress, speed, cancel and retry.")
 Feature("Resume","HTTP Range requests are used when the server supports them.")
 Feature("Public storage","Completed files are saved under Downloads/Nova.")
}

@Composable private fun Feature(title:String,text:String){
 ElevatedCard(Modifier.fillMaxWidth().padding(bottom=8.dp)){Column(Modifier.padding(13.dp)){
  Text(title,style=MaterialTheme.typography.titleMedium)
  Text(text,style=MaterialTheme.typography.bodySmall)
 }}
}

@Composable private fun DownloadList(items:List<DownloadItem>,vm:DownloaderViewModel){
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
  Text("Downloads",style=MaterialTheme.typography.headlineSmall)
  TextButton({vm.clearHistory()}){Text("Clear")}
 }
 Spacer(Modifier.height(8.dp))
 if(items.isEmpty())Text("Nothing here yet.")
 LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){
  items(items,key={it.id}){item->
   ElevatedCard(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){
    Text(item.fileName,style=MaterialTheme.typography.titleMedium)
    Text(item.type+" • "+item.status,style=MaterialTheme.typography.bodySmall)
    if(item.total>0){
     LinearProgressIndicator(progress={item.progress/100f},Modifier.fillMaxWidth().padding(vertical=7.dp))
     Text(item.progress.toString()+"% • "+item.speedText)
    }
    Row{
     if(item.status=="Downloading")IconButton({vm.cancel(item.id)}){Icon(Icons.Default.Close,"Cancel")}
     if(item.status.startsWith("Failed"))IconButton({vm.retry(item.id)}){Icon(Icons.Default.Refresh,"Retry")}
     IconButton({vm.remove(item.id)}){Icon(Icons.Default.Delete,"Remove")}
    }
   }}
  }
 }
}

@Composable private fun SettingsScreen(vm:DownloaderViewModel){
 Text("Settings",style=MaterialTheme.typography.headlineSmall)
 Spacer(Modifier.height(12.dp))
 Text("This build targets public/direct media. It does not bypass DRM, paywalls or login/security restrictions.")
 Spacer(Modifier.height(12.dp))
 OutlinedButton({vm.clearHistory()},modifier=Modifier.fillMaxWidth()){Text("Clear download history")}
}
