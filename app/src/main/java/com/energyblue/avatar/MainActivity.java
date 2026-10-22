package com.energyblue.avatar;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.widget.*;
import android.net.Uri;
import android.os.Handler;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
public class MainActivity extends Activity {
 static final int PHOTO=10,SPEECH=11;
 ImageView portrait;TextView status,mouth;EditText endpoint;TextToSpeech tts;
 Handler handler=new Handler();boolean speaking=false;
 Runnable mouthTick=new Runnable(){boolean open=false;public void run(){if(!speaking)return;open=!open;mouth.setText(open?"●":"━");handler.postDelayed(this,170);}};
 @Override public void onCreate(Bundle b){super.onCreate(b);
 LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(20,24,20,16);root.setBackgroundColor(Color.rgb(247,249,253));
 TextView title=new TextView(this);title.setText("EB AI 아바타 V0.3");title.setTextSize(23);title.setTypeface(null,Typeface.BOLD);root.addView(title);
 FrameLayout frame=new FrameLayout(this);portrait=new ImageView(this);portrait.setScaleType(ImageView.ScaleType.FIT_CENTER);frame.addView(portrait,new FrameLayout.LayoutParams(-1,-1));
 mouth=new TextView(this);mouth.setText("━");mouth.setTextSize(27);mouth.setTextColor(Color.RED);mouth.setGravity(Gravity.CENTER);
 FrameLayout.LayoutParams mp=new FrameLayout.LayoutParams(55,55,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);mp.bottomMargin=55;frame.addView(mouth,mp);
 root.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
 status=new TextView(this);status.setText("사진을 선택하고 HTTPS 서버 주소를 설정하세요.");root.addView(status);
 endpoint=new EditText(this);endpoint.setSingleLine(true);endpoint.setHint("HTTPS AI 서버 URL");endpoint.setText(getPreferences(0).getString("endpoint",""));root.addView(endpoint);
 Button photo=new Button(this);photo.setText("사진 선택");root.addView(photo);
 photo.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PHOTO);});
 Button talk=new Button(this);talk.setText("마이크로 대화하기");root.addView(talk);
 talk.setOnClickListener(v->{Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR");i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);try{startActivityForResult(i,SPEECH);}catch(Exception e){status.setText("음성 인식 앱을 사용할 수 없습니다.");}});
 TextView note=new TextView(this);note.setText("AI 합성 아바타 · 사진 사용 동의 필요");root.addView(note);setContentView(root);
 tts=new TextToSpeech(this,s->{if(s==TextToSpeech.SUCCESS){tts.setLanguage(Locale.KOREAN);tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener(){public void onStart(String id){runOnUiThread(()->{speaking=true;handler.post(mouthTick);});}public void onDone(String id){runOnUiThread(()->stopMouth());}public void onError(String id){runOnUiThread(()->stopMouth());}});}});
 }
 void stopMouth(){speaking=false;handler.removeCallbacks(mouthTick);mouth.setText("━");}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null)return;
 if(request==PHOTO){Uri uri=data.getData();if(uri!=null){getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);portrait.setImageURI(uri);}}
 if(request==SPEECH){ArrayList<String> words=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);if(words!=null&&!words.isEmpty())send(words.get(0));}}
 void send(String message){String url=endpoint.getText().toString().trim();if(!url.startsWith("https://")){status.setText("HTTPS 서버 주소가 필요합니다.");return;}
 getPreferences(0).edit().putString("endpoint",url).apply();status.setText("질문: "+message+"\n응답 대기 중...");
 new Thread(()->{String answer;HttpURLConnection c=null;try{
 c=(HttpURLConnection)new URL(url).openConnection();c.setRequestMethod("POST");c.setConnectTimeout(8000);c.setReadTimeout(20000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=utf-8");
 byte[] bytes=new JSONObject().put("message",message).toString().getBytes(StandardCharsets.UTF_8);
 try(OutputStream os=c.getOutputStream()){os.write(bytes);}
 if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());
 ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(out.size()+n>65536)throw new IOException("응답이 너무 큽니다");out.write(buf,0,n);}}
 answer=new JSONObject(out.toString("UTF-8")).getString("reply");
 }catch(Exception e){answer="연결 오류: "+e.getMessage();}finally{if(c!=null)c.disconnect();}
 String resultText=answer;runOnUiThread(()->{status.setText(resultText);if(!resultText.startsWith("연결 오류:"))tts.speak(resultText,TextToSpeech.QUEUE_FLUSH,null,"reply");});}).start();}
 @Override protected void onDestroy(){stopMouth();if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
}