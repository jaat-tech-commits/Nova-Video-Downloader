package com.nova.videodownloader
import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID
import kotlin.math.max

data class DownloadItem(val id:String=UUID.randomUUID().toString(),val url:String,val fileName:String,val progress:Int=0,val total:Long=0,val status:String="Queued",val speedText:String="Waiting")

class DownloaderViewModel(app:Application):AndroidViewModel(app){
 private val _items=MutableStateFlow<List<DownloadItem>>(emptyList())
 val items=_items.asStateFlow()
 private val jobs=mutableMapOf<String,Job>()
 private val client=OkHttpClient.Builder().followRedirects(true).build()

 fun add(url:String){
  if(url.isBlank())return
  val item=DownloadItem(url=url,fileName=guessName(url))
  _items.value=listOf(item)+_items.value
  start(item)
 }
 fun retry(id:String){_items.value.find{it.id==id}?.let{start(it.copy(status="Queued",progress=0))}}
 fun cancel(id:String){jobs.remove(id)?.cancel();update(id){it.copy(status="Cancelled")}}

 private fun start(item:DownloadItem){
  jobs[item.id]?.cancel()
  jobs[item.id]=viewModelScope.launch(Dispatchers.IO){
   try{
    update(item.id){it.copy(status="Downloading")}
    val dir=getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
    dir.mkdirs()
    var file=File(dir,safeName(item.fileName))
    if(file.exists()&&file.length()>0)file=File(dir,safeName(System.currentTimeMillis().toString()+"_"+item.fileName))
    var existing=file.length()
    val builder=Request.Builder().url(item.url)
    if(existing>0)builder.header("Range","bytes="+existing+"-")
    client.newCall(builder.build()).execute().use{res->
     if(!res.isSuccessful)error("HTTP "+res.code)
     val body=res.body?:error("Empty response")
     val append=existing>0&&res.code==206
     if(!append)existing=0
     val total=max(0L,existing+body.contentLength())
     val raf=RandomAccessFile(file,"rw");raf.seek(existing)
     body.byteStream().use{input->
      val buffer=ByteArray(65536);var done=existing;var last=System.currentTimeMillis();var lastBytes=done
      while(true){
       ensureActive()
       val n=input.read(buffer)
       if(n<0)break
       raf.write(buffer,0,n);done+=n
       val now=System.currentTimeMillis()
       if(now-last>=500){
        val speed=(done-lastBytes)*1000L/max(1L,now-last)
        val pct=if(total>0)((done*100)/total).toInt() else 0
        update(item.id){it.copy(fileName=file.name,progress=pct,total=total,speedText=formatSpeed(speed))}
        last=now;lastBytes=done
       }
      }
     }
     raf.close()
     update(item.id){it.copy(fileName=file.name,progress=100,total=total,status="Completed",speedText="Done")}
    }
   }catch(e:CancellationException){update(item.id){it.copy(status="Cancelled")}}
   catch(e:Exception){update(item.id){it.copy(status="Failed: "+(e.message?:"Unknown error"))}}
  }
 }
 private fun update(id:String,f:(DownloadItem)->DownloadItem){_items.value=_items.value.map{if(it.id==id)f(it)else it}}
 private fun guessName(url:String):String=runCatching{
  val p=java.net.URI(url).path.substringAfterLast('/')
  if(p.isBlank())"video_"+System.currentTimeMillis()+".mp4" else safeName(p)
 }.getOrDefault("video_"+System.currentTimeMillis()+".mp4")
 private fun safeName(s:String)=s.replace(Regex("[\\/:*?\"<>|]"),"_").take(180).ifBlank{"video.mp4"}
 private fun formatSpeed(b:Long)=when{b>=1024*1024->"%.1f MB/s".format(b/1024f/1024f);b>=1024->"%.0f KB/s".format(b/1024f);else->b.toString()+" B/s"}
}