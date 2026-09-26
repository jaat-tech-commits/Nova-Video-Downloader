package com.nova.videodownloader

import android.app.Activity
import android.os.Bundle
import android.webkit.*
import android.widget.*
import android.graphics.Color

class BrowserActivity:Activity(){
 private val detected=LinkedHashSet<String>()
 private lateinit var web:WebView
 private lateinit var status:TextView

 override fun onCreate(state:Bundle?){
  super.onCreate(state)
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
  val bar=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
  val input=EditText(this).apply{hint="Enter website URL";setSingleLine(true);setText(intent.getStringExtra("url").orEmpty());layoutParams=LinearLayout.LayoutParams(0,48).apply{weight=1f}}
  val go=Button(this).apply{setText("Go")}
  val download=Button(this).apply{setText("Download")}
  status=TextView(this).apply{setTextColor(Color.DKGRAY);setPadding(12,6,12,6);text="Detected media: 0"}
  bar.addView(input);bar.addView(go);bar.addView(download)
  root.addView(bar);root.addView(status)
  web=WebView(this)
  web.settings.javaScriptEnabled=true
  web.settings.domStorageEnabled=true
  web.settings.mediaPlaybackRequiresUserGesture=false
  web.settings.userAgentString=MediaResolver.USER_AGENT
  web.webViewClient=object:WebViewClient(){
   override fun shouldInterceptRequest(view:WebView?,request:WebResourceRequest?):WebResourceResponse?{
    val u=request?.url?.toString().orEmpty()
    if(isMedia(u)) synchronized(detected){detected.add(u)}
    runOnUiThread{status.text="Detected media: "+detected.size}
    return super.shouldInterceptRequest(view,request)
   }
   override fun onPageFinished(view:WebView?,url:String?){
    status.text="Detected media: "+detected.size
    view?.evaluateJavascript("(function(){var a=[];document.querySelectorAll('video,source').forEach(function(v){if(v.src)a.push(v.src);});return JSON.stringify(a);})()"){raw->
     raw.replace("\\","").removePrefix("\"").removeSuffix("\"").split(",").forEach{candidate->
      val u=candidate.trim().trim('"','[',']')
      if(isMedia(u))synchronized(detected){detected.add(u)}
     }
     status.text="Detected media: "+detected.size
    }
   }
  }
  root.addView(web,LinearLayout.LayoutParams(-1,0).apply{weight=1f})
  setContentView(root)

  fun navigate(){
   var u=input.text.toString().trim()
   if(u.isNotBlank()&&!u.startsWith("http://")&&!u.startsWith("https://"))u="https://"+u
   if(u.isNotBlank())web.loadUrl(u)
  }
  go.setOnClickListener{navigate()}
  download.setOnClickListener{
   val url=synchronized(detected){detected.lastOrNull()}
   if(url==null)Toast.makeText(this,"No public media URL detected yet",Toast.LENGTH_SHORT).show()
   else{setResult(Activity.RESULT_OK,intent.putExtra("media_url",url));finish()}
  }
  navigate()
 }

 private fun isMedia(url:String):Boolean{
  val x=url.lowercase()
  return x.contains(".mp4")||x.contains(".webm")||x.contains(".m4v")||x.contains(".mov")||x.contains(".m3u8")||x.contains(".mp3")||x.contains(".m4a")
 }
}
