package myapp.lenovo.httpclient.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;

import myapp.lenovo.httpclient.dialog.MyProgressBarDialog;
import myapp.lenovo.httpclient.R;
import myapp.lenovo.httpclient.utils.DensityUtils;
import myapp.lenovo.httpclient.utils.ImageLoader;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends Activity {
    private EditText account;
    private EditText password;
    private EditText confirm;
    private ImageView verification;
    private Bitmap verifyBitmap;

    private String cookie;
    private String loginResult;
    private String accountStr;

    private MyProgressBarDialog myProgressBarDialog;

    private static final String SCHOOL_URL = "https://i.loli.net/2020/03/12/EqR4BwkTzjfFoP7.png";
    private static final String LOGIN_URL="http://222.24.62.120/default2.aspx";
    private static final String VERIFICATION_URL="http://222.24.62.120/CheckCode.aspx";
    private static final String VIEW_STATE="dDwxNTMxMDk5Mzc0Ozs+lYSKnsl/mKGQ7CKkWFJpv0btUa8=";

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.arg1)
            {
                case 0:verification.setImageBitmap(verifyBitmap);break;
                case 1:
                    Toast.makeText(LoginActivity.this, "登录成功",
                            Toast.LENGTH_SHORT).show();
                    Intent intent=new Intent(LoginActivity.this,MainActivity.class);
                    intent.putExtra("loginResult",loginResult);
                    intent.putExtra("cookie",cookie);
                    intent.putExtra("accountStr",accountStr);
                    startActivity(intent);
                    finish();break;
                case 2:Toast.makeText(LoginActivity.this, "用户名不存在或未按照要求参加教学活动",
                        Toast.LENGTH_SHORT).show();
                    loadVerification();break;
                case 3:Toast.makeText(LoginActivity.this, "密码错误",
                        Toast.LENGTH_SHORT).show();
                    loadVerification();break;
                case 4:Toast.makeText(LoginActivity.this, "验证码不正确",
                        Toast.LENGTH_SHORT).show();
                    loadVerification();break;
                case 5:Toast.makeText(LoginActivity.this, "用户名不能为空",
                        Toast.LENGTH_SHORT).show();
                    loadVerification();break;
                case 6:Toast.makeText(LoginActivity.this, "密码不能为空",
                        Toast.LENGTH_SHORT).show();
                    loadVerification();break;
                case 7:Toast.makeText(LoginActivity.this, "验证码不能为空，如看不清请刷新",
                        Toast.LENGTH_SHORT).show();
                    loadVerification();break;
                case 20:Toast.makeText(LoginActivity.this,"验证码请求失败,请检查网络设置",
                        Toast.LENGTH_SHORT).show();break;
                case 21:
                    Toast.makeText(LoginActivity.this,"登录请求失败,请检查网络设置",
                            Toast.LENGTH_SHORT).show();
                    if (myProgressBarDialog!=null&&myProgressBarDialog.isShowing())
                        myProgressBarDialog.dismiss();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Intent intent=LoginActivity.this.getIntent();
        boolean isLogout=intent.getBooleanExtra("isLogout",true);
        if(!isLogout){
            Toast.makeText(LoginActivity.this
                    ,"你还没有进行本学期的课堂教学质量评价,请先登录西邮教务处官网进行评价"
                    ,Toast.LENGTH_LONG).show();
        }

        account = findViewById(R.id.account_et);
        password = findViewById(R.id.password_et);
        confirm= findViewById(R.id.verification_et);
        Button login = findViewById(R.id.login_btn);
        verification= findViewById(R.id.identifying_code_iv);
        ImageView school= findViewById(R.id.login_iv);
        ImageLoader imageLoader = ImageLoader.build(LoginActivity.this);
        imageLoader.bindBitmap(SCHOOL_URL, school,
                DensityUtils.dipToPx(  LoginActivity.this , 220),
                DensityUtils.dipToPx(  LoginActivity.this , 220));


        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myProgressBarDialog=new MyProgressBarDialog(LoginActivity.this,"登录中…");
                myProgressBarDialog.setCancelable(false);
                myProgressBarDialog.show();

                accountStr=account.getText().toString().trim();
                String passwordStr=password.getText().toString().trim();
                String confirmStr=confirm.getText().toString().trim();

                MyLoginThread myloginThread=new MyLoginThread(accountStr,passwordStr,confirmStr);
                myloginThread.start();
            }
        });

        verification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadVerification();
            }
        });
    }

    @Override
    protected void onResume() {
        loadVerification();
        super.onResume();
    }

    class MyVerifyThread extends Thread{
        @Override
        public void run() {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(VERIFICATION_URL)
                    .build();
            try {
                Response response = client.newCall(request).execute();
                cookie = response.header("Set-Cookie");
                if (response.code() == 200){
                    InputStream in = response.body().byteStream();
                    verifyBitmap= BitmapFactory.decodeStream(in);
                    Message msg=handler.obtainMessage(1,"");
                    msg.arg1=0;
                    handler.sendMessage(msg);
                }
            } catch (IOException e) {
                Message msg=handler.obtainMessage(1,"");
                msg.arg1=20;
                handler.sendMessage(msg);
                e.printStackTrace();
            }
        }
    }

    class MyLoginThread extends Thread{
        private String accountStr;
        private String passwordStr;
        private String verificationStr;

        MyLoginThread(String accountStr, String passwordStr ,String verificationStr){
            this.accountStr=accountStr;
            this.passwordStr=passwordStr;
            this.verificationStr=verificationStr;
        }

        @Override
        public void run() {
            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = new FormBody.Builder()
                    .add("Button1","")
                    .add("hidPdrs","")
                    .add("hidsc","")
                    .add("lbLanguage","")
                    .addEncoded("RadioButtonList1","%D1%A7%C9%FA")
                    .add("Textbox1","")
                    .add("TextBox2",passwordStr)
                    .add("txtSecretCode",verificationStr)
                    .add("txtUserName",accountStr)
                    .add("__VIEWSTATE",VIEW_STATE).build();
            Request request = new Request.Builder()
                    .url(LOGIN_URL)
                    .header("Cookie",cookie)
                    .post(requestBody)
                    .build();
            try {
                Response response = client.newCall(request).execute();
                if (response.code() == 200){
                    loginResult = response.body().string();
                    myProgressBarDialog.dismiss();
                    analyzeLogin();
                }
            } catch (IOException e) {
                Log.d("IOException","IOException");
                Message msg=handler.obtainMessage(1,"");
                msg.arg1=21;
                handler.sendMessage(msg);
                e.printStackTrace();
            }
        }
    }

    public void loadVerification(){
        MyVerifyThread myVerifyThread=new MyVerifyThread();
        myVerifyThread.start();
    }

    public void analyzeLogin(){
        Document doc= Jsoup.parse(loginResult);
        Elements links=doc.select("a[href]");
        Elements alerts=doc.select("script[language]");

        for(Element link:links){
            if(link.text().equals("个人信息")){
                Message msg=handler.obtainMessage(1,"");
                msg.arg1=1;
                handler.sendMessage(msg);
                return;
            }
        }

        for(Element alert:alerts){
            Message msg=handler.obtainMessage(1,"");
            if(alert.data().contains("用户名不存在或未按照要求参加教学活动")){
                msg.arg1=2;
                handler.sendMessage(msg);
            }
            else if(alert.data().contains("密码错误")){
                msg.arg1=3;
                handler.sendMessage(msg);
            }
            else if(alert.data().contains("验证码不正确")){
                msg.arg1=4;
                handler.sendMessage(msg);
            }
            else if(alert.data().contains("用户名不能为空")){
                msg.arg1=5;
                handler.sendMessage(msg);
            }
            else if(alert.data().contains("密码不能为空")){
                msg.arg1=6;
                handler.sendMessage(msg);
            }
            else if(alert.data().contains("验证码不能为空，如看不清请刷新")){
                msg.arg1=7;
                handler.sendMessage(msg);
            }
        }
    }
}
