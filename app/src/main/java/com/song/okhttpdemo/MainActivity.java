package com.song.okhttpdemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;
import okio.BufferedSink;

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

            case R.id.post_span_request:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            MainActivity.this.postSpan();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                break;

            case R.id.post_cache_request:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            File file = new File("/storage/emulated/0/cache.txt");
                            if (!file.exists() || !file.isFile())
                                file.createNewFile();
                            MainActivity.this.cacheRespon(file);
                            okhttpCache();
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


    //
    // Post方式提交分块请求

    private static final String IMUR_CLIENT_ID = "";
    private static final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");
    public void postSpan() throws IOException {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("title", "Square Logo")
                .addFormDataPart("image", "logo-sqare.png",
                        RequestBody.create(MEDIA_TYPE_PNG, new File("")))
                        .build();

        final Request request = new Request.Builder()
                .header("Authorization", "Client-ID" + IMUR_CLIENT_ID) //任何包含这个头文件的文件，都会被替换
                .url("https://api.imgur.com/3/image")
                .post(requestBody)
                .build();
        final Response response = okHttpClient.newCall(request).execute();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    textView.setText("是否提交成功："+response.isSuccessful()+"\n"
                                    + response.body().string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });



    }


    //header 设置的内容通常是惟一的
    //addHeader 添加内容，添加的内容是多个的
    public void requestHeader() throws IOException {
        final Request request = new Request.Builder()
                .url("https://api.github.com/repos/square/okhttp/issues")
                .header("User-Agent", "OKHttp Headers.java")
                .addHeader("Accept", "application/json; q = .5")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .build();
        final Response response = okHttpClient.newCall(request).execute();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                    textView.setText("是否请求成功："+response.isSuccessful()+"\n"
                                        +response.toString());
            }
        });
    }

    //响应缓存
    public void cacheRespon(File cacheDirectory){
        int cacheSize = 10 * 1024 * 1024; //10Mib
        Cache cache = new Cache(cacheDirectory, cacheSize);

        okHttpClient = new OkHttpClient.Builder()
                .cache(cache)
                .build();
    }


    /**
     * 缓存到文件中
     * 强制使用本地缓存 FORCE_CACHE， 网络缓存 CacheControl.FORCE_NETWORK
     *
     * @throws IOException
     */
    public void okhttpCache() throws IOException {
        Request request = new Request.Builder()
                .url("http://publicobject.com/helloworld.txt")
//                .header("Cache-Control","no-cache")
                .build();

        CacheControl cacheControl = request.cacheControl();
//        没有网络时使用缓存
        cacheControl.onlyIfCached();
        cacheControl.noCache();

        final Response response1 = okHttpClient.newCall(request).execute();

//        Call call = okHttpClient.newCall(request);
//        final Response response1 = call.execute();
//        call.cancel();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText("response1------------");
                textView.append("Response 1:"+response1+"\n");
                textView.append("Response 1:"+response1.cacheResponse()+"\n");
                textView.append("Response 1:"+response1.networkResponse()+"\n");

            }
        });

        final Response response2 = okHttpClient.newCall(request).execute();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.append("response2------------");
                textView.append("Response 2:"+response2+"\n");
                textView.append("Response 2:"+response2.cacheResponse()+"\n");
                textView.append("Response 2:"+response2.networkResponse()+"\n");

                textView.append("response2------------"+"\n");

                textView.append("Response 2 equeals Response2 :"+ response1.equals(response2));

            }
        });

    }

    /**
     * 设置超时（连接，读写超时）
     */
    public void okHttpOutTime() throws IOException {
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .writeTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3,TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url("http://httpbin.org/delay/2")
                .build();
        Call call = okHttpClient.newCall(request);
        Response response = call.execute();

    }

    //为每个call设置配置,通过Builder
    public void setMoreCall() throws IOException {
        okHttpOutTime();
        OkHttpClient.Builder builder = okHttpClient.newBuilder();
        //设置连接超时
        builder.connectTimeout(2, TimeUnit.SECONDS);
        builder.writeTimeout(2, TimeUnit.SECONDS);
        Cache cache = new Cache(new File("cacheFile"), 10*1024);
        //设置Cache
        builder.cache(cache);
    }


    //Http认证（如果认证失败，那么就进入用户登录界面）
    public void Authenticate() throws IOException {
        okHttpClient = new OkHttpClient.Builder()
                .authenticator(new Authenticator() {
                    @Nullable
                    @Override
                    public Request authenticate(Route route, Response response) throws IOException {
                        String credential = Credentials.basic("jesse", "password1");
                        //当认证无法工作时，跳过认证
                        if (credential.equals(response.request().header("Authorization")))
                            return null; //如果已经认证失败，不在尝试

                        if (responseCount(response) >5)
                            return null; //多次认证无法通过，避免重复尝试

                        return response.request().newBuilder()
                                .header("Authorization", credential)
                                .build();
                    }
                })
                .build();

        Request request = new Request.Builder()
                .url("http://publicobject.com/secrets/hellosecret.txt")
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


    protected int responseCount(Response response){
        int result = 1;
        while ((response = response.priorResponse()) != null){
            result ++;
        }
        return result;

    }


    //Post方式提交流
    public void postStream(){
        RequestBody requestBody = new RequestBody() {
            @Nullable
            @Override
            public MediaType contentType() {
                return MediaType.parse("text/html");
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                sink.writeUtf8("");


            }
        };


    }





}
