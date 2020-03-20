package myapp.lenovo.httpclient.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import myapp.lenovo.httpclient.R;

/**
 * Created by Lenovo on 2017/2/2.
 */

public class MyListViewAdapter extends BaseAdapter {
    private List<String[]> planList;
    private Context context;
    private LayoutInflater inflater;

    public MyListViewAdapter(List<String[]> planList, Context context){
        this.planList=planList;
        this.context=context;
        inflater=LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return planList.size();
    }

    @Override
    public Object getItem(int i) {
        return planList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if(view==null)
            view=inflater.inflate(R.layout.list_item,null);
        TextView order= view.findViewById(R.id.order_tv);
        TextView name= view.findViewById(R.id.name_tv);
        TextView necessity= view.findViewById(R.id.necessity_tv);
        TextView credit= view.findViewById(R.id.credit_content_tv);
        TextView period= view.findViewById(R.id.period_content_tv);
        TextView check= view.findViewById(R.id.check_content_tv);
        TextView category= view.findViewById(R.id.category_content_tv);
        TextView code= view.findViewById(R.id.code_content_tv);
        TextView week= view.findViewById(R.id.week_content_tv);

        order.setText(i+1+".");
        for(int j=0;j<=6;j++)
            if(planList.get(i)[j].length()<=1)
                planList.get(i)[j]="无";
        if(planList.get(i)[14].length()<=1)
            planList.get(i)[14]="无";
        name.setText(planList.get(i)[1]);
        necessity.setText(planList.get(i)[5]);
        credit.setText(planList.get(i)[2]);
        period.setText(planList.get(i)[3]);
        check.setText(planList.get(i)[4]);
        category.setText(planList.get(i)[6]);
        code.setText(planList.get(i)[0]);
        week.setText(planList.get(i)[14]);

        String cn=planList.get(i)[5];
        if(cn.equals("必修课"))
            necessity.setTextColor(context.getResources().getColor(R.color.colorBlack));
        else
            necessity.setTextColor(context.getResources().getColor(R.color.colorDarkGray));

        return view;
    }
}
