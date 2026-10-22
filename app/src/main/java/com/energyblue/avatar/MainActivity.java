package com.energyblue.avatar;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Matrix;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
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
import org.json.JSONArray;
import android.text.InputType;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
public class MainActivity extends Activity {
 static final int PHOTO=10,SPEECH=11;
 AvatarView portrait;TextView status,mouth;TextToSpeech tts;FaceLandmarker faceLandmarker;
 Handler handler=new Handler();boolean speaking=false;EditText apiKeyField;boolean requestBusy=false;
 Runnable mouthTick=new Runnable(){boolean open=false;public void run(){if(!speaking)return;open=!open;portrait.setMouthOpen(open?0.95f:0.08f);handler.postDelayed(this,125);}};
 @Override public void onCreate(Bundle b){super.onCreate(b);
 LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(20,56,20,16);root.setBackgroundColor(Color.rgb(247,249,253));
 TextView title=new TextView(this);title.setText("EB AI 아바타 V1.4 · 오프라인 얼굴 움직임");title.setTextSize(23);title.setTypeface(null,Typeface.BOLD);root.addView(title);
 FrameLayout frame=new FrameLayout(this);portrait=new AvatarView();frame.addView(portrait,new FrameLayout.LayoutParams(-1,-1));
 mouth=new TextView(this);mouth.setText("━");mouth.setTextSize(27);mouth.setTextColor(Color.RED);mouth.setGravity(Gravity.CENTER);
 FrameLayout.LayoutParams mp=new FrameLayout.LayoutParams(55,55,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);mp.bottomMargin=55;mouth.setVisibility(View.GONE);frame.addView(mouth,mp);
 root.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
 TextView guide=new TextView(this);guide.setText("MediaPipe가 얼굴 위치를 자동 인식합니다. 검은 입술 효과는 제거했습니다.");root.addView(guide);
 status=new TextView(this);status.setText("사진을 선택하세요.");status.setVisibility(View.VISIBLE);root.addView(status);
 Button demo=new Button(this);demo.setText("사진 움직임 / 음성 테스트");root.addView(demo);demo.setOnClickListener(v->speak("안녕하세요. 사진 아바타가 말하고 있습니다. 입 움직임을 확인해 주세요."));
 Button photo=new Button(this);photo.setText("사진 선택");root.addView(photo);
 photo.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PHOTO);});
 apiKeyField=new EditText(this);apiKeyField.setSingleLine(true);apiKeyField.setHint("OpenAI API 키 입력 (앱 종료 시 삭제)");
 apiKeyField.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
 root.addView(apiKeyField);
 TextView keyNote=new TextView(this);keyNote.setText("키는 앱에 저장하지 않습니다. 개인 테스트용이며 안전한 운영에는 별도 인증 서버가 필요합니다.");keyNote.setTextSize(12);root.addView(keyNote);
 Button talk=new Button(this);talk.setText("마이크로 대화하기");root.addView(talk);
 talk.setOnClickListener(v->{Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR");i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"ko-KR");i.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE,false);i.putExtra(RecognizerIntent.EXTRA_PROMPT,"한국어로 말씀해 주세요.");i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);try{startActivityForResult(i,SPEECH);}catch(Exception e){status.setText("음성 인식 앱을 열 수 없습니다. 구글 앱과 음성인식 서비스를 확인해 주세요.");}});
 EditText typed=new EditText(this);typed.setSingleLine(true);typed.setHint("음성인식 실패 시 질문 직접 입력");root.addView(typed);Button typeSend=new Button(this);typeSend.setText("입력한 질문 보내기");root.addView(typeSend);typeSend.setOnClickListener(v->{String q=typed.getText().toString().trim();if(!q.isEmpty())send(q);});
 TextView note=new TextView(this);note.setText("AI 합성 아바타 · 사진 사용 동의 필요");root.addView(note);setContentView(root);
 tts=new TextToSpeech(this,s->{if(s==TextToSpeech.SUCCESS){tts.setLanguage(Locale.KOREAN);tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener(){public void onStart(String id){runOnUiThread(()->{speaking=true;handler.post(mouthTick);});}public void onDone(String id){runOnUiThread(()->stopMouth());}public void onError(String id){runOnUiThread(()->stopMouth());}});}});
 }
 void detectFace(Bitmap b){
   if(b==null)return;
   status.setText("MediaPipe 얼굴 인식 중...");
   new Thread(()->{
     try {
       if(faceLandmarker==null){
         BaseOptions base=BaseOptions.builder().setModelAssetPath("face_landmarker.task").build();
         FaceLandmarker.FaceLandmarkerOptions options=FaceLandmarker.FaceLandmarkerOptions.builder()
           .setBaseOptions(base).setNumFaces(1).build();
         faceLandmarker=FaceLandmarker.createFromOptions(getApplicationContext(),options);
       }
       Bitmap argb=b.getConfig()==Bitmap.Config.ARGB_8888?b:b.copy(Bitmap.Config.ARGB_8888,false);
       FaceLandmarkerResult result=faceLandmarker.detect(new BitmapImageBuilder(argb).build());
       runOnUiThread(()->{
         if(result.faceLandmarks().isEmpty()){status.setText("얼굴을 인식하지 못했습니다. 입술 중앙을 터치해 주세요.");return;}
         portrait.setLandmarks(result.faceLandmarks().get(0));
         status.setText("얼굴·눈·입술 자동 인식 완료. 음성 테스트를 눌러 주세요.");
       });
     }catch(Exception e){runOnUiThread(()->status.setText("얼굴 자동 인식 실패: "+e.getClass().getSimpleName()+". 입술을 직접 터치해 주세요."));}
   }).start();
 }
 void stopMouth(){speaking=false;handler.removeCallbacks(mouthTick);mouth.setText("━");portrait.setMouthOpen(0f);}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null){if(request==SPEECH)status.setText("음성인식 결과가 없습니다. 구글 앱의 마이크 권한·한국어 언어팩·네트워크를 확인하거나 직접 입력해 주세요.");return;}
 if(request==PHOTO){Uri uri=data.getData();if(uri!=null){getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);try(InputStream stream=getContentResolver().openInputStream(uri)){Bitmap photoBitmap=BitmapFactory.decodeStream(stream);portrait.setPhoto(photoBitmap);detectFace(photoBitmap);}catch(Exception ex){status.setText("사진을 읽지 못했습니다.");}}}
 if(request==SPEECH){ArrayList<String> words=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);if(words!=null&&!words.isEmpty())send(words.get(0));else status.setText("인식된 문장이 없습니다. 다시 말하거나 직접 입력해 주세요.");}}
 void send(String message){
   String key=apiKeyField.getText().toString().trim();
   if(key.isEmpty()){status.setText("OpenAI API 키를 입력해 주세요. 키가 없으면 음성 테스트만 가능합니다.");return;}
   if(requestBusy){status.setText("이전 AI 답변을 기다리는 중입니다.");return;}
   requestBusy=true;status.setText("질문: "+message+"\\nAI 답변 생성 중...");
   new Thread(()->{
     HttpURLConnection conn=null;
     try{
       URL url=new URL("https://api.openai.com/v1/chat/completions");
       conn=(HttpURLConnection)url.openConnection();conn.setRequestMethod("POST");
       conn.setConnectTimeout(15000);conn.setReadTimeout(45000);
       conn.setDoOutput(true);conn.setRequestProperty("Authorization","Bearer "+key);
       conn.setRequestProperty("Content-Type","application/json; charset=utf-8");
       JSONObject body=new JSONObject();body.put("model","gpt-4o-mini");
       JSONArray messages=new JSONArray();
       messages.put(new JSONObject().put("role","system").put("content","당신은 한국어로 자연스럽고 간결하게 대화하는 친절한 AI 아바타입니다. 음성으로 들려줄 답변이므로 짧고 명확하게 답하세요."));
       messages.put(new JSONObject().put("role","user").put("content",message));
       body.put("messages",messages);body.put("max_tokens",300);
       try(OutputStream out=conn.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}
       int code=conn.getResponseCode();
       InputStream stream=code>=200&&code<300?conn.getInputStream():conn.getErrorStream();
       ByteArrayOutputStream buffer=new ByteArrayOutputStream();
       if(stream!=null)try(InputStream input=stream){byte[] chunk=new byte[4096];int n;while((n=input.read(chunk))!=-1){if(buffer.size()>100000)break;buffer.write(chunk,0,n);}}
       String response=new String(buffer.toByteArray(),StandardCharsets.UTF_8);
       if(code<200||code>=300){final String err=code==401?"API 키 인증 실패(401)":code==429?"요청 한도 또는 결제 한도 초과(429)":"API 오류 HTTP "+code;runOnUiThread(()->status.setText(err));return;}
       String answer=new JSONObject(response).getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content","").trim();
       if(answer.isEmpty())throw new IOException("Empty AI answer");
       runOnUiThread(()->speak(answer));
     }catch(Exception e){runOnUiThread(()->status.setText("AI 연결 실패: "+e.getClass().getSimpleName()+". 인터넷과 API 설정을 확인해 주세요."));}
     finally{if(conn!=null)conn.disconnect();runOnUiThread(()->requestBusy=false);}
   }).start();
 }
 void speak(String message){if(tts==null){status.setText("음성 엔진 준비 중입니다.");return;}status.setText("아바타 음성 출력: "+message);tts.speak(message,TextToSpeech.QUEUE_FLUSH,null,"reply");}
  class AvatarView extends View {
  Bitmap bitmap; Paint paint=new Paint(3);float mouthOpen=0f;float mouthX=.5f,mouthY=.46f;boolean mouthSelected=false;float faceCX=.5f,faceCY=.4f,faceRX=.18f,faceRY=.25f;boolean detected=false;long animationStart=android.os.SystemClock.uptimeMillis();
  AvatarView(){super(MainActivity.this);}
  void setPhoto(Bitmap b){bitmap=b;mouthSelected=false;detected=false;mouthX=.5f;mouthY=.46f;invalidate();}
  void setLandmarks(java.util.List<NormalizedLandmark> lm){
    if(lm.size()<400)return;
    NormalizedLandmark top=lm.get(10),chin=lm.get(152),left=lm.get(234),right=lm.get(454);
    NormalizedLandmark lipTop=lm.get(13),lipBottom=lm.get(14);
    mouthX=(lipTop.x()+lipBottom.x())*.5f;mouthY=(lipTop.y()+lipBottom.y())*.5f;
    faceCX=(left.x()+right.x())*.5f;faceCY=(top.y()+chin.y())*.5f;
    faceRX=Math.abs(right.x()-left.x())*.53f;faceRY=Math.abs(chin.y()-top.y())*.53f;
    detected=true;mouthSelected=true;invalidate();
  }
  @Override public boolean onTouchEvent(MotionEvent event){if(event.getAction()!=MotionEvent.ACTION_UP||bitmap==null)return true;float scale=Math.min((float)getWidth()/bitmap.getWidth(),(float)getHeight()/bitmap.getHeight());float w=bitmap.getWidth()*scale,h=bitmap.getHeight()*scale,l=(getWidth()-w)/2f,t=(getHeight()-h)/2f;float x=(event.getX()-l)/w,y=(event.getY()-t)/h;if(x>=0&&x<=1&&y>=0&&y<=1){mouthX=x;mouthY=y;mouthSelected=true;status.setText("입술 위치 지정 완료. 음성 테스트를 눌러 확인하세요.");invalidate();}return true;}
  void setMouthOpen(float amount){mouthOpen=amount;invalidate();}
  @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(bitmap==null)return;
    float scale=Math.min((float)getWidth()/bitmap.getWidth(),(float)getHeight()/bitmap.getHeight());
    float width=bitmap.getWidth()*scale,height=bitmap.getHeight()*scale;
    float left=(getWidth()-width)/2f,top=(getHeight()-height)/2f;
    RectF dst=new RectF(left,top,left+width,top+height);canvas.drawBitmap(bitmap,null,dst,paint);
    if(!detected)return;
    float mx=left+width*mouthX,my=top+height*mouthY;
    // Offline micro-motion only: no fabricated mouth or skin patches.
    if(detected){
      float cx=left+width*faceCX,cy=top+height*faceCY;
      float rx=width*faceRX,ry=height*faceRY;
      canvas.save();
      Path mask=new Path();
      mask.addOval(new RectF(cx-rx,cy-ry,cx+rx,cy+ry),Path.Direction.CW);
      canvas.clipPath(mask);
      canvas.rotate((float)Math.sin(android.os.SystemClock.uptimeMillis()*.003)*1.3f,cx,cy);
      canvas.translate((float)Math.sin(android.os.SystemClock.uptimeMillis()*.002)*width*.002f,-height*.0015f*mouthOpen);
      canvas.drawBitmap(bitmap,null,dst,paint);
      canvas.restore();
    }
    // Never paint artificial dark mouth shapes over the user's original lips.
    // Natural lip-sync and blinking require a real on-device image animation model.

  }
 }
 @Override protected void onDestroy(){stopMouth();if(tts!=null){tts.stop();tts.shutdown();}if(faceLandmarker!=null)faceLandmarker.close();super.onDestroy();}
}