package myapp.lenovo.httpclient.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import org.jsoup.Jsoup;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import myapp.lenovo.httpclient.utils.IOUtils;
import myapp.lenovo.httpclient.activity.MainActivity;
import myapp.lenovo.httpclient.utils.MyBase64;
import myapp.lenovo.httpclient.adapter.MyBaseExpandableListAdapter;
import myapp.lenovo.httpclient.R;
import myapp.lenovo.httpclient.entity.Score;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * A simple {@link Fragment} subclass.
 */
public class ScoreFragment extends Fragment {
    private CourseFragment.PassValue passValue;

    private List<String> groupName;
    private Map<String,List<Score>> childName;
    private String currentSemester;
    private ExpandableListView elv;

    private String linkURL;
    public static Handler handler;

    private static final String HOST_URL="http://222.24.62.120/";
    private static final String MAIN_URL="http://222.24.62.120/xs_main.aspx?xh=";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.fragment_score, container, false);
        elv=view.findViewById(R.id.score_elv);

        groupName=new ArrayList<>();
        childName=new HashMap<>();
        currentSemester="";

        Log.d("score","score");
        handler=new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.arg1==10){
                    String viewState=msg.obj.toString();
                    MyScoreThread myScoreThread=new MyScoreThread(linkURL,viewState);
                    myScoreThread.start();
                }
                else if(msg.arg1==12){
                    MyBaseExpandableListAdapter ela=new MyBaseExpandableListAdapter(groupName
                            ,childName,getContext());
                    elv.setAdapter(ela);
                    ela.notifyDataSetChanged();
                    passValue.passData(1);
                }
                else if(msg.arg1==22){
                    Toast.makeText(getActivity(),"请求失败", Toast.LENGTH_SHORT).show();
                    if (MainActivity.myProgressBarDialog!=null
                            &&MainActivity.myProgressBarDialog.isShowing()){
                        MainActivity.myProgressBarDialog.dismiss();
                        Toast.makeText(getActivity(),"请求失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };

        linkURL= IOUtils.analyzeURL(MainActivity.loginResult,"成绩查询");
        IOUtils.getViewState(HOST_URL+linkURL,MAIN_URL+MainActivity.accountStr, MainActivity.cookie);

        return view;
    }

    class MyScoreThread extends Thread{
        private String linkURL;
        private String viewState;

        MyScoreThread(String linkURL,String viewState){
            this.linkURL=linkURL;
            this.viewState=viewState;
        }

        @Override
        public void run() {
            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = new FormBody.Builder()
                    .add("__EVENTARGUMENT","")
                    .add("__EVENTTARGET","")
                    .add("__VIEWSTATE",viewState)
                    .add("btn_zcj","%C0%FA%C4%EA%B3%C9%BC%A8")
                    .add("ddlXN","")
                    .add("ddlXQ","")
                    .add("ddl_kcxz","")
                    .add("hidLanguage","").build();
            Request request = new Request.Builder()
                    .url(HOST_URL+linkURL)
                    .header("Cookie",MainActivity.cookie)
                    .header("Referer",MAIN_URL+MainActivity.accountStr)
                    .post(requestBody)
                    .build();
            try {
                Response response = client.newCall(request).execute();
                if (response.code() == 200){
                    String allScoreResult = response.body().string();
                    analyzeScore(allScoreResult);
                }
            } catch (IOException e) {
                Message msg=handler.obtainMessage(1,"");
                msg.arg1=22;
                handler.sendMessage(msg);
                e.printStackTrace();
            }
        }
    }

    private void analyzeScore(String allScoreResult){
        org.jsoup.nodes.Document doc=Jsoup.parse(allScoreResult);
        String viewState=doc.select("input[name=__VIEWSTATE]").val();
        analyzeDailyFinalScore(viewState);
    }

    private void analyzeDailyFinalScore(String viewState) {
        String decodedMiddleString;
        String decodedFinalString=null;
        try {
            decodedMiddleString = new String(MyBase64.decode(viewState));
            decodedFinalString=new String(MyBase64.decode(decodedMiddleString),"utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Log.d("decodedFinalString0",decodedFinalString);
        decodedFinalString=decodedFinalString.substring(decodedFinalString.indexOf("<?xml")
                ,decodedFinalString.indexOf("ram>"))+"ram>";
        decodedFinalString=decodedFinalString.replace
                (decodedFinalString.substring(decodedFinalString.indexOf("<xs:schema")
                        ,decodedFinalString.indexOf("<diffgr"))," ");
        decodedFinalString=decodedFinalString.replace("utf-16","utf-8");
        //Log.d("decodedFinalString1",decodedFinalString);
        InputStream is=new ByteArrayInputStream(decodedFinalString.getBytes());
        DocumentBuilderFactory dbf=DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        org.w3c.dom.Document doc=null;
        try {
            db=dbf.newDocumentBuilder();
            doc=db.parse(is);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }
        NodeList tables=doc.getElementsByTagName("Table");
        String[] nodeNames={"KCMC","KCXZ","PSCJ","QMCJ","CJ"};
        List<Score> scores=new ArrayList<>();
        for(int i=0;i<tables.getLength();i++){
            Score score=new Score();
            NodeList nodes=tables.item(i).getChildNodes();
            String year=null,semester=null;
            for(int j=0;j<nodes.getLength();j++){
                Node node=nodes.item(j);
                String nodeName=node.getNodeName();
                for(int k=0;k<5;k++){
                    if(nodeName.equals(nodeNames[k])){
                        addScore(score,k,node.getTextContent());
                    }
                }
                if(nodeName.equals("XN")){
                    year=node.getTextContent();
                }
                else if(nodeName.equals("XQ")){
                    semester=node.getTextContent();
                }
            }
            if(!currentSemester.equals(year+semester)){
                if(scores.size()>0){
                    childName.put(currentSemester,scores);
                }
                currentSemester=year+semester;
                groupName.add(currentSemester);
                scores=new ArrayList<>();
                scores.add(score);
            }
            else{
                scores.add(score);
            }
        }
        childName.put(currentSemester,scores);
//        for(int i=0;i<groupName.size();i++){
//            Log.d("groupName",i+"---"+groupName.get(i));
//        }
//        List<Score> list0=childName.get("2015-20161");
//        for(int i=0;i<list0.size();i++){
//            Log.d("list0",i+"---"+list0.get(i));
//        }
//        List<Score> list1=childName.get("2015-20162");
//        for(int i=0;i<list1.size();i++){
//            Log.d("list1",i+"---"+list1.get(i));
//        }
//        List<Score> list2=childName.get("2016-20171");
//        for(int i=0;i<list2.size();i++){
//            Log.d("list2",i+"---"+list2.get(i));
//        }
//        List<Score> list3=childName.get("2016-20172");
//        for(int i=0;i<list3.size();i++){
//            Log.d("list3",i+"---"+list3.get(i));
//        }
        Message msg=handler.obtainMessage(1,"");
        msg.arg1=12;
        handler.sendMessage(msg);
    }

    private void addScore(Score score,int index,String content){
        switch (index){
            case 0:score.setCourseName(content);break;
            case 1:score.setCourseType(content);break;
            case 2:score.setDailyScore(content);break;
            case 3:score.setFinalScore(content);break;
            case 4:score.setScore(content);break;
            default:break;
        }
    }

    @Override
    public void onAttach(Context context) {
        if(getActivity()instanceof CourseFragment.PassValue){
            passValue=(CourseFragment.PassValue) getActivity();
        }
        super.onAttach(context);
    }
}
