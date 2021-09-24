package com.example.imageclassifier;

import android.content.Context;

import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.model.Model;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;

// 이미지 분류 추론 모델
public class Classifier {
    // assets에 있는 자원을 사용하기 위한 변수
    Context context;

    // 모델 파일의 이름 설정
    private static final String MODEL_NAME = "mobilenet_imagenet_model.tflite";

    // 추론 모델 변수
    private Model model;

    // 입력 이미지를 위한 변수
    int modelInputWidth, modelInputHeight, modelInputChannel;
    TensorImage inputImage;

    // 출력을 위한 변수
    TensorBuffer outputBuffer;

    // 생성자
    public Classifier(Context context){
        this.context = context;
    }

    // 사용자 정의 초기화 메소드
    public void init() throws IOException{
        //  모델 생성
        model = Model.createModel(context, MODEL_NAME);
        
        // 입출력 구조를 만들어주는 사용자 정의 메소드 호출
        initModelShape();
    }
    
    // 입력 구조와 출력 구조를 만들어주는 사용자 정의 메소드
    private void initModelShape(){

        // 입력 데이터의 shape를 가져와서 변수들에 저장
        Tensor inputTensor = model.getInputTensor(0);
        int [] shape = inputTensor.shape();

        modelInputChannel = shape[0];
        modelInputWidth = shape[1];
        modelInputHeight = shape[2];

        // 입력 텐서
        inputImage = new TensorImage(inputTensor.dataType());

        // 출력 버퍼 생성
        Tensor outputTensor = model.getOutputTensor(0);
        outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(),
                outputTensor.dataType());
    }
}
