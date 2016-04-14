package com.ztiany2011.simplezxing;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;


public class ShowResultActivity extends Activity {
	private TextView show_result;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_result_activity);
		show_result = (TextView) findViewById(R.id.show_result);
		initView();
	}
	
	private void initView(){
		if(getIntent().hasExtra("result") && null != getIntent().getStringExtra("result")){
			String result = getIntent().getStringExtra("result");
			show_result.setText(result);
		}else{
			show_result.setText(getString(R.string.result_fail));
		}
	}

}
