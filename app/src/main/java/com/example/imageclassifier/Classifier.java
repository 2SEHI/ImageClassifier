package com.example.imageclassifier;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Pair;

import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.model.Model;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod.NEAREST_NEIGHBOR;

//이미지 분류 추론 모델
public class Classifier {
    //assets 에 있는 자원을 사용하기 위한 변수
    Context context;
    //생성자
    public Classifier(Context context){
        this.context = context;
    }

    //모델 파일의 이름 설정
    private static final String MODEL_NAME = "mobilenet_imagenet_model.tflite";

    //추론 모델 변수
    private Model model;

    //입력 이미지를 위한 변수
    int modelInputWidth, modelInputHeight, modelInputChannel;
    TensorImage inputImage;

    //출력을 위한 변수
    TensorBuffer outputBuffer;

    //레이블 파일 이름 과 목록을 저장할 변수
    private static final String LABEL_FILE = "labels.txt";
    private List<String> labels;


    //사용자 정의 초기화 메소드
    public void init() throws IOException {
        //모델을 생성
        model = Model.createModel(context, MODEL_NAME);

        //입력 구조 와 출력 구조를 만들어주는 사용자 정의 메소드를 호출
        initModelShape();

        //레이블 파일을 읽어서 labels에 저장
        labels = FileUtil.loadLabels(context, LABEL_FILE);
        //파일을 만들 때 첫번째 줄을 삭제하지 않은 경우 수행
        //labels.remove(0);
    }

    //입력 구조 와 출력 구조를 만들어주는 사용자 정의 메소드
    private void initModelShape(){
        //입력 데이터의 shape를 가져와서 변수들에 저장
        Tensor inputTensor = model.getInputTensor(0);
        int [] shape = inputTensor.shape();
        modelInputChannel = shape[0];
        modelInputWidth = shape[1];
        modelInputHeight = shape[2];

        //입력 텐서 생성
        inputImage = new TensorImage(inputTensor.dataType());

        //출력 버퍼 생성
        Tensor outputTensor = model.getOutputTensor(0);
        outputBuffer = TensorBuffer.createFixedSize(
                outputTensor.shape(),
                outputTensor.dataType());
    }

    //안드로이드의 이미지를 분류 모델에서 사용할 수 있도록 변환해주는 메소드
    private Bitmap convertBitmapToARGB8888(Bitmap bitmap){
        return bitmap.copy(Bitmap.Config.ARGB_8888, true);
    }


    //이미지를 읽어서 전처리 한 후 딥러닝에 사용할 이미지로 리턴해주는 메소드
    private TensorImage loadImage(final Bitmap bitmap){
        //이미지를 읽어옵니다.
        //inputImage.load(bitmap);
        if(bitmap.getConfig() != Bitmap.Config.ARGB_8888){
            inputImage.load(convertBitmapToARGB8888(bitmap));
        }else{
            inputImage.load(bitmap);
        }

        //전처리 수행
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeOp(
                                modelInputWidth,
                                modelInputHeight,
                                NEAREST_NEIGHBOR))
                        .add(new NormalizeOp(0.0f, 255.0f))
                        .build();
        //전처리를 수행한 후 리턴
        return imageProcessor.process(inputImage);
    }

    //추론 결과 해석을 위한 메소드
    //확률이 가장 높은 레이블 이름 과 확률을 Pair로 리턴하는 메소드
    private Pair<String, Float> argMax(Map<String, Float> map){
        String maxKey = "";
        //확률이 0 ~ 1 사이이므로 최대값을 구하기 위한 임시변수는
        //0보다 작은 값에서 출발하면 됩니다.
        //최소값을 구하는 문제이면 1보다 큰 값 아무거나 가능
        //배열의 경우는 첫번째 데이터를 삽입하는 것이 효율적
        float maxVal = -1;
        //Map을 하나씩 순회
        for(Map.Entry<String, Float> entry: map.entrySet()){
            //순회할 때 마다 값을 가져와서 maxVal 과 비교해서
            //maxVal 보다 크면 그 때의 key 와 value를 저장
            float f = entry.getValue();
            if(f > maxVal){
                maxKey = entry.getKey();
                maxVal = f;
            }
        }
        //key 와 value를 하나로 묶어서 리턴
        return new Pair<>(maxKey, maxVal);
    }

    //추론을 위한 메소드
    //스마트 폰에서 이미지를 사용할 때 기억해야 할 것
    //기기의 방향 문제 입니다.
    public Pair<String, Float> classify(
            Bitmap image, int sensorOrientation){
        //전처리된 이미지 가져오기
        inputImage = loadImage(image);

        //model 에 입력 가능한 형태로 변환
        Object [] inputs = new Object[]{inputImage.getBuffer()};

        Map<Integer, Object> outputs = new HashMap<>();
        outputs.put(0, outputBuffer.getBuffer().rewind());
        //추론
        model.run(inputs, outputs);
        //결과를 해석
        Map<String, Float> output =
                new TensorLabel(labels, outputBuffer)
                        .getMapWithFloatValue();

        return argMax(output);

    }

    //메모리 정리 메소드
    public void finish(){
        if(model != null){
            model.close();
        }
    }
}