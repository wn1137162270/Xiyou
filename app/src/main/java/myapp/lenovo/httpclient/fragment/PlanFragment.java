package myapp.lenovo.httpclient.fragment;


import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import myapp.lenovo.httpclient.utils.IOUtils;
import myapp.lenovo.httpclient.activity.MainActivity;
import myapp.lenovo.httpclient.adapter.MyListViewAdapter;
import myapp.lenovo.httpclient.dialog.MyProgressBarDialog;
import myapp.lenovo.httpclient.R;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * A simple {@link Fragment} subclass.
 */
public class PlanFragment extends Fragment {
    private CourseFragment.PassValue passValue;
    private boolean isFirst;

    private MyProgressBarDialog myProgressBarDialog;

    private ListView listView;

    private String linkURL;
    private String planViewStateStr;
    private String semesterStr;
    private String currentPageStr;
    private String pageSizeStr;
    private List<String[]> planList;

    private static final String HOST_URL="http://222.24.62.120/";
    private static final String MAIN_URL="http://222.24.62.120/xs_main.aspx?xh=";

    private Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.arg1==9){
                int cps=Integer.parseInt(currentPageStr);
                if(cps<Integer.parseInt(pageSizeStr)){
                    cps++;
                    currentPageStr=String.valueOf(cps);
                    MyPlanThread myPlanThread=new MyPlanThread(linkURL);
                    myPlanThread.start();
                }
                else{
                    if(isFirst){
                        isFirst=false;
                        passValue.passData(1);
                    }
                    else {
                        myProgressBarDialog.dismiss();
                    }
                    MyListViewAdapter myListViewAdapter=new MyListViewAdapter(planList,getContext());
                    listView.setAdapter(myListViewAdapter);
                }
            }
            else if(msg.arg1==22){
                if(isFirst){
                    if (MainActivity.myProgressBarDialog!=null
                            &&MainActivity.myProgressBarDialog.isShowing()){
                        MainActivity.myProgressBarDialog.dismiss();
                        Toast.makeText(getActivity(),"请求失败", Toast.LENGTH_SHORT).show();
                    }
                    isFirst=false;
                }
                else if (myProgressBarDialog!=null&&myProgressBarDialog.isShowing()){
                    Toast.makeText(getActivity(),"请求失败", Toast.LENGTH_SHORT).show();
                    myProgressBarDialog.dismiss();
                }
                else {
                    Toast.makeText(getActivity(),"请求失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.fragment_plan, container, false);
        listView= view.findViewById(R.id.plan_lv);

        isFirst=true;
        planList=new ArrayList<>();
        Log.d("plan","plan");

        planList.clear();
        linkURL= IOUtils.analyzeURL(MainActivity.loginResult,"培养计划");
        MyPlanFirstThread myPlanFirstThread=new MyPlanFirstThread(linkURL);
        myPlanFirstThread.start();

        return view;
    }

    class MyPlanFirstThread extends Thread{
        private String linkURL;

        MyPlanFirstThread(String linkURL){
            this.linkURL=linkURL;
        }

        @Override
        public void run() {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(HOST_URL+linkURL)
                    .header("Cookie",MainActivity.cookie)
                    .header("Referer",MAIN_URL+MainActivity.accountStr)
                    .build();
            try {
                Response response = client.newCall(request).execute();
                if (response.code() == 200){
                    String planFirstResult = response.body().string();
                    analyzePlan(planFirstResult);
                }
            } catch (IOException e) {
                Message msg=handler.obtainMessage(1,"");
                msg.arg1=22;
                handler.sendMessage(msg);
                e.printStackTrace();
            }
        }
    }

    class MyPlanThread extends Thread{
        private String linkURL;

        MyPlanThread(String linkURL){
            this.linkURL=linkURL;
        }

        @Override
        public void run() {
            String target;
            if(Integer.parseInt(currentPageStr)>1)
                target="dpDBGrid:txtChoosePage";
            else
                target="xq";
            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = new FormBody.Builder()
                    .add("__EVENTARGUMENT","")
                    .add("__EVENTTARGET",target)
                    .add("__VIEWSTATE",planViewStateStr)
                    .add("dpDBGrid:txtChoosePage",currentPageStr)
                    .add("dpDBGrid:txtPageSize","10")
                    .add("kcxz","全部")
                    .add("xq",semesterStr).build();
            Request request = new Request.Builder()
                    .url(HOST_URL+linkURL)
                    .header("Cookie",MainActivity.cookie)
                    .header("Referer",MAIN_URL+MainActivity.accountStr)
                    .post(requestBody)
                    .build();
            try {
                Response response = client.newCall(request).execute();
                if (response.code() == 200){
                    String planResult = response.body().string();
                    analyzePlan(planResult);
                }
            } catch (IOException e) {
                Message msg=handler.obtainMessage(1,"");
                msg.arg1=22;
                handler.sendMessage(msg);
                e.printStackTrace();
            }
        }
    }

    public void analyzePlan(String planResult){
        Document doc=Jsoup.parse(planResult);
        planViewStateStr =doc.select("input[name=__VIEWSTATE]").val();
        Elements td=doc.select("td");
        Element pageSize=doc.getElementById("dpDBGrid_lblTotalPages");
        Element currentPage=doc.getElementById("dpDBGrid_lblCurrentPage");
        Elements selected=doc.select("option[selected]");
        int i=22;
        while (!td.get(i).text().equals("课程代码")) {
            String[] plans = new String[15];
            for (int j = 0; j < 15; j++) {
                plans[j] = td.get(i + j).text();
            }
            i=i+16;
            planList.add(plans);
        }
        //for(int j=0;j<planList.size();j++){
        //    for(String aplan:planList.get(j))
        //        Log.d("plan",aplan);
        //}
        pageSizeStr=pageSize.text();
        Log.d("pageSize",pageSizeStr);
        currentPageStr=currentPage.text();
        Log.d("currentPageStr",currentPageStr);
        semesterStr=selected.get(3).text();
        Log.d("semester",semesterStr);
        Message msg=handler.obtainMessage(1,"");
        msg.arg1=9;
        handler.sendMessage(msg);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        int base=Menu.FIRST;
        for(int i=0;i<8;i++) {
            String ci="第"+(i+1)+"学期";
            MenuItem mi=menu.add(base, base + i + 1, base + i + 1, ci);
            mi.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            if(ci.substring(1,2).equals(semesterStr))
                mi.setVisible(false);
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()>Menu.FIRST){
            myProgressBarDialog =new MyProgressBarDialog(getContext(),"数据加载中…");
            myProgressBarDialog.show();
            myProgressBarDialog.setCancelable(false);
            String ci=item.getTitle().toString();
            semesterStr=ci.substring(1,2);
            currentPageStr="1";
            planList.clear();
            MyPlanThread myPlanThread=new MyPlanThread(linkURL);
            myPlanThread.start();
        }
        return true;
    }

    @Override
    public void onAttach(Context context) {
        if(getActivity()instanceof CourseFragment.PassValue){
            passValue=(CourseFragment.PassValue) getActivity();
        }
        super.onAttach(context);
    }
}
