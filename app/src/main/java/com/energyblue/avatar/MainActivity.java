package com.energyblue.avatar;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
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
 AvatarView portrait;TextView status,mouth;EditText endpoint;TextToSpeech tts;
 Handler handler=new Handler();boolean speaking=false;
 Runnable mouthTick=new Runnable(){boolean open=false;public void run(){if(!speaking)return;open=!open;portrait.setMouthOpen(open?0.95f:0.08f);handler.postDelayed(this,170);}};
 @Override public void onCreate(Bundle b){super.onCreate(b);
 LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(20,24,20,16);root.setBackgroundColor(Color.rgb(247,249,253));
 TextView title=new TextView(this);title.setText("EB AI 아바타 V0.5");title.setTextSize(23);title.setTypeface(null,Typeface.BOLD);root.addView(title);
 FrameLayout frame=new FrameLayout(this);portrait=new AvatarView();frame.addView(portrait,new FrameLayout.LayoutParams(-1,-1));
 mouth=new TextView(this);mouth.setText("━");mouth.setTextSize(27);mouth.setTextColor(Color.RED);mouth.setGravity(Gravity.CENTER);
 FrameLayout.LayoutParams mp=new FrameLayout.LayoutParams(55,55,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);mp.bottomMargin=55;mouth.setVisibility(View.GONE);frame.addView(mouth,mp);
 root.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
 status=new TextView(this);status.setText("사진을 선택하세요.");status.setVisibility(View.VISIBLE);root.addView(status);
 endpoint=new EditText(this);endpoint.setVisibility(View.GONE);endpoint.setText(getPreferences(0).getString("endpoint",""));
 Button photo=new Button(this);photo.setText("사진 선택");root.addView(photo);
 photo.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PHOTO);});
 Button talk=new Button(this);talk.setText("마이크로 대화하기");root.addView(talk);
 talk.setOnClickListener(v->{Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR");i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"ko-KR");i.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE,false);i.putExtra(RecognizerIntent.EXTRA_PROMPT,"한국어로 말씀해 주세요.");i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);try{startActivityForResult(i,SPEECH);}catch(Exception e){status.setText("음성 인식 앱을 열 수 없습니다. 구글 앱과 음성인식 서비스를 확인해 주세요.");}});
 EditText typed=new EditText(this);typed.setSingleLine(true);typed.setHint("음성인식 실패 시 질문 직접 입력");root.addView(typed);Button typeSend=new Button(this);typeSend.setText("입력한 질문 보내기");root.addView(typeSend);typeSend.setOnClickListener(v->{String q=typed.getText().toString().trim();if(!q.isEmpty())send(q);});
 TextView note=new TextView(this);note.setText("AI 합성 아바타 · 사진 사용 동의 필요");root.addView(note);setContentView(root);
 tts=new TextToSpeech(this,s->{if(s==TextToSpeech.SUCCESS){tts.setLanguage(Locale.KOREAN);tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener(){public void onStart(String id){runOnUiThread(()->{speaking=true;handler.post(mouthTick);});}public void onDone(String id){runOnUiThread(()->stopMouth());}public void onError(String id){runOnUiThread(()->stopMouth());}});}});
 }
 void stopMouth(){speaking=false;handler.removeCallbacks(mouthTick);mouth.setText("━");portrait.setMouthOpen(0f);}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null){if(request==SPEECH)status.setText("음성인식 결과가 없습니다. 구글 앱의 마이크 권한·한국어 언어팩·네트워크를 확인하거나 직접 입력해 주세요.");return;}
 if(request==PHOTO){Uri uri=data.getData();if(uri!=null){getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);try(InputStream stream=getContentResolver().openInputStream(uri)){portrait.setPhoto(BitmapFactory.decodeStream(stream));}catch(Exception ex){status.setText("사진을 읽지 못했습니다.");}}}
 if(request==SPEECH){ArrayList<String> words=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);if(words!=null&&!words.isEmpty())send(words.get(0));else status.setText("인식된 문장이 없습니다. 다시 말하거나 직접 입력해 주세요.");}}
 void send(String message){String url=endpoint.getText().toString().trim();if(!url.startsWith("https://")){status.setText("음성은 인식됐지만 AI 응답 서버가 설정되지 않았습니다. 서버 연결 설정이 필요합니다.");return;}
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
 class AvatarView extends View {
  Bitmap bitmap; Paint paint=new Paint(3);float mouthOpen=0f;
  AvatarView(){super(MainActivity.this);}
  void setPhoto(Bitmap b){bitmap=b;invalidate();}
  void setMouthOpen(float amount){mouthOpen=amount;invalidate();}
  @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(bitmap==null)return;
    float scale=Math.min((float)getWidth()/bitmap.getWidth(),(float)getHeight()/bitmap.getHeight());
    float width=bitmap.getWidth()*scale,height=bitmap.getHeight()*scale;
    float left=(getWidth()-width)/2f,top=(getHeight()-height)/2f;
    RectF dst=new RectF(left,top,left+width,top+height);canvas.drawBitmap(bitmap,null,dst,paint);
    if(mouthOpen<=0.01f)return;
    // V0.4 prototype: distort the original photo's mouth area rather than overlaying a symbol.
    // Assumes a front-facing centered portrait; facial landmark detection is not yet included.
    float mx=left+width*.5f,my=top+height*.73f;
    float mw=width*.15f,mh=height*.055f;
    int sx=Math.max(0,(int)(bitmap.getWidth()*.425f)),sy=Math.max(0,(int)(bitmap.getHeight()*.705f));
    int ex=Math.min(bitmap.getWidth(),(int)(bitmap.getWidth()*.575f));
    int ey=Math.min(bitmap.getHeight(),(int)(bitmap.getHeight()*.76f));
    if(ex<=sx||ey<=sy)return;
    Rect src=new Rect(sx,sy,ex,ey);
    canvas.save();canvas.clipRect(mx-mw/2,my-mh*.8f,mx+mw/2,my+mh*(.8f+mouthOpen));
    RectF warped=new RectF(mx-mw/2,my-mh*(.25f+mouthOpen*.25f),mx+mw/2,my+mh*(.65f+mouthOpen));
    canvas.drawBitmap(bitmap,src,warped,paint);canvas.restore();
  }
 }
 @Override protected void onDestroy(){stopMouth();if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
}