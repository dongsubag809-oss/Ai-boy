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
import android.view.MotionEvent;
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
 AvatarView portrait;TextView status,mouth;TextToSpeech tts;
 Handler handler=new Handler();boolean speaking=false;
 Runnable mouthTick=new Runnable(){boolean open=false;public void run(){if(!speaking)return;open=!open;portrait.setMouthOpen(open?0.95f:0.08f);handler.postDelayed(this,170);}};
 @Override public void onCreate(Bundle b){super.onCreate(b);
 LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(20,56,20,16);root.setBackgroundColor(Color.rgb(247,249,253));
 TextView title=new TextView(this);title.setText("EB AI 아바타 V1.1");title.setTextSize(23);title.setTypeface(null,Typeface.BOLD);root.addView(title);
 FrameLayout frame=new FrameLayout(this);portrait=new AvatarView();frame.addView(portrait,new FrameLayout.LayoutParams(-1,-1));
 mouth=new TextView(this);mouth.setText("━");mouth.setTextSize(27);mouth.setTextColor(Color.RED);mouth.setGravity(Gravity.CENTER);
 FrameLayout.LayoutParams mp=new FrameLayout.LayoutParams(55,55,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);mp.bottomMargin=55;mouth.setVisibility(View.GONE);frame.addView(mouth,mp);
 root.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
 TextView guide=new TextView(this);guide.setText("사진을 선택한 후 입술 중앙을 한 번 터치하세요.");root.addView(guide);
 status=new TextView(this);status.setText("사진을 선택하세요.");status.setVisibility(View.VISIBLE);root.addView(status);
 Button demo=new Button(this);demo.setText("사진 움직임 / 음성 테스트");root.addView(demo);demo.setOnClickListener(v->speak("안녕하세요. 사진 아바타가 말하고 있습니다. 입 움직임을 확인해 주세요."));
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
 void send(String message){status.setText("인식: "+message+"\n서버 없이 음성으로 따라 말합니다.");speak(message);}
 void speak(String message){if(tts==null){status.setText("음성 엔진 준비 중입니다.");return;}status.setText("아바타 음성 출력: "+message);tts.speak(message,TextToSpeech.QUEUE_FLUSH,null,"reply");}
  class AvatarView extends View {
  Bitmap bitmap; Paint paint=new Paint(3);float mouthOpen=0f;float mouthX=.5f,mouthY=.46f;boolean mouthSelected=false;
  AvatarView(){super(MainActivity.this);}
  void setPhoto(Bitmap b){bitmap=b;mouthSelected=false;mouthX=.5f;mouthY=.46f;invalidate();}
  @Override public boolean onTouchEvent(MotionEvent event){if(event.getAction()!=MotionEvent.ACTION_UP||bitmap==null)return true;float scale=Math.min((float)getWidth()/bitmap.getWidth(),(float)getHeight()/bitmap.getHeight());float w=bitmap.getWidth()*scale,h=bitmap.getHeight()*scale,l=(getWidth()-w)/2f,t=(getHeight()-h)/2f;float x=(event.getX()-l)/w,y=(event.getY()-t)/h;if(x>=0&&x<=1&&y>=0&&y<=1){mouthX=x;mouthY=y;mouthSelected=true;status.setText("입술 위치 지정 완료. 음성 테스트를 눌러 확인하세요.");invalidate();}return true;}
  void setMouthOpen(float amount){mouthOpen=amount;invalidate();}
  @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(bitmap==null)return;
    float scale=Math.min((float)getWidth()/bitmap.getWidth(),(float)getHeight()/bitmap.getHeight());
    float width=bitmap.getWidth()*scale,height=bitmap.getHeight()*scale;
    float left=(getWidth()-width)/2f,top=(getHeight()-height)/2f;
    RectF dst=new RectF(left,top,left+width,top+height);canvas.drawBitmap(bitmap,null,dst,paint);
    if(mouthOpen<=0.01f||!mouthSelected)return;
    // Subtle jaw opening: preserve the original upper lip and cheeks.
    float mx=left+width*mouthX,my=top+height*mouthY;
    float mw=width*.115f;
    float gap=Math.min(width*.020f,height*.012f)*mouthOpen;
    float lipH=Math.min(width*.012f,height*.008f);
    // Dark interior appears only between the lips, rather than stretching the photograph.
    Paint inside=new Paint(3);
    inside.setColor(Color.rgb(65,23,30));
    RectF opening=new RectF(mx-mw*.40f,my-gap*.38f,mx+mw*.40f,my+gap*.65f);
    canvas.drawOval(opening,inside);
    // Keep the lip edge soft and close to the source photograph.
    Paint lip=new Paint(3);
    lip.setStyle(Paint.Style.STROKE);
    lip.setStrokeWidth(Math.max(1f,lipH*.42f));
    lip.setColor(Color.argb(140,116,57,65));
    android.graphics.Path upper=new android.graphics.Path();
    upper.moveTo(mx-mw*.45f,my);
    upper.quadTo(mx,my-gap*.52f,mx+mw*.45f,my);
    canvas.drawPath(upper,lip);
    android.graphics.Path lower=new android.graphics.Path();
    lower.moveTo(mx-mw*.45f,my);
    lower.quadTo(mx,my+gap*.84f,mx+mw*.45f,my);
    canvas.drawPath(lower,lip);
  }
 }
 @Override protected void onDestroy(){stopMouth();if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
}