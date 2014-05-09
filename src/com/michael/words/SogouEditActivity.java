package com.michael.words;

import android.os.Bundle;

public class SogouEditActivity extends BaseActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SetFilter(", type=buf");
	}
}
