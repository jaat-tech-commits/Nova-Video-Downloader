package com.nova.videodownloader
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity: ComponentActivity(){
 override fun onCreate(b:Bundle?){
  super.onCreate(b)
  val shared=if(intent?.action==Intent.ACTION_SEND) intent.getStringExtra(Intent.EXTRA_TEXT) else null
  setContent{NovaApp(shared)}
 }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun NovaApp(shared:String?,vm:DownloaderViewModel=viewModel()){
 var url by remember{mutableStateOf(shared.orEmpty())}
 var tab by remember{mutableIntStateOf(0)}
 val items by vm.items.collectAsState()
 Scaffold(topBar={TopAppBar(title={Text("Nova Video Downloader")})},bottomBar={
  NavigationBar{
   NavigationBarItem(tab==0,{tab=0},icon={Icon(Icons.Default.Download,null)},label={Text("Download")})
   NavigationBarItem(tab==1,{tab=1},icon={Icon(Icons.Default.History,null)},label={Text("History")})
  }
 }){pad->
  Column(Modifier.padding(pad).padding(16.dp).fillMaxSize()){
   if(tab==0){
    Text("Download from a URL",style=MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(url,{url=it},Modifier.fillMaxWidth(),label={Text("Paste video/page URL")},singleLine=true)
    Spacer(Modifier.height(10.dp))
    Button({vm.add(url);url=""},enabled=url.startsWith("http://")||url.startsWith("https://"),modifier=Modifier.fillMaxWidth()){
     Icon(Icons.Default.Download,null);Spacer(Modifier.width(8.dp));Text("Start download")
    }
    Spacer(Modifier.height(20.dp))
   }else Text("Download history",style=MaterialTheme.typography.headlineSmall)
   LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(items,key={it.id}){DownloadRow(it,vm)}}
  }
 }
}
@Composable fun DownloadRow(item:DownloadItem,vm:DownloaderViewModel){
 ElevatedCard(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){
  Text(item.fileName,style=MaterialTheme.typography.titleMedium)
  Text(item.status,style=MaterialTheme.typography.bodySmall)
  if(item.total>0){
   LinearProgressIndicator(progress={item.progress/100f},Modifier.fillMaxWidth().padding(vertical=8.dp))
   Text(item.progress.toString()+"% • "+item.speedText)
  }
  Row{
   if(item.status=="Downloading")IconButton({vm.cancel(item.id)}){Icon(Icons.Default.Close,"Cancel")}
   if(item.status.startsWith("Failed"))IconButton({vm.retry(item.id)}){Icon(Icons.Default.Refresh,"Retry")}
  }
 }}
}