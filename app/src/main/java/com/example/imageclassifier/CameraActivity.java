package com.example.imageclassifier;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.imageclassifier.databinding.ActivityCameraBinding;

public class CameraActivity extends AppCompatActivity {

    // Log 출력에 사용할 태그 이름
    private static final String TAG = "CAMERA ACTIVITY";
    // 카메라를 사용하고 응답을 받을 때 사용할 코드
    private static final int CAMERA_REQUEST_CODE = 1;

    // 이미지를 안드로이드 10.0미만 버전에서 사용하기 위한 변수
    private static final String KEY_SELECTED_URI = "KEY_SELECTED_URI";
    private Uri selectedImageUri;
    
    // 뷰 관련 변수
    ImageView imageView;
    TextView textView;
    Button selectBtn;

    // 추론 모델 관련 변수
    Classifier cls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        
        // 뷰 찾아오기
        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        selectBtn = findViewById(R.id.selectBtn);
        
        // 카메라 촬영 버튼 클릭 이벤트 처리
        selectBtn.setOnClickListener(view ->{
            // 화면에 카메라를 출력하는 코드작성
            // 갤러리 화면 출력
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, 1);
        });
        // cls 생성
        cls = new Classifier(this);
        try{
            cls.init();
        }catch(Exception e){
            Log.e(TAG, "초기화 실패");
        }
        // 이전에 저장된 번들이 있으면 읽어오기
        if (savedInstanceState != null){
            Uri uri = savedInstanceState.getParcelable(KEY_SELECTED_URI);
            if (uri != null){
                selectedImageUri = uri;
            }
        }
    }
    // startActivityForResult 를 호출해서 Activity 를 출력한 후
    // Activity가 사라지면 호출되는 메소드
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        
        // 카메라 촬영 후, 확인버튼을 눌렀을 경우에만 처리
        if(resultCode == Activity.RESULT_OK && requestCode == CAMERA_REQUEST_CODE){

            Bitmap bitmap = null;
            // 메모리가 부족한 상황을 대비하여 try catch
            try{

                if(Build.VERSION.SDK_INT >= 29){
                    bitmap = (Bitmap)data.getExtras().get("data");
                }else{
                    bitmap = MediaStore.Images.Media.getBitmap(
                            getContentResolver(),selectedImageUri);
                }
            }catch(Exception e){
                Log.e(TAG, "이미지 가져오기 실패");

            }
            // 이미지 확인
            if(bitmap != null){
                // 이미지 출력
                imageView.setImageBitmap(bitmap);
                
                // 이미지 추론
                Pair<String, Float> output = 
                        // 이미지와 방향
                        cls.classify(bitmap, 0);

                // 결과 해석
                String resultStr =
                        // 백분율을 소수 2자리 까지
                        String.format("class:%s prob:%.2f%%",
                                output.first, output.second * 100);
                
                // 출력
                textView.setText(resultStr);
            }
        }
    }
    // Activity의 상태변화가 발생했을 때 호출되는 베소드
    // 현재 상태를 저장
    // Bundle은 하나의 맵
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState){
        super.onSaveInstanceState(outState);
        // selectedImageUri 를 저장
        outState.putParcelable(KEY_SELECTED_URI, selectedImageUri);

    }
    @Override
    public void onDestroy(){
        cls.finish();
        super.onDestroy();
    }
}