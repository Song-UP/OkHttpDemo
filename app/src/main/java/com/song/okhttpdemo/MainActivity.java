package com.song.okhttpdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.content_body);
    }


    OkHttpClient okHttpClient = new OkHttpClient();

    /**
     * 同步Get
     * html小文件载入内存树解析，大文件必须使用流式解析
     *
     * execute是运行在当前线程中的，因此如果想请求，需要开启子线程
     * @throws IOException
     */
    Response response = null;
    public void okHttpSync() throws IOException {

        new Thread(new Runnable() {
            @Override
            public void run() {
                Request request = new Request.Builder()
                        .url("http://publicobject.com/helloworld.txt")
                        .build();

                try {
                    response = okHttpClient.newCall(request).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Headers resposeHeader = null;
                if (!response.isSuccessful()) {
                    resposeHeader = response.headers();
                }
                Log.d("ResponseHead--","--------------------");
                Log.d("ResponseHead--","Code:"+response.code());
                for (int i = 0; resposeHeader != null && i <resposeHeader.size(); i++){
                    Log.d("ResponseHead--",resposeHeader.name(i)+":"+resposeHeader.value(i));
                }
                Log.d("ResponseHead--","--------------------");

                try {
                    contentText = response.body().string();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(contentText);
                    }
                });

//
//                ResponseBody responseBody =  response.body();

            }
        }).start();



    }


    /**
     * 异步Get
     *
     * enqueue() 会在Okttp内开启线程，此时提交数据和响应都是在子线程中的
     */
    String contentText = "";

    public void okHttpAsync(){
        final Request request = new Request.Builder()
                .url("http://publicobject.com/helloworld.txt")
                .build();

        Log.d("async","------------------");
        okHttpClient.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//
//            }

            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("async","response fail -- Thread:"+Thread.currentThread() + "--");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("async","response"+response.code()+" -- Thread:"+Thread.currentThread());

                contentText = response.body().string();
                Log.d("async",contentText);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(contentText);
                        contentText = "";
                    }
                });


//                Headers headers = response.headers();
//                for (int i = 0; i<headers.size(); i++){
//                }
            }
        });
        Log.d("async","------------------");

    }


    public void clickBtn(View view) throws IOException {
        textView.setText("");
        int id = view.getId();
        switch (id){
            case R.id.sycnc_request:
                okHttpSync();
                break;
            case R.id.asycnc_request:
                okHttpAsync();
                break;

            case R.id.post_request:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            MainActivity.this.okHttpPost();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                break;
            case R.id.post_file_request:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            MainActivity.this.postFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                break;



        }
    }

    //Post---------------------
    //Post提交String：此时数据体全部存放在内存中，因此避免提交大文档（>1MB）
    public static final MediaType MEDIA_TYPE = MediaType.parse("text/x-markdown; charset=utf-8");
    public void okHttpPost() throws IOException {
        String postBody = ""
                + "Releases\n"
                + "--------\n"
                +"\n"
                +"Song\n";
        Request request = new Request.Builder()
                .url("https://api.github.com/markdown/raw")
                .post(RequestBody.create(MEDIA_TYPE,postBody))
                .build();
        final Response response = okHttpClient.newCall(request).execute();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    textView.setText("提交成功："+response.isSuccessful()+"  body:" + response.body().string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //post提交文件
    public static final MediaType MEDIA_TYPE_FILE = MediaType.parse("text/x-markdown; charset=utf-8");
    public void postFile() throws IOException {
//        MainActivity.this.getAssets().
        File file = new File("/storage/emulated/0/README.md");
        if (!file.exists() || !file.isFile()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        writeBytesToFile("READER.md",file);
        final Request request = new Request.Builder()
                .url("https://api.github.com/markdown/raw")
                .post(RequestBody.create(MEDIA_TYPE_FILE, file))
                .build();
        final Response response = okHttpClient.newCall(request).execute();

        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    textView.setText("提交文件成功："+response.isSuccessful() +"\n"
                                +  response.toString()
                                +"\n"+ response.body().string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }



    public void writeBytesToFile(String fileName, File file) throws IOException{
        InputStream is =  getAssets().open("READER.md");
        FileOutputStream fos = null;
        try {
            byte[] data = new byte[2048];
            int nbread = 0;
            fos = new FileOutputStream(file);
            while((nbread=is.read(data))>-1){
                fos.write(data,0,nbread);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally{
            if (fos!=null){
                fos.close();
            }
        }
    }


    //Post 表单提交
    public void postForm() throws IOException {
        RequestBody formBody = new FormBody.Builder()
                .add("search", "Jurassic Park")
                .build();
        Request request = new Request.Builder()
                .url("https://en.wikipedia.org/w/index.php")
                .post(formBody)
                .build();
        Response response = okHttpClient.newCall(request).execute();





    }



}
