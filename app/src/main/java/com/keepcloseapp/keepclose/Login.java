package com.keepcloseapp.keepclose;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.dd.CircularProgressButton;


public class Login extends AppCompatActivity {
    CognitoCachingCredentialsProvider credentialsProvider;
    CircularProgressButton loginProgress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createLogin();

    }

    public void createLogin()
    {
        setContentView(R.layout.activity_login);
        initializeAmazonComponents();


        loginProgress = (CircularProgressButton) findViewById(R.id.btnLogin);

        loginProgress.setIndeterminateProgressMode(true);
        loginProgress.setText("Log In");
        loginProgress.setCompleteText("Logged in");
        loginProgress.setErrorText("Log in Failed");
        loginProgress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loginProgress.getProgress() == 0) {
                    loginProgress.setProgress(50);
                    findViewById(R.id.txtAuth).setVisibility(View.VISIBLE);
                } else if (loginProgress.getProgress() == 100) {
                    loginProgress.setProgress(0);
                } else {
                    loginProgress.setProgress(100);
                }
            }

        });


    }

    public void initializeAmazonComponents() {
        // Initialize the Amazon Cognito credentials provider
                credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:0afc869f-b0d3-4649-9841-0044fd0ade19", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );


    }
}
